TeaVM Metaprogramming is a compile-time code generation API that lets you write Java code that runs *during* TeaVM compilation to generate the actual runtime code. Think of it as a type-safe, Java-based alternative to annotation processors or bytecode instrumentation — but tightly integrated into the TeaVM compilation pipeline.


## Core concepts

The metaprogramming API lets a *meta-method* (compile-time Java code) generate the body of a *target method* (runtime Java code). The meta-method runs at TeaVM compilation time, inspects its arguments as compile-time descriptors, and uses the `Metaprogramming` API to emit the code that will actually execute at runtime.

All methods on `Metaprogramming` throw `UnsupportedOperationException` when called outside a TeaVM compile-time environment; the class exists only to give you a typed API.

## The two worlds

There are two separate execution contexts, and it is essential to keep them apart:

| | Compile-time world | Runtime world |
|---|---|---|
| When it runs | During TeaVM compilation | In the browser / on the JVM after transpilation |
| What you can do | Inspect types, iterate fields/methods, call `emit`/`lazy`/`exit` | Run the generated code |
| Java values | Ordinary Java objects, `ReflectClass`, `ReflectMethod`, … | Ordinary Java objects |
| Represented as | Regular variables in the meta-method | `Value<T>` handles in the meta-method |

The meta-method lives entirely in the compile-time world. Lambdas you pass to `emit()`, `lazy()`, and `exit()` are *snippets of runtime code*, not closures executed at compile time.


## @Meta — declaring a meta-method

Annotate a `native` method with `@Meta` to tell the compiler that a meta-method exists alongside it in the same class. The meta-method has **the same name**, returns `void`, and is `static`. Its parameters are a remapped version of the target method's parameters:

- `Class<X>` parameters become `ReflectClass<X>` — the class argument that was passed at the call-site is resolved at compile time.
- All other parameters become `Value<T>` — a handle to the runtime value that the caller will pass.
- For instance methods, an implicit first parameter `Value<TheClass>` representing the receiver is prepended.

```java
// Target method — called from normal code
@Meta
static native int classNameLength(Class<?> cls, int add);

// Meta-method — runs at compile time to generate the body of the above
static void classNameLength(ReflectClass<?> cls, Value<Integer> add) {
    int length = cls.getName().length(); // compile-time computation
    exit(() -> length + add.get());      // emit the return expression
}
```

When TeaVM encounters a call to `classNameLength(String.class, 3)`, it runs the meta-method with `cls = ReflectClass<String>` and `add = Value<Integer>` (bound to the runtime argument `3`). The meta-method computes `length` at compile time and emits code equivalent to `return 14 + add;`.

### Overloading

Multiple `@Meta`-annotated methods can share the same name as long as they have different signatures. Each gets its own meta-method:

```java
@Meta private static native String callDebug(Class<?> cls, Object obj);
static void callDebug(ReflectClass<?> cls, Value<Object> obj) { … }

@Meta private static native String callDebug(Class<?> cls, Object obj, String a, int b);
static void callDebug(ReflectClass<?> cls, Value<Object> obj, Value<String> a, Value<Integer> b) { … }
```


## Value&lt;T&gt; — the bridge between worlds

`Value<T>` is an opaque handle to a runtime value. You obtain `Value<T>` instances in two ways:

1. **As parameters** to a meta-method — the framework wraps the callee's arguments.
2. **From `emit()`** — calling `emit(() -> expr)` executes `expr` at runtime and returns a `Value<T>` that represents the result.

The only method on `Value<T>` is `T get()`. Its semantics depend on where you call it:

- **Inside a lambda passed to `emit()` or `lazy()`** — this is the *emitter domain*. `get()` splices the held runtime value into the generated code. This is the only valid place to call it.
- **Outside the emitter domain** — calling `get()` throws `IllegalStateException` at compile time. You cannot observe runtime values at compile time.

### Capturing compile-time variables

Inside an emitter lambda, you can freely reference compile-time-local variables. They are captured by value and become compile-time constants in the generated code:

```java
static void classNameLength(ReflectClass<?> cls, Value<Integer> add) {
    int length = cls.getName().length(); // computed at compile time
    exit(() -> length + add.get());      // `length` is a constant; `add` is a runtime value
}
```

### Passing Value handles around

`Value<T>` handles are plain Java objects and can be stored in arrays, local variables, or passed to helper methods (that are `@CompileTime` or called from within a meta-method). The lambda you give to `emit()` / `lazy()` can then call `.get()` on any `Value<T>` it can reach:

```java
static void captureArray(Value<Integer> a, Value<String> b) {
    Value<?>[] parts = { a, emit(() -> ":"), b };
    exit(() -> String.valueOf(parts[0].get()) + parts[1].get() + parts[2].get());
}
```


## emit() — producing runtime code

```java
static <T> Value<T> emit(Computation<T> computation)
static void        emit(Action action)
```

`emit()` is the primary tool for generating runtime code. The lambda body you provide becomes a fragment of the generated method body.

- **`emit(Computation<T>)`** — the lambda returns a value; `emit` returns a `Value<T>` handle to that result.
- **`emit(Action)`** — the lambda returns nothing; use this for side-effecting statements.

Multiple `emit()` calls build up the generated method body sequentially:

```java
static <T> void createProxyWithBoxedParameters(ReflectClass<T> proxyType) {
    Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
        Value<StringBuilder> sb = emit(() -> new StringBuilder());  // allocate at runtime
        String name = method.getName();                              // compile-time constant
        emit(() -> sb.get().append(name).append('('));               // append name
        for (int i = 0; i < args.length; ++i) {
            Value<Object> arg = args[i];
            emit(() -> sb.get().append(',').append(arg.get()));
        }
        emit(() -> sb.get().append(')'));
        exit(() -> sb.get().toString());
    });
    exit(() -> proxy.get());
}
```

Compile-time variables (`name`, `arg`) referenced inside lambdas become inlined constants in the generated code. Runtime values are accessed via `.get()`.

### Emitting a Value as a computation

You can pass an existing `Value<T>` directly to `emit()` as a `Computation<T>` (because `Value<T>` is a supertype):

```java
Value<String> v = emit(() -> "hello");
Value<String> v2 = emit(v);  // emits code that reads the same runtime variable
```

## lazy() — conditional code emission

```java
static <T> Value<T> lazy(Computation<T> computation)
```

`lazy()` creates a `Value<T>` whose computation is *deferred* — the lambda body is only emitted into the generated code when (and each time) the resulting `Value<T>` is read via `.get()` inside another emitter.

This is the primary tool for **conditional dispatch** and **building chains** of decisions. The key insight is that `lazy()` never unconditionally emits code; the code is spliced in wherever the value is consumed.

### Idiom: conditional field lookup

```java
static void fieldType(ReflectClass<Object> cls, Value<String> name) {
    // Start with a default: "not found"
    Value<String> result = lazy(() -> null);

    for (ReflectField field : cls.getDeclaredFields()) {
        String type      = field.getType().getName(); // compile-time constant
        String fieldName = field.getName();           // compile-time constant
        Value<String> previous = result;              // capture current chain

        // Each iteration wraps the previous chain: if this field matches, return its type;
        // otherwise delegate to the rest of the chain.
        result = lazy(() -> fieldName.equals(name.get()) ? type : previous.get());
    }

    Value<String> type = result;
    exit(() -> type.get());
}
```

At runtime this generates a cascade of `if/else` checks without any reflection, because all field names and type strings are compile-time constants inlined by the code generator.

### Idiom: short-circuit evaluation

```java
static void withLazy(Value<WithSideEffect> a, Value<WithSideEffect> b) {
    Value<Boolean> first  = lazy(() -> a.get().getValue() > 0);
    Value<Boolean> second = lazy(() -> b.get().getValue() > 0);
    // `second` is only evaluated if `first` is false — standard || semantics
    exit(() -> first.get() || second.get() ? 1 : 2);
}
```

If `first.get()` is `true` at runtime, the `second` computation is never executed.

### lazyFragment()

```java
static <T> Value<T> lazyFragment(LazyComputation<T> computation)
```

A lower-level variant where the lambda itself returns a `Value<T>` (i.e. it calls `emit()` / `lazy()` internally). Prefer `lazy()` in most cases.


## exit() — returning values

```java
static void exit(Computation<?> returnValue)
static void exit()
```

`exit()` terminates the generated method by emitting a return statement.

- `exit(() -> expr)` — emit `return expr;` and stop generating.
- `exit()` — emit `return;` (for void methods).

`exit()` can be called conditionally in an `if/else` to generate different return paths:

```java
static void callDebug(ReflectClass<?> cls, Value<Object> obj) {
    ReflectMethod method = cls.getMethod("debug");
    if (method == null) {
        exit(() -> "missing");     // generates: return "missing";
    } else {
        exit(() -> method.invoke(obj.get()));  // generates: return obj.debug();
    }
}
```

After `exit()` returns to the meta-method, control flow in the meta-method continues normally, but further `emit()` calls after `exit()` extend **unreachable** branches in the generated IR; prefer not to emit anything meaningful after `exit()`.


## unsupportedCase()

```java
static void unsupportedCase()
```

Signals that the current set of compile-time arguments cannot be handled by this meta-method. When called, the compiler skips this usage and reports a compilation error (the specific call-site is flagged as unsupported). Use it as a guard when your meta-method only handles a known finite set of types:

```java
static void classNameLength(ReflectClass<?> cls, Value<Integer> add) {
    if (cls != findClass(Object.class) && cls != findClass(Integer.class)) {
        unsupportedCase();
        return;
    }
    // … handle the supported cases
}
```

## proxy() — generating anonymous implementations

```java
static <T> Value<T> proxy(Class<T> type,        InvocationHandler<T> handler)
static <T> Value<T> proxy(ReflectClass<T> type, InvocationHandler<T> handler)
```

Creates a new anonymous class at compile time that implements or extends `type`, generates a method body for each abstract method by calling `handler`, and returns a `Value<T>` holding an instance of the new class.

### InvocationHandler

```java
interface InvocationHandler<T> {
    void invoke(Value<T> proxy, ReflectMethod method, Value<Object>[] args);
}
```

`handler.invoke` is called *at compile time*, once per abstract method. It receives:

- `proxy` — a `Value<T>` representing `this` inside the generated method.
- `method` — compile-time reflection of the method being generated.
- `args` — `Value<Object>[]` for each parameter, boxed to `Object`. Primitive arguments are auto-boxed.

Inside `invoke`, use `emit()`, `lazy()`, and `exit()` exactly as in a normal meta-method. You must call `exit()` (or let the compiler insert a default return via falling off the end).

### Example: interface proxy

```java
@Meta
private static native <T> T createProxy(Class<T> proxyType, String add);
private static <T> void createProxy(ReflectClass<T> proxyType, Value<String> add) {
    Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
        String name = method.getName();          // compile-time: method name becomes a constant
        exit(() -> name + add.get());            // runtime: concatenate the name with `add`
    });
    exit(() -> proxy.get());
}
```

Calling `createProxy(MyInterface.class, "!")` generates a class whose every method returns `"<methodName>!"`.

### Default return values

If `invoke` does not call `exit()`, the compiler inserts a default return: `null` for object types, `0` for numeric types, `false` for booleans, `void` for void methods. This makes it easy to log method calls without caring about return types:

```java
Value<T> proxy = proxy(proxyType, (instance, method, args) -> {
    String name = method.getName();
    emit(() -> log.append(name + ";"));  // side effect only, no exit() call
});
```

### Boxing of primitive arguments

All `args` are `Value<Object>`. When the real parameter type is a primitive, the value is auto-boxed before being handed to the handler. The handler is responsible for unboxing if needed (e.g. by calling a typed method on the boxed value inside the emitter lambda).


## MetaprogrammingProvider — annotation-driven generators

As an alternative to `@Meta`, you can register a `MetaprogrammingProvider` via the Java SPI. This allows you to attach code generation to arbitrary annotations without touching the target class.

### Interfaces

```java
public interface MetaprogrammingProvider {
    MethodGenerator provide(ReflectMethodDescriptor method);
}

public interface MethodGenerator {
    void generate(MethodGeneratorContext context);
}

public interface MethodGeneratorContext {
    ReflectMethodDescriptor method();
    List<? extends Value<?>> parameters();
    Value<?> callReceiver();  // null for static methods
}
```

### Registration

Create a file `META-INF/services/org.teavm.metaprogramming.MetaprogrammingProvider` listing your implementation class, just like any Java SPI:

```
com.example.MyMetaprogrammingProvider
```

### How it works

1. TeaVM discovers all `MetaprogrammingProvider` implementations via SPI.
2. For every native method encountered during compilation, it calls `provider.provide(method)` on each registered provider.
3. If `provide` returns a non-null `MethodGenerator`, that generator's `generate` method is called with the context, and the generator uses `emit()`, `lazy()`, `exit()` to produce the method body.

### Example

```java
// Annotation that triggers generation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgWithType { }

// Provider (registered via SPI)
@CompileTime
public class MyProvider implements MetaprogrammingProvider {
    @Override
    public MethodGenerator provide(ReflectMethodDescriptor method) {
        if (method.getAnnotation(ArgWithType.class) != null) {
            return this::generate;
        }
        return null;
    }

    private void generate(MethodGeneratorContext ctx) {
        Value<StringBuilder> sb = emit(() -> new StringBuilder());
        for (int i = 0; i < ctx.method().getParameterCount(); i++) {
            String typeName = ctx.method().getParameterType(i).toString();
            Value<?> param = ctx.parameters().get(i);
            emit(() -> sb.get().append(typeName).append(": ").append(param.get()).append("\n"));
        }
        exit(() -> sb.get().toString());
    }
}

// Usage — just annotate a native method; no @Meta pairing needed
@ArgWithType
private static native String describe(int a, String b);
```

Calling `describe(1, "hello")` at runtime produces `"int: 1\njava.lang.String: hello\n"`.

The provider approach is suitable when the same generation strategy applies across many methods, or when you cannot modify the class that declares the target methods.


## @CompileTime — compile-time-only classes

```java
@Target({ ElementType.TYPE, ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CompileTime
```

Mark a class or a whole package with `@CompileTime` to indicate that this class should be picked by Metaprogramming API. Without it, you'll get error trying to call methods of the class from Metaprogramming API.

Use `@CompileTime` for:
- Helper classes that only call `emit()`, `lazy()`, etc.
- `MetaprogrammingProvider` implementations.
- Utility classes shared among multiple meta-methods.

Without `@CompileTime`, a class used from meta-methods may accidentally be pulled into the compiled output.

### Package-level annotation

Put `@CompileTime` in `package-info.java` to mark an entire package:

```java
// package-info.java
@CompileTime
package com.example.generators;

import org.teavm.metaprogramming.CompileTime;
```

All classes in the package inherit the annotation.

### Sharing generators across classes

A `@CompileTime` helper class can expose methods that call `emit()` / `lazy()`, and these methods can be called from any meta-method in the same compilation unit:

```java
@CompileTime
public class WrapperGenerator {
    public Value<String> wrap(String prefix, String suffix, String value) {
        return emit(() -> prefix + value + suffix);
    }
}

// In a meta-method:
static void compileTimeClass(Value<Boolean> ignored) {
    Value<String> result = new WrapperGenerator().wrap("[", "]", "foo");
    exit(() -> result.get());
}
```

## Reflection API

The compile-time reflection API (`ReflectClass`, `ReflectMethod`, `ReflectField`) mirrors `java.lang.reflect` but operates on compile-time class data. Instances are obtained via `Metaprogramming.findClass()` or received as meta-method parameters.

```java
static ReflectClass<?> findClass(String name)    // by fully-qualified name
static <T> ReflectClass<T> findClass(Class<T> cls)
static <T> ReflectClass<T[]> arrayClass(ReflectClass<T> componentType)
```

### ReflectClass&lt;T&gt;

Mirrors `java.lang.Class`. Key additions over standard reflection:

| Method | Description |
|--------|-------------|
| `asJavaClass()` | Convert to `Class<T>` for use inside emitter lambdas |
| `createArray(Value<Integer> size)` | Emit array creation |
| `getArrayElement(Value<Object> array, Value<Integer> index)` | Emit array element access |
| `getArrayLength(Value<Object> array)` | Emit array length read |

Use `isAssignableFrom(Class<?>)` and `isAssignableFrom(ReflectClass<?>)` to check type compatibility at compile time.

### ReflectMethod

```java
Object invoke(Object obj, Object... args)       // emit an instance/static method call
Object construct(Object... args)                // emit a constructor call
```

Both methods are called *inside an emitter lambda*. Their arguments are either `Value<?>` handles (unwrapped by the framework) or plain objects (treated as compile-time constants). The return value is a `Value<T>` when returned from an emitter lambda.

```java
// Call an instance method
exit(() -> method.invoke(obj.get(), a.get(), b.get()));

// Invoke a constructor
exit(() -> ctor.construct(a.get(), b.get()));
```

Look up constructors by the special name `"<init>"`:

```java
ReflectMethod ctor = type.getMethod("<init>", stringClass, intClass);
```

`getMethods()` / `getDeclaredMethods()` return all methods visible/declared on the class. `getMethod()` searches the entire hierarchy; `getDeclaredMethod()` looks only in the current class. The `J` variants (`getDeclaredJMethod`, `getJMethod`) accept `Class<?>` instead of `ReflectClass<?>` for convenience.

### ReflectField

```java
Object get(Object target)              // emit a field read
void   set(Object target, Object value) // emit a field write
```

Again, called inside emitter lambdas:

```java
exit(() -> field.get(obj.get()));       // emit: return obj.fieldName;
emit(() -> field.set(obj.get(), val));  // emit: obj.fieldName = val;
```

### Annotations

All reflection types implement `ReflectAnnotatedElement`:

```java
<T extends Annotation> T getAnnotation(Class<T> type)
```

Annotation instances returned at compile time are real Java annotation proxy objects that you can read normally:

```java
TestAnnotation ann = method.getAnnotation(TestAnnotation.class);
if (ann != null) {
    String value = ann.a();  // read annotation attribute at compile time
}
```

## Diagnostics

```java
static Diagnostics getDiagnostics()

interface Diagnostics {
    void error(SourceLocation location, String message, Object... params)
    void warning(SourceLocation location, String message, Object... params)
}
```

Report compile-time errors and warnings. Errors cause compilation to fail; warnings are informational. `params` may include `ReflectClass`, `ReflectMethod`, `ReflectField`, or `Class<?>` instances — they are formatted as their respective names.

In the message you should put placeholders for arguments. Arguments have form: `{{<specifier><index>}}`,
where specifier is one of:

* `t` - matches `ReflectClass`;
* `m` - matches `ReflectMethod`;
* `f` - matches `ReflectField`;

and <index> is the index of the argument, zero-based.

```java
Metaprogramming.getDiagnostics().error(
    new SourceLocation(callerMethod),
    "Unsupported type: {{t0}}",
    cls
);
```

## Source locations

```java
static void         location(String fileName, int lineNumber)
static void         defaultLocation()
static SourceLocation getLocation()
```

By default, emitted instructions carry no debug source location. Use `location()` to associate subsequent `emit()` / `lazy()` / `exit()` calls with a specific source position (for source-maps and error messages). `defaultLocation()` removes the override.

`getLocation()` returns the current forced location (or the location of the original call-site if no override is set), useful for passing to `Diagnostics`.

## Utilities

### getClassLoader()

```java
static ClassLoader getClassLoader()
```

Returns the class-loader used during compilation. Useful for loading resources or classes by name using standard Java APIs.

### getResources()

```java
static Iterator<Resource> getResources(String name)
```

Iterates over classpath resources with the given name. Useful for processing data files at compile time and embedding results as constants in the generated code.

```java
static void initFromConfig(ReflectClass<?> cls) {
    Iterator<Resource> resources = getResources("config.properties");
    while (resources.hasNext()) {
        try (InputStream is = resources.next().open()) {
            Properties props = new Properties();
            props.load(is);
            String val = props.getProperty("key");
            emit(() -> System.out.println(val));  // val is a compile-time constant
        }
    }
}
```

### createClass()

```java
static ReflectClass<?> createClass(byte[] bytecode)
```

Submits a dynamically-generated class (as raw bytecode) into the compilation unit and returns a `ReflectClass<?>` for it. The class is treated like any other class — it can be used with `proxy()`, methods can be invoked via `ReflectMethod`, etc.
