---
title: Interacting with JavaScript
permalink: /docs/runtime/jso/
---

TeaVM runs in browser and can't be isolated from browser's environment.
Moreover, if you use TeaVM, you probably use it to alter an HTML page or draw something in Canvas element.
Of course, TeaVM can interact with built-in JavaScript APIs as well as with existing JavaScript libraries.
TeaVM was designed as a modular compiler, so core knows nothing about interaction with JavaScript.
You need a TeaVM extension for this purpose.
Fortunately, TeaVM comes with a bundled-in extension called JSO.
Also, there is DOM module, that has existing wrappers around popular browser APIs.
This section shows how to use JSO to interact with an existing JavaScript code.

Note, that if you are familiar with GWT, you can find that JSO concepts are quite similar to approach taken by GWT.


Maven dependencies
------------------

To create your own wrappers, you should include the following

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-jso</artifactId>
  <version>0.5.1</version>
</dependency>
```

To use existing wrapper, you may also include the following

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-jso-apis</artifactId>
  <version>0.5.1</version>
</dependency>
```


Running JavaScript code from Java
---------------------------------

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

`@JSBody` method is restricted to take and return only values of the following types, called transferable types:

* primitives, except for `long`;
* strings;
* overlay types (see below);
* array of transferable types.

Examples are: `int`, `boolean`, `short[][]`, `org.teavm.dom.html.HTMLElement[]`.
The following types are invalid: `java.lang.Object`, `java.lang.Short[]`, `java.util.Date`, `java.util.List`.


Calling Java from JavaScript
----------------------------

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


Overlay types
-------------

Often you are not satisfied exchanging primitives with JavaScript.
JSO comes with concept of *overlay types*, similar to overlay types in GWT.
Overlay types allow to talk with JavaScript in terms of objects.
However, as JSO it built upon Java, overlay objects are almost regular Java objects,
so you get all advantages of static type system, IDE support, javadoc, etc.

An overlay type is an abstract class or an interface that meets the following conditions:

* It must extend or implement [JSObject](/javadoc/0.5.x/jso/core/org/teavm/jso/JSObject.html) interface, directly or indirectly.
* It must not have member fields.
* All of its methods are either abstract or static or final; no member methods with implementation are allowed.
* Final member methods must not implement or override parent method.
* In case of abstract class, it should extend either another overlay abstract class or `java.lang.Object`.
* Abstract class should not declare constructor.

By default each abstract method of an overlay class is mapped to a corresponding method of JavaScript, i.e. when you
call a Java method, the JavaScript method of the same name is actually called.
Parameters are converted before invocation to JavaScript and return value is converted back to Java.

For example,

```java
public interface Node extends JSObject {
    void appendChild(Node newChild);

    Node cloneNode(boolean deep);

    //...
}
```


Wrapping properties
-------------------

To access JavaScript properties from Java, you should declare getter and setter methods, both optional.
Getters and setters must satisfy Java Beans naming conventions.
You also must annotate getters and setters with the
[@JSProperty](/javadoc/0.5.x/jso/core/org/teavm/jso/JSProperty.html) annotation.
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


Non-abstract methods
--------------------

You can embed your custom logic to existing JavaScript object by declaring methods in overlay objects
declared via abstract classes or via Java 8 default methods.
These methods, however, have additional restriction: they should not override methods of a parent class or interface.
*Please, note that the current version of TeaVM does not validate this, so you violate this restriction on your
risk*. Future versions of TeaVM will check this rule properly.

Example:

```java
public abstract class HTMLElement implements JSObject {
    @JSProperty
    public abstract CSSStyleDeclaration getStyle();

    public void hide() {
        getStyle().setProperty("display", "none");
    }

    public void show() {
        getStyle().removeProperty("display");
    }
}
```

Or the same in Java 8:

```java
public interface HTMLElement extends JSObject {
    @JSProperty
    CSSStyleDeclaration getStyle();

    default void hide() {
        getStyle().setProperty("display", "none");
    }

    default void show() {
        getStyle().removeProperty("display");
    }
}
```

Also, you can declare `native` methods in abstract classes. Rewrite our example this way:

```java
public abstract class HTMLElement implements JSObject {
    @JSBody(script = "this.style.display = 'none';")
    public native void hide();

    @JSBody(script = "this.style.display = '';")
    public native void show();
}
```


Static methods
--------------

Overlay classes can declare static methods, either with Java or JavaScript implementation.
Example:

```java
public abstract class JSArray<T extends JSObject> implements JSObject {
    @JSBody(params = { "size" }, script = "return new Array(size);")
    public static <S extends JSObject> native JSArray<S> create(int size);

    public static <S extends JSObject> JSArray<S> create(
            Collection<S> elements) {
        JSArray<S> array = create(elements.size());
        for (int i = 0; i < elements.size(); ++i) {
            array.set(i, elements.get(i));
        }
        return array;
    }
}
```


Wrapping indexers
-----------------

To access JavaScript objects as arrays or maps, you can declare indexer methods.
Indexer methods are either get indexers or set indexers.

* getter indexers take one parameter and return a non-void value;
* setter indexers take two parameters; first is index, second is value to set;

You are free to name your indexers as your want.

To tell TeaVM that method is either get or set indexer, you should annotate it with
[@JSIndexer](/javadoc/0.5.x/jso/core/org/teavm/jso/JSIndexer.html).

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


Passing Java objects to JavaScript
----------------------------------

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
final Window window = Window.current();
Element element = window.getDocument().getElementById("my-elem");
element.addListener("click", new EventListener() {
    @Override
    public void handleEvent(Event evt) {
        window.alert(evt);
    }
});
```


Passing Java objects as JavaScript functions
--------------------------------------------

Often, JavaScript APIs expect you to pass a callback function.
This case is similar to passing as JavaScript objects, however you need to tell TeaVM to pass your
Java classes as JavaScript functions.
To do this, simply add the [@JSFunctor](/javadoc/0.5.x/jso/core/org/teavm/jso/JSFunctor.html) annotation.
Functor interfaces must contain exactly one method.

For example,

```java
@JSFunctor
public interface TimerHandler extends JSObject {
    void onTimer();
}

@JSBody(params = { "handler", "delay" }, script = "setTimeout(handler, delay);")
static void setTimeout(TimerHandler handler, int delay);

static void doWork() {
    HTMLDocument doc = HTMLDocument.current();
    setTimeout(() -> doc.getBody().appendChild(doc.createTextNode("-")), 1000);
}
```


Conversion rules
----------------

JavaScript wrapper methods are limited to take and return the following *supported* types:

* `boolean`, `byte`, `short`, `int`, `float`, `double` which correspond to JavaScript numeric values.
* `java.lang.String` which corresponds to JavaScript `String` object.
* `T[]`, where T is a *supported* type, which corresponds to JavaScript array.
* interface or class that implements [JSObject](/javadoc/0.5.x/jso/core/org/teavm/jso/JSObject.html).

Notice that TeaVM won't check types before passing, so you need to design your wrappers carefully,
so that no type violations occur in runtime.
If you really require some type checking or casting, please implement them in JavaScript.


Dynamic type casting
--------------------

TeaVM does not support `instanceof` operator with `JSObject`.
Please, use another mechanisms to determine actual type of your JavaScript wrappers.
For example, to get actual type of [Node](/javadoc/0.5.x/jso/apis/org/teavm/jso/dom/xml/Node.html),
use [getNodeType()](/javadoc/0.5.x/jso/apis/org/teavm/jso/dom/xml/Node.html#getNodeType%28%29) method
instead of `instanceof`.