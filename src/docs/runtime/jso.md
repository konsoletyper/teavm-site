TeaVM runs in browser and can't be isolated from browser's environment.
Moreover, if you use TeaVM, you probably use it to alter an HTML page or draw something in Canvas element.
Of course, TeaVM can interact with built-in JavaScript APIs as well as with existing JavaScript libraries.
TeaVM was designed as a modular compiler, so core knows nothing about interaction with JavaScript.
You need a TeaVM extension for this purpose.
Fortunately, TeaVM comes with a bundled-in extension called JSO.
Also, there is DOM module, that has existing wrappers around popular browser APIs.
This section shows how to use JSO to interact with an existing JavaScript code.

Note, that if you are familiar with GWT, you can find that JSO concepts are quite similar to approach taken by GWT.

*The API, described above, only works for JavaScript and WebAssembly GC backends.
It won't work with classic (aka MVP) WebAssembly, WASI and C backends.* 


## Maven dependencies

To create your own wrappers, you should include the following

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-jso</artifactId>
  <version>${teavm_version}</version>
</dependency>
```

To use existing wrapper, you may also include the following

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-jso-apis</artifactId>
  <version>${teavm_version}</version>
</dependency>
```


## Running JavaScript code from Java

To execute JavaScript code, you should declare native method and mark it with [@JSBody](/javadoc/0.5.x/jso/core/org/teavm/jso/JSBody.html) annotation.
`@JSBody` has two parameters.
First, `params`, specifies which names in JavaScript correspond to parameters in Java, by position.
The number of items of `params` array must be equal to the number of parameters of the method.
Second, `script`, is JavaScript code.

Example:

```java
@JSBody(params = { "message" }, script = "console.log(message)")
public static native void log(String message);
```

### Using modules from `@JSBody`

It's possible to import external modules to use in `@JSBody` scripts. For this purpose use `imports` parameter.

For example, we have a module named `testModule.js`:

```js
export function foo() {
    console.log("foo called");
}
```

to access this function, you can write following declaration in TeaVM:

```java
@JSBody(
        script = "return testModule.foo();",
        imports = @JSBodyImport(
                alias = "testModule",
                fromModule = "testModule.js"
        )
)
private static native int callModule();
```

## Calling Java from JavaScript

There are two ways to call Java code from JavaScript.
The preferred way is to use overlay types and functors to pass Java callbacks to JavaScript.
Read about overlay types below.
Another way is to call Java method directly from `@JSBody` script.
To call Java method from JavaScript, use the following syntax:

```javascript
return javaMethods.get('method-reference').invoke(parameters);
```

where `method-reference` consists of fully qualified class name followed by method descriptor as described in [JVM Specification](https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3). Parameters should be specified like parameters of function invocation.
If you call member method, the first parameter corresponds to invocation target.
See example below:

```java
@JSBody(params = { "str", "count" }, script = ""
        + "return javaMethods.get('java.lang.String.substring(II)Ljava/lang/String;')"
                + ".invoke(str, 0, count);")
public static native void left(String str, int count);
```

Note that you can't pass non-constant string to `javaMethods.get` method.


## Overlay types

Often you are not satisfied exchanging primitives with JavaScript.
JSO comes with concept of *overlay types*, similar to overlay types in GWT.
Overlay types allow to talk with JavaScript in terms of objects.
However, as JSO is built upon Java, overlay objects are almost regular Java objects,
so you get all advantages of static type system, IDE support, javadoc, etc.

An overlay type is a class or an interface that meets the following conditions:

* It must extend or implement [JSObject](/javadoc/${teavm_branch}/jso/core/org/teavm/jso/JSObject.html) interface, directly or indirectly.
* It must not have member fields.
* Final member methods must not implement or override parent method.
* In case of abstract class, it should extend either another overlay abstract class or `java.lang.Object`.
* If it's non-abstract class, it should be annotated with `@JSClass`.

By default, each abstract or native method of an overlay class is mapped to a corresponding method of JavaScript,
i.e. when you call a Java method, the JavaScript method of the same name is actually called.
Parameters are converted before invocation to JavaScript and return value is converted back to Java.

For example,

```java
public interface Node extends JSObject {
    void appendChild(Node newChild);

    Node cloneNode(boolean deep);

    //...
}
```


## Mapping properties

To access JavaScript properties from Java, you should declare getter and setter methods, both optional.
Getters and setters must satisfy Java Beans naming conventions.
You also must annotate getters and setters with the
[@JSProperty](/javadoc/${teavm_branch}/jso/core/org/teavm/jso/JSProperty.html) annotation.
By default, these methods will access the JavaScript property with the corresponding name, but
you can define another property name in *@JSProperty*.

For example,

```java
public interface HTMLElement extends Element {
    @JSProperty
    String getTitle();

    @JSProperty()
    void setTitle(String title);

    //...
}
```

## Mapping constructors

To map a JS constructor into Java, just declare non-abstract class, annotate it with `@JSClass` and
define constructors (their body may be either empty or contain some logic; in latter case TeaVM will
ignore any code inside constructor). For example:

```java
@JSClass
public class Int8Array extends ArrayBufferView {
  public Int8Array(int length) {
  }
  public Int8Array(ArrayBuffer buffer) {
  }
  // etc
}
```


## Extension methods

You can embed your custom logic to existing JavaScript object by declaring non-abstract non-native methods 
in overlay types.
These methods, however, have additional restriction: they should not override methods of a parent class or interface.
*Please, note that the current version of TeaVM does not validate this, so you violate this restriction on your
risk*. Future versions of TeaVM will check this rule properly.

Example:

```java
public interface HTMLElement extends JSObject {
    @JSProperty
    CSSStyleDeclaration getStyle();

    // Subtypes can't override these methods
    default void hide() {
        getStyle().setProperty("display", "none");
    }

    default void show() {
        getStyle().removeProperty("display");
    }
}
```

Also, you can declare `@JSBody` methods in abstract classes. Rewrite our example this way:

```java
public abstract class HTMLElement implements JSObject {
    @JSBody(script = "this.style.display = 'none';")
    public native void hide();

    @JSBody(script = "this.style.display = '';")
    public native void show();
}
```


## Static methods

Overlay classes can declare static methods, either with Java or JavaScript implementation.
Example:

```java
@JSClass("Array")
public class JSArray<T extends JSObject> implements JSObject {
    public JSArray() {
    }

    // Does not exist in JS Array, implemented on Java side 
    public static <S extends JSObject> JSArray<S> of(
            Collection<S> elements) {
        var array = new JSArray<S>(elements.size());
        for (int i = 0; i < elements.size(); ++i) {
            array.set(i, elements.get(i));
        }
        return array;
    }

    // Wraps JS method Array.isArray
    public static native boolean isArray(Object object);
}
```


## Wrapping indexers

To access JavaScript objects as arrays or maps, you can declare indexer methods.
Indexer methods are either get indexers or set indexers.

* getter indexers take one parameter and return a non-void value;
* setter indexers take two parameters; first is index, second is value to set;

You are free to name your indexers as your want.

To tell TeaVM that method is either get or set indexer, you should annotate it with
[@JSIndexer](/javadoc/${teavm_branch}/jso/core/org/teavm/jso/JSIndexer.html).

For example,

```java
public interface Int8Array extends JSObject {
    @JSIndexer
    byte get(int index);

    @JSIndexer
    void set(int index, byte value);

    //...
}
```


## Passing Java objects to JavaScript

Some JavaScript APIs expect that you pass a callback object.
You can simply implement *JSObject* interfaces in Java and pass these implementations to JavaScript wrappers.
However, these implementation can only support method invocations, no properties, indexers and constructors.

For example, if you have

```java
public interface Element extends JSObject {
    //...

    void addEventListener(String type, EventListener listener);

    //...
}
public interface EventListener extends JSObject {
    void handleEvent(Event evt);
}
```

you can do the following:

```java
var window = Window.current();
var element = window.getDocument().getElementById("my-elem");
element.addListener("click", evt -> window.alert(evt));
```


## Passing Java objects as JavaScript functions

Often, JavaScript APIs expect you to pass a callback function.
This case is similar to passing as JavaScript objects, however you need to tell TeaVM to pass your
Java classes as JavaScript functions.
To do this, simply add the [@JSFunctor](/javadoc/${teavm_branch}/jso/core/org/teavm/jso/JSFunctor.html) annotation.
Functor interfaces must contain exactly one method.

For example,

```java
@JSFunctor
public interface TimerHandler extends JSObject {
    void onTimer();
}

@JSBody(params = { "handler", "delay" }, script = "setTimeout(handler, delay);")
static native void setTimeout(TimerHandler handler, int delay);

static void doWork() {
    var doc = HTMLDocument.current();
    setTimeout(() -> doc.getBody().appendChild(doc.createTextNode("-")), 1000);
}
```


## Passing arrays without copying

By default, TeaVM copies all arrays in the gap between JavaScript and Java.
To override this behaviour, use `@JSByRef` annotation on parameters or method.
For example:

```java
@JSByRef
@JSBody(script = "return new Int32Array(10);")
private native int[] getArrayFromJS();

@JSBody(params = "array", script = "console.log(array.byteLength);")
private native void passArrayToJs(@JSByRef float[] array);
```

You should be careful when using `@JSByRef` with return type. In Java arrays never overlap, but using
`@JSByRef` you can make TeaVM violate this contract:

```java
@JSByRef(params = "array", script = "return new Int8Array(array.buffer, 1);")
private native byte[] subarray(@JSByRef byte[] array);
```

This can have unexpected consequences and non-obvious errors. Please, avoid this!

*`@JSByRef` annotation is not supported by WebAssembly GC backend.
This is not a limitation of TeaVM, but a limitation of the WebAssembly GC spec itself.*


## Defining top-level functions and properties

You can also define top-level functions and properties using `@JSTopLevel` with `static` class methods.
For example:

```java
public class Window {
  @JSTopLevel
  public static native String atob(String s);

  @JSTopLevel
  public static native String btoa(String s);
  
  @JSTopLevel
  @JSProperty
  public static native HTMLDocument getDocument();
}
```

## Importing declarations from module

You can import classes, functions and properties from external modules.
To do so, annotate corresponding elements with `@JSModule`.
For example:

```java
@JSClass
@JSModule("./myModule.js")
public class ImportedClass implements JSObject {
}
```

```java
@JSClass
public class ImportedDeclarations implements JSObject {
  @JSTopLevel
  @JSModule("./myModule.js")
  public static native void someFunction();

  @JSTopLevel
  @JSProperty
  @JSModule("./myModule.js")
  public static native String getSomeProperty();
}
```

## Conversion rules

TeaVM automatically converts from and to JS following types:

* `boolean`, `byte`, `short`, `int`, `float`, `double` which correspond to JavaScript numeric values.
* `java.lang.String` which corresponds to JavaScript `String` object.
* arrays of objects and primitives listed above.

TeaVM **does not** convert Java collections and primitive wrappers.
Additionally, TeaVM only performs conversion when type is directly known from method's signature.
This means that with generics you won't get expected results, because type arguments from generics
only known at compile time.

In following example TeaVM is able to convert JS string to `java.lang.String`:

```java
private static void test() {
  System.out.println(read());
}

@JSBody(script = "return document.getElementById('value-input').value;")
private static native String read();
```

however, with generics TeaVM will produce `ClassCastException` on runtime:

```java
private static void test() {
    readAsync().then(value -> System.out.println(value));
}

private static native JSPromise<String> readAsync();
```

the right way to fix this is to declare JS wrapper as the type argument:

```java
private static void test() {
    readAsync().then(value -> System.out.println(value.stringValue()));
}

private static native JSPromise<JSString> readAsync();
```


## Dynamic type casting

TeaVM only supports `instanceof` against non-interface overlay types.
The reason is that there's no such thing as "interface" in JavaScript.
Some APIs in JavaScript declare that they consume or produce an object with given properties,
but this object should not necessarily extend some class.
Due to duck typing in JavaScript, there's no need to declare interfaces.

To express such APIs in statically typed Java, you can use interfaces, but these interfaces don't exist on runtime.
Additionally, you may want to express such "anonymous" JavaScript object with abstract classes.
In this case you can prevent TeaVM from inserting type checks for such classes 
by adding `@JSClass(transparent = true)`.
For example:

```java
// `instanceof SomeClass` will always produce true
@JSClass(transparent = true)
public abstract class SomeClass {
    public abstract void foo();
    
    @JSProperty
    public abstract String getBar();
}
```