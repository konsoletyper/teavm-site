---
title: Tuning Java class library
---

Due to number of reasons TeaVM does not rely on Java class library of host JVM.
Instead, TeaVM comes with its own implementation of subset of Java class library.
Java class library was not designed for efficiency with AOT compiler producing code for a limited
environment like JavaScript, that's why only a limited subset is available.
Some of the classes in TeaVM don't behave 100% similar to JVM, for the sake of efficiency.
However, you can tune their behaviour to be closer to normal JVM.
This section provides description of such cases.


# Timezone detection

JavaScript does not provide API to get current timezone.
Only timezone offset is available, which is not enough.
TeaVM has heuristic algorithm that tries to detect timezone using offset history.
However, this algorithm may slow down boot time, so it's disable by default.
To enable it, you should set `java.util.TimeZone.autodetect` TeaVM compiler property to `true`.
If you use Maven, the configuration would look like this:

```xml
<plugin>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-maven-plugin</artifactId>
  <executions>
    <execution>
      ...
      <configuration>
        ...
        <properties>
          <java.util.TimeZone.autodetect>true</java.util.TimeZone.autodetect>
        </properties>
      </configuration>
    </execution>
  </executions>
</plugin>
```


# Locales

JavaScript does not provide full access to locales.
To emulate Java APIs for locales, TeaVM embeds [Unicode CLDR](http://cldr.unicode.org/) data into JavaScript.
However, this data is huge, so for the sake of efficiency the only locale embedded into JavaScript is en_EN.
To include more locales, specify them as a comma-separated string to `java.util.Locale.available` property.
For example, in Maven configuration:

```xml
  <plugin>
    <groupId>org.teavm</groupId>
    <artifactId>teavm-maven-plugin</artifactId>
    <executions>
      <execution>
        ...
        <configuration>
          ...
          <properties>
            <java.util.Locale.available>en_EN, en_US, ru_RU</java.util.Locale.available>
          </properties>
        </configuration>
      </execution>
    </executions>
  </plugin>
```


# Reflection

Reflection API is not friendly to ahead-of-time compilers like TeaVM.
That's why TeaVM provides only limited support for reflection.
By default, you can access metadata of classes and methods, 
but any attempt to call methods from reflection will produce `SecurityException`.
To enable reflective access to certain methods, you should follow these steps:

1. Implement `ReflectionSupplier` interface
2. Create a resource file named `META-INF/services/org.teavm.classlib.ReflectionSupplier`
   with a single line containing fully-qualified name of your implementation

Example:

```java
package org.teavm.classlib.support;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;

public class ReflectionSupplierImpl implements ReflectionSupplier {
    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        Set<String> fields = new HashSet<>();
        for (FieldReader field : cls.getFields()) {
            if (field.getAnnotations().get(Reflectable.class.getName()) != null) {
                fields.add(field.getName());
            }
        }
        return fields;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        Set<MethodDescriptor> methods = new HashSet<>();
        for (MethodReader method : cls.getMethods()) {
            if (method.getAnnotations().get(Reflectable.class.getName()) != null) {
                methods.add(method.getDescriptor());
            }
        }
        return methods;
    }
}
```

and corresponding `META-INF/services/org.teavm.classlib.ReflectionSupplier`:

```
org.teavm.classlib.support.ReflectionSupplierImpl
```

Note that syntax for `MethodDescriptor` constructor is following.
Consider you have method `String foo(int bar, Object baz)`.
Corresponding `MethodDescriptor` should look like: 

```java
new MethodDescriptor("foo", int.class, Object.class, String.class);
```

You can apply whatever rule you like, for example:

```java
     method.getAnnotations().get(MyReflectable.class.getName()) != null
     method.getName().startsWith("canReflect");
     method.getParameterTypes().length == 2 && method.getResultType() == ValueType.parse(void.class);
``` 

Please, not that you should keep the number of reflectable methods as small as possible!