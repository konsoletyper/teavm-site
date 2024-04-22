With TeaVM you can not only compile Java application to JavaScript, 
but also create JavaScript libraries with Java.
You are required to put additional annotations on your code 
and make some extra setup in the build system.
These topics are uncovered below.

*Note that this section describes functionality available in [preview builds](/docs/intro/preview-builds.html)*.


# Setting up module system

By default, TeaVM generates UMD wrapper, which is compatible both with AMD and CommonJS module systems,
or puts Java entry points into global namespace, if none available.
You can change this behaviour, e.g. to produce ES2015 modules.

If you are using Gradle, you need following configuration:

```groovy
teavm.js {
    moduleType = JSModuleType.ES2015
}
```

where available module types are following:

* `COMMON_JS` &ndash; CommonJS module, e.g. for compilation with Node.js.
* `UMD` &ndash; (default) UMD wrapper for compatibility with AMD, Node.js, if available.
* `NONE` &ndash; make TeaVM entry points available as global declarations (for example, to use in web).
* `ES2015` &ndash; produce ES2015 modules.

another way is to specify `teavm.js.moduleType` gradle property or `js.moduleType` TeaVM property
(please, refer to [Gradle documentation](/docs/tooling/gradle.html)).

In Maven you need following in plugin configuration:

```xml
<jsModuleType>ES2015</jsModuleType>
```


# Generating a module

By default, you use Java convention with public static `main` method of the main 
class specified in configuration.
A module will be generated that exports one method called `main`.
If you need to convert a library, don't follow this convention.
Instead, create static methods in your main class and annotate them with `@JSExport` annotation, 
as follows:

```java
public class MyModuleExample {
    @JSExport
    public static void foo() {
        System.out.println("foo called");
    }
    
    @JSExport
    public static String bar(int a, int b) {
        return "bar: " + (a + b); 
    }
}
```

TeaVM will produce module that exports `foo` and `bar` functions.

In case you set up ES2015 module type in TeaVM, you can use this module from JS side like follows:

```js
import { foo, bar } from './myModuleExample.js'
foo();
bar(2, 3);
```


# Exporting properties from module

To export properties from modules, use Java beans naming convention and put `@JSProperty` 
annotation on methods:

```java
public class ModuleWithExportedProperties {
    private static int bar = 23;
    
    @JSProperty
    public static String getFoo() {
        return "foo value";
    }
    
    @JSProperty
    public static int getBar() {
        return bar;
    }
    
    @JSProperty
    public static void setBar(int value) {
        bar = value;
    }
}
```

TeaVM will produce module that exports `foo` readonly property and `bar` read-write property.

Usage example:

```js
import * as java from './myModuleExample.js'
console.log(java.foo);
console.log(java.bar);
java.bar = 42;
console.log(java.bar);
```


# Returning non-primitive values from module

If you need a function that produces an object, you can do with either of two ways:

* Return a simple Java class that exports its declarations to JavaScript with `@JSExport`.
* Return a Java class that implements one of sub-interfaces of JSObject interface 
  (see [Interacting with JavaScript](https://teavm.org/docs/runtime/jso.html)).

For example:

```java
public class Point {
    private int x;
    private int y;

    @JSExport
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @JSExport
    @JSProperty
    public int getX() {
        return x;
    }

    @JSExport
    @JSProperty
    public int getY() {
        return y;
    }

    @JSExport
    public static Point getZero() {
        return new Point(0, 0);
    }
}

public interface Color extends JSObject {
    @JSProperty
    int getRed();
  
    @JSProperty
    int getGreen();
  
    @JSProperty
    int getBlue();
}

public class ColorImpl implements Color {
    private int red;
    private int green;
    private int blue;
  
    public ColorImpl(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }
}

public class MyModule {
    @JSExport
    public static Point createPoint(int x, int y) {
        return new Point(x, y);
    }
  
    @JSExport
    public static Color createColor(int red, int green, int blue) {
        return new ColorImpl(red, green, blue);
    }
}
```

There's some differences between these approaches.
The first approach exports entire class, so that you can consume it and then, for example,
use it in `instanceof` expression, or call its static methods.
The second approach returns just anonymous object with certain methods and properties.

From JS side:

```js
import { createPoint, createColor, Point } from 'myModule.js';
let pt = createPoint(2, 3);
console.log(pt.x, pt.y);
let color = createColor(255, 255, 0);
console.log(color.red, color.green, color.blue);
console.log(pt instanceof Point); // prints 'true'
console.log(color instanceof Point); // prints 'false'
```

Note that if you want to specify exported class name explicitly, you can use `@JSClass` annotation:

```java
// This class will be seen as 'Bar' in JS 
@JSExport("Bar")
public class Foo {
}
```


# Taking non-primitive parameters

To take non-primitive parameters to exported methods, you can use the same two approaches,
that you use to return values, i.e.:

```java
public class MyModule {
    @JSExport
    public static Point createPoint(int x, int y) {
        return new Point(x, y);
    }
    
    @JSExport
    public static double length(Point pt) {
        return Math.sqrt(pt.getX() * pt.getX() + pt.getY() * pt.getY());
    }
  
    @JSExport
    public static void logColor(Color color) {
        System.out.println("rgb: " + color.getRed() + ", " + color.getGreen() 
                + ", " + color.getBlue());
    }
}
```

The difference is however, a bit more sensitive.
In the first case you should pass *exactly* instance of `Point` class that you got from Java module.
In the second case you are only required to pass object with corresponding properties.

For example, this is the right usage of `length` method:

```js
import { createPoint, length } from './myModule.js';
let pt = createPoint(2, 3);
console.log(length(pt));
```

And this is invalid:

```js
import { length } from './myModule.js';
console.log(length({ x: 2, y: 3 }));
```

However, this is valid:

```js
import { logColor } from './myModule.js';
logColor({ red: 255, green: 255, blue: 255 });
```


# Exporting classes explicitly

When a Java class mentioned explicitly somewhere in signature of module methods, 
they are exported automatically.
However, sometimes you need to export some class that does not present in signatures of other methods.
In this scenario you just put `@JSExportClasses` annotation to the module entry point class,
or alternatively to other exported classes.
The behaviour is following: as soon as a class with `@JSExportClasses` annotation is exported to JavaScript
for some reason, all classes enumerated in the annotation will also be exported. For example:

```java
@JSExportedClasses({ Point.class })
public class MyModule {
    // This class is empty, it does not export any methods to JavaScript
    // However, Point class is exported
}
```