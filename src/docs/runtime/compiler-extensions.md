TeaVM provides several extension points that let you customize compiler behavior from your own library
or application code, without modifying TeaVM itself.
Extensions are discovered through Java's standard
[service-provider interface (SPI)](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism.


## Dependencies

The extension API is split into two artifacts published under the `org.teavm` group:

* `teavm-extension-spi` — interfaces you implement
* `teavm-extension-annotation-processor` — annotation processor that auto-registers your implementations

### Gradle

**Application projects** (applying `id("org.teavm")`) have both artifacts wired in automatically:
`teavm-extension-spi` is placed on the compile classpath and the annotation processor is active.
No extra declarations are needed.

**Library projects** (applying `id("org.teavm.library")`) have the annotation processor wired in
automatically, but you still need to declare the SPI dependency.
Use the `teavm.libs.spi` shortcut provided by the TeaVM DSL:

```groovy
dependencies {
    compileOnly teavm.libs.spi
}
```

**Projects without a TeaVM plugin** — for example, a standalone library that is later placed on
the TeaVM compiler's classpath — declare both artifacts explicitly:

```kotlin
dependencies {
    compileOnly("org.teavm:teavm-extension-spi:VERSION")
    annotationProcessor("org.teavm:teavm-extension-annotation-processor:VERSION")
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-extension-spi</artifactId>
        <version>VERSION</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.teavm</groupId>
                        <artifactId>teavm-extension-annotation-processor</artifactId>
                        <version>VERSION</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```


## Registering an extension

Each extension point is a Java interface discovered via `ServiceLoader`.
You register an implementation by creating a file under `META-INF/services/` whose name is the
fully-qualified interface name and whose content is the fully-qualified name of your implementation class.

The `@Autoregistered` annotation (from `org.teavm.extension`) automates this step.
Annotate your class with it and the annotation processor generates the `META-INF/services/` entry for you:

```java
import org.teavm.extension.Autoregistered;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

@Autoregistered
public class MyReflectionPolicy extends SimpleReflectionPolicy {
    // ...
}
```

In Kotlin projects, `@Autoregistered` requires the `kapt` plugin; it does not work with KSP.

Without the annotation processor you can register an implementation manually.
For example, for `ReflectionPolicy` create the file
`META-INF/services/org.teavm.extension.spi.reflection.ReflectionPolicy`
containing the fully-qualified name of your class.


## Extension points

### ReflectionPolicy

`org.teavm.extension.spi.reflection.ReflectionPolicy` controls Java reflection behavior
in compiled code.

By default TeaVM does not support arbitrary reflection, because it would prevent dead-code elimination.
`ReflectionPolicy` lets you declare exactly:

* which members (fields, methods) are accessible via the reflection API,
* which classes are findable by `Class.forName()`,
* which interfaces support `java.lang.reflect.Proxy`.

The interface has four methods, all with default no-op implementations:

```java
public interface ReflectionPolicy {
    // Called once at compiler startup
    default void initialize(ExtensionEnvironment env) { }
    // Return the set of members of cls that are accessible via reflection
    default Collection<IntrospectMember> classAccessibleMembers(IntrospectClass<?> cls) { ... }
    // Return true if Class.forName() should be able to locate cls
    default boolean isClassFoundByName(IntrospectClass<?> cls) { ... }
    // Enumerate the interface combinations that need dynamic Proxy support
    default ProxyListener getProxyInterfaces(ProxyInterfaceConsumer consumer) { ... }
}
```

#### SimpleReflectionPolicy

For most cases extend `SimpleReflectionPolicy` and implement the required `setup()` method.
It provides a fluent DSL that handles all four methods internally:

```java
@Autoregistered
public class MyReflectionPolicy extends SimpleReflectionPolicy {
    @Override
    protected void setup() {
        // Make all public members of classes annotated with @MyReflectable accessible
        selectClasses(withAnnotation(MyReflectable.class))
                .reflectablePublicMembers()
                .foundByName();

        // Make all public fields and methods in a specific package accessible
        selectPackage("com.example.dto")
                .reflectablePublicFields()
                .reflectablePublicMethods();

        // Custom predicate: instance methods whose name starts with "get"
        allClasses()
                .reflectableMethods(INSTANCE.and(namePattern("get*")));

        // Each interface annotated with @Proxiable gets a Proxy implementation generated for it
        selectClasses(withAnnotation(Proxiable.class)).proxyable();

        // Static proxy: generate a Proxy for a fixed combination of interfaces
        proxyGroup(Runnable.class, Callable.class);
    }
}
```

**Class selection methods** return a `ClassPolicy` object on which you chain further configuration:

| Method | Description |
|---|---|
| `allClasses()` | Every class on the classpath |
| `selectClass(String name)` | A single class by fully-qualified name |
| `selectPackage(String pkg)` | Direct members of a package (non-recursive) |
| `selectPackage(String pkg, true)` | All classes in a package and its sub-packages |
| `selectClasses(Predicate)` | Arbitrary class predicate |

**`ClassPolicy` configuration methods:**

| Method | Description |
|---|---|
| `foundByName()` | Allow `Class.forName()` for matched classes |
| `reflectablePublicMembers()` | All public fields and methods |
| `reflectableMembers(Predicate)` | Fields and methods matching a predicate |
| `reflectablePublicMethods()` | All public methods |
| `reflectableMethods(Predicate)` | Methods matching a predicate |
| `reflectablePublicFields()` | All public fields |
| `reflectableFields(Predicate)` | Fields matching a predicate |
| `proxyable()` | Generate `Proxy` support for this interface |
| `proxyableWith(Class<?>... extras)` | Generate proxy combining this interface with `extras` |

**Built-in member predicates:**

`PUBLIC`, `PROTECTED`, `PRIVATE`, `PACKAGE_PRIVATE`, `ABSTRACT`, `STATIC`, `INSTANCE`, `FINAL`,
`TRANSIENT` (fields only), `VOLATILE` (fields only), `NATIVE` (methods only),
`SYNCHRONIZED` (methods only), `DEFAULT_METHOD`

**Built-in class predicates:**

`ENUM`, `ANNOTATION`, `INTERFACE`, `IS_CLASS`

**Helper predicate factories:**

| Method | Applicable to | Description |
|---|---|---|
| `named(String)` | any element | Exact name match |
| `namePattern(String)` | any element | Glob pattern (`*` matches within a segment, `**` crosses `.` boundaries) |
| `withAnnotation(Class)` or `withAnnotation(String)` | annotated elements | Element carries the given annotation |
| `extending(Class)` or `extending(String)` | classes | Class is a subtype of the argument |
| `inPackage(String)` | classes | Class is a direct member of the package (non-recursive) |
| `inPackage(String, boolean)` | classes | Class is in the package, optionally recursive |
| `withSignature(Class<?>...)` | methods | Method has exactly these parameter types |
| `withParameterCount(int)` | methods | Method has this number of parameters |
| `withReturnType(Class)` or `withReturnType(String)` | methods | Method returns this type |
| `ofType(Class)` or `ofType(String)` | fields | Field has this type |

Predicates can be combined with standard `Predicate` methods: `.and()`, `.or()`, `.negate()` (or the
`not()` helper provided by `SimpleReflectionPolicy`).


### ResourcesPolicy

`org.teavm.extension.spi.resources.ResourcesPolicy` controls which classpath resources are embedded
in the compiled output.

```java
public interface ResourcesPolicy {
    // Called once at compiler startup
    default void initialize(ExtensionEnvironment env) { }
    // Return the resource paths (relative to classpath root) to embed
    default String[] supplyResources(Collection<? extends String> availableClassNames) {
        return new String[0];
    }
}
```

`supplyResources` receives the set of class names included in the compiled output, so you can
conditionally include resources based on which classes are reachable.
Extend `DefaultResourcesPolicy` if you need access to the `environment()` accessor:

```java
@Autoregistered
public class MyResourcesPolicy extends DefaultResourcesPolicy {
    @Override
    public String[] supplyResources(Collection<? extends String> availableClassNames) {
        if (availableClassNames.contains("com.example.MyClass")) {
            return new String[] { "com/example/data.json" };
        }
        return new String[0];
    }
}
```


### SubstitutionPolicy

`org.teavm.extension.spi.substitution.SubstitutionPolicy` maps class names from one package
hierarchy to another during compilation.
This is useful for transparently replacing platform-specific APIs with portable equivalents,
or for renaming an entire package tree.

```java
public interface SubstitutionPolicy {
    void contribute(SubstitutionSink sink);
}
```

`SubstitutionSink` lets you select classes and describe how to rename them:

```java
public interface SubstitutionSink {
    // Select classes whose fully-qualified names match the predicate, then configure substitution rules
    ClassSubstitutionPolicy selectClasses(Predicate<String> predicate);
    // Convenience: replace the entire `from` sub-tree with `to`
    default void substitutePackage(String from, String to) { ... }
}
```

`ClassSubstitutionPolicy` describes how a matched class name is transformed into the replacement name:

| Method | Description |
|---|---|
| `packagePrefix(String)` | Prepend this string before the package name |
| `packageSuffix(String)` | Append this string after the package name |
| `simpleNamePrefix(String)` | Prepend this string before the simple class name |
| `simpleNameSuffix(String)` | Append this string after the simple class name |
| `replacePackage(String from, String to)` | Swap the package prefix `from` for `to` |
| `dontFallbackWhenNoSubstitution()` | Do not fall back to the original class when no substitution is found for a matched class |

Extend `SimpleSubstitutionPolicy` to get predicate helpers (`named()`, `namePattern()`, `inPackage()`):

```java
// Scenario: you have a cross-platform game that uses an Android-like OpenGL ES API
// (android.opengl.GLES20, GLES30, …). For the browser target you've written WebGL-based
// drop-in replacements under com.example.webgl.*. This policy makes the TeaVM compiler
// transparently redirect all OpenGL ES references to those replacements.

@Autoregistered
public class GLSubstitutionPolicy extends SimpleSubstitutionPolicy {
    @Override
    public void contribute(SubstitutionSink sink) {
        // android.opengl.GLES20  →  com.example.webgl.GLES20
        // android.opengl.GLES30  →  com.example.webgl.GLES30
        // android.opengl.ETC1    →  com.example.webgl.ETC1, …
        sink.substitutePackage("android.opengl", "com.example.webgl");

        // Any other android.* class has no browser equivalent — fail loudly rather than
        // silently trying to compile the original Android SDK class.
        sink.selectClasses(inPackage("android", true))
                .dontFallbackWhenNoSubstitution();
    }
}
```


### MetaprogrammingProvider

`org.teavm.metaprogramming.MetaprogrammingProvider` lets you register compile-time code generators.
It is covered in detail in the [Metaprogramming](/docs/runtime/metaprogramming.html) article.


## ExtensionEnvironment

`ReflectionPolicy.initialize()` and `ResourcesPolicy.initialize()` both receive an
`ExtensionEnvironment`. It extends `IntrospectWorld` and provides:

* **Type introspection** — `findClass(String name)`, `findClass(Class<T> cls)` return
  `IntrospectClass<T>` objects that let you inspect the class hierarchy, fields, methods,
  annotations, and generic type parameters of any class on the classpath.
* **`resources(String name)`** — iterate `Resource` instances for a given classpath-relative path.
* **`diagnostics()`** — emit compilation errors and warnings via the `Diagnostics` API.
* **`classLoader()`** — access the class loader used during compilation.
* **`property(String name)`** — read compiler properties (the same properties you can set
  in Maven/Gradle configuration).
