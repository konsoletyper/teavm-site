Due to number of reasons TeaVM does not rely on Java class library of host JVM.
Instead, TeaVM comes with its own implementation of subset of Java class library.
Of course, it's a subset of Java class library.
The list of supported classes is available [here](/jcl-report/recent/jcl.html).

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

or in Gradle:

```kotlin
teavm {
   all {
      properties.put("java.util.TimeZone.autodetect", "true")
   }
}
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

or in Gradle:

```kotlin
teavm {
   all {
      properties.put("java.util.Locale.available", "en_EN, en_US, ru_RU")
   }
}
```

