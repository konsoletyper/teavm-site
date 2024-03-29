TeaVM was designed to create web applications. One important requirement for web applications is their size.
Nobody will use a compiler which produces tens of megabytes of JS. That's why tools like TeaVM and GWT
perform advanced optimizations to reduce code size. Unfortunately, it's impossible to make these optimizations
reflection-friendly. Even if TeaVM implemented reflection, any attempt to use it could lead to "dependency explosion",
i.e. huge JS file. That's why TeaVM does not provide reflection support. Instead, it comes with its own replacement,
called metaprogramming API.


# Essence

Unlike Java reflection, TeaVM metaprogramming does not work in runtime. It allows you to write a code that runs
in the compile-time. TeaVM can query from compiler much about classes, methods and fields and generate JS code
that will be executed at run-time. 

> One, familiar with GWT can find this approach very close to deferred binding (aka generators).
> However, TeaVM's approach is slightly more powerful, as you can see below.


# Quick start

Let's start by writing a method which reads field called `foo` or returns `null` if such field is not available.

```java
    public static Object getFoo(Object obj) {
        return getFooImpl(obj.getClass(), obj);
    }

    @Meta
    private static native Object getFooImpl(Class<?> cls, Object obj);

    private static void getFooImpl(ReflectClass<Object> cls, Value<Object> obj) {
        if (!whitelist(cls)) {// TODO important to whitelist classes you are
            unsupportedCase();// using in metaprogramming, otherwise it will
            return;           // increase size of generated javascript dramatically
        }                     // or lead to long compilation time
        var field = cls.getField("foo");
        if (field != null) {
            exit(() -> field.get(obj));
        } else {
            exit(() -> null);
        }
    }
```

Here, our method simply delegates all work to `getFooImpl`, which is a native method marked with `@Meta` annotation.
This annotation does all magic. It tells compiler to generate body of `getFooImpl` by invoking another `getFooImpl`
method with slightly different signature. It's quite easy to find the difference: returning value must be `void`,
the `ReflectClass` argument must correspond to `Class` and `Value` argument must correspond to any other argument.
Note that only one argument can be of `ReflectClass` type.

> GWT's entry point to generators are special classes created by `GWT.create` method, which requires
> single class literal argument (class variable are not allowed). TeaVM's approach looks similar, however,
> it does not restrict developer to the literal argument. You can get class from anywhere, for example,
> by calling Object.getClass().

The second `getFooImpl` which has actual Java code is executed in run-time. It provides access to classes available
to compiler via API which resembles Java reflection, with `Reflect` prefix added to each class. We use it
to find a field called "foo".

To get field's value and return it from method, we use `Metaprogramming.exit` (or just `exit`, since there's
a convention to always statically import entire `Metaprogramming` class). This method takes lambda which will be
again executed in run-time and causes original `getFoo` method to return evaluated expression.

We can now test this method:

```java
class A {
    private String foo;
    
    A(String foo) { this.foo = foo; }
}
class B {
}

public static void main(String[] args) {
    System.out.println(getFoo(new A("barbaz"));
    System.out.println(getFoo(new B()));
}
```

So, we can freely switch between compile-time and run-time. To switch from runtime to compile-time, we
declare a pair of methods with equal names and similar signatures, mark first one with `@Meta` annotation.
To switch back, we call special method like `exit` or `emit`.

> GWT requires you to generate Java code in compile-time which it further compiles to JavaScript.
> TeaVM works with bytecode so it would be hard to write the code that generates byte-code.
> Mataprogramming API does byte-code generation for you using lambdas as templates.


# `@Meta` annotation

`@Meta` annotation must be put on a static native method. Another static non-abstract non-native method with the 
same name must exist in the same class. Its signature must correspond to methods of the original method:

* It should be `void` no matter which type is returned by the original method.
* Any `Class` argument can be mapped to `ReflectClass` argument.
* Any other argument must be mapped to `Value` argument.
* There can be at most one `ReflectClass` argument.

`@Meta` annotation causes the first method's body to be generated by the second method which runs in compile-time.


# Emitting bytecode

There are several methods which can be called from compile-time to generate code. All they take lambda as an
argument, and simply write the lambda's body to the generated code. Unlike normal lambdas, template lambdas
have some restrictions over variables they can capture.

* It's allowed to capture primitive values (numbers, strings).
* It's allowed to capture `Value`, `ReflectClass`, `ReflectField`, `ReflectMethod`.
* Any other value is disallowed.

These captured variables act as template parameters.

The main (and the simplest) one is `Metaprogramming.emit`. It simply writes template as-is. `exit` method
writes template and additional `return` statement which returns expression evaluated by lambda.
`lazy` does not write template immediately. Instead, it produces `Value` which is written as soon as
accessed by another lambda.


# Passing data between template lambdas

In the real world templates can't be isolated. Value produced by one template may be needed to another template.
TeaVM uses `Value` for this purpose. Methods like `emit` and `lazy` produce `Value`. Value can't be read at
compile-time. The only way to read `Value` is to capture it by another lambda and call `get` method there.
For example:

```java
Value a = emit(() -> 2);
Value b = emit(() -> a.get() + 3);
exit(() -> b.get());
```


# Reflection restrictions

You can use metaprogramming reflection much like usual Java reflection. However, it imposes several restrictions:

* You can't search and enumerate fields and methods of a class in template lambdas.
* You can't get and set value of `ReflectField` in compile-time.
* You can't do `ReflectMethod.invoke` methods in compile-time.


# Proxies

Of course, there is an alternative to reflection proxies. You can call `Metaprogramming.proxy` method
which accepts interface and `InvocationHandler`.
