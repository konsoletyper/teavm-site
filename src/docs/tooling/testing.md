Code, generated with TeaVM, can be tested by using JUnit.
TeaVM compiles each test class to JavaScript, WebAssembly GC or C, runs the result in a browser
(or as a native application) and reports the result back to JUnit.
Both JUnit 5 (Jupiter API, also works with JUnit 6) and JUnit 4 are supported.

First, you need to include library that extends JUnit.
Gradle plugins do it automatically, for Maven you should add following dependency:

```xml
<dependency>
  <groupId>org.teavm</groupId>
  <artifactId>teavm-junit</artifactId>
  <version>${teavm_version}</version>
  <scope>test</scope>
</dependency>
```

Note that TeaVM does not add JUnit itself, you should add it as you usually do.


# JUnit 5

Add JUnit Jupiter to your project. In Gradle:

```kotlin
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

In Maven, add `org.junit.jupiter:junit-jupiter` dependency with `test` scope.
Recent versions of Surefire plugin detect JUnit Platform automatically.

Then put `@TeaVMTest` annotation on a test class:

```java
@TeaVMTest
public class SimpleTest {
    @Test
    public void simpleFields() {
        var x = 2;
        var y = 3;
        assertEquals(5, x + y);
    }
}
```

TeaVM supports the following subset of JUnit 5:

* `@Test` methods;
* `@ParameterizedTest` methods (see below);
* `@BeforeEach` and `@AfterEach` methods;
* `@Disabled` annotation;
* assertions from `org.junit.jupiter.api.Assertions`.

Other features, like `@BeforeAll`/`@AfterAll`, `@Nested` classes, dynamic tests, `@RepeatedTest`
and custom extensions (`@ExtendWith`), have no ahead-of-time compiled equivalent and are not supported
by TeaVM.

By default, all test methods of a class are compiled together, which is much faster than
compiling each method separately.


## Parameterized tests

Arguments of parameterized tests are computed inside the compiled code, so TeaVM supports only argument
sources that can be analyzed at compile time:

* `@ValueSource`;
* `@CsvSource`;
* `@EnumSource`;
* `@NullSource`, `@EmptySource` and `@NullAndEmptySource`;
* `@MethodSource` and `@FieldSource`, referring to static members; factory methods must have no parameters.

Arguments are converted to parameter types according to implicit conversion rules of JUnit.
Explicit converters (`@ConvertWith`), argument aggregators, `@CsvFileSource` and custom `@ArgumentsSource`
providers are not supported, TeaVM reports an error when it encounters them.

Example:

```java
@TeaVMTest
public class ParameterizedExampleTest {
    @ParameterizedTest
    @CsvSource({ "1, 2, 3", "2, 3, 5", "-1, 1, 0" })
    public void sum(int a, int b, int expected) {
        assertEquals(expected, a + b);
    }

    @ParameterizedTest
    @MethodSource("strings")
    public void notBlank(String s) {
        assertFalse(s.isBlank());
    }

    static Stream<String> strings() {
        return Stream.of("foo", "bar");
    }
}
```


# JUnit 4

Put `@RunWith(TeaVMTestRunner.class)` annotation on a test class:

```java
@RunWith(TeaVMTestRunner.class)
public class SimpleTest {
    @Test
    public void simpleFields() {
        var x = 2;
        var y = 3;
        assertEquals(5, x + y);
    }
}
```

`@Before` and `@After` methods are supported.


# Additional annotations

Following annotations from `org.teavm.junit` package work with both JUnit 5 and JUnit 4:

* `@SkipJVM` (on class or method) &ndash; don't run test in JVM, only in TeaVM targets.
* `@SkipPlatform` (on class or method) &ndash; don't run test on listed platforms.
* `@OnlyPlatform` (on class or method) &ndash; run test only on listed platforms.
* `@EachTestCompiledSeparately` (on class) &ndash; compile each test method separately.
  This can be necessary if you are testing custom TeaVM plugins or using metaprogramming API.
* `@TeaVMProperties` (on class) &ndash; pass properties to the compiler.
* `@AsyncEnabled` (on class or method) &ndash; enable support for coroutines.
  Tests that use threads or other blocking APIs, like `Thread.sleep` or `Object.wait`,
  must be annotated with it, otherwise compilation fails.
  See [Coroutines](/docs/runtime/coroutines.html) for details.


# Configuring runner

## Maven

For Maven, pass system properties to the test runner:

```xml
<plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>

    <configuration>
      <systemPropertyVariables>
        <teavm.junit.target>\${project.build.directory}/js-tests</teavm.junit.target>
        <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
      </systemPropertyVariables>
    </configuration>
  </plugin>
</plugins>
```

## Gradle

For Gradle, use TeaVM DSL to configure JavaScript tests:

```groovy
teavm.tests.js {
    enabled = true
    runner = TeaVMWebTestRunner.CHROME
}
```

`teavm.tests.js` has the following properties:

* `enabled: Boolean` &ndash; enable JavaScript target.
* `runner: TeaVMWebTestRunner` &ndash; how to run tests: `CHROME`, `FIREFOX`, `CUSTOM_BROWSER`
  (print a link to stdout, that you should open in a browser) or `NONE` (only generate files).
* `decodeStack: Boolean` &ndash; enable stack trace deobfuscation.

For other targets, pass system properties (see below) to the `test` task:

```kotlin
tasks.test {
    systemProperty("teavm.junit.wasm-gc", "true")
    systemProperty("teavm.junit.wasm-gc.runner", "browser-chrome")
}
```


## System properties

Here is the list of available system properties:

* `teavm.junit.target` &ndash; target directory where test files will be generated.
* `teavm.junit.optimized` &ndash; additionally compile and run each test with optimizations enabled
  (`true` or `false`).
* `teavm.junit.js` &ndash; whether JS target is enabled (`true` or `false`).
  JS target is enabled by default.
* `teavm.junit.js.runner` &ndash; how to run JS tests.
  Available values: `browser`, `browser-chrome`, `browser-firefox`, `none`.
  None means that only JavaScript files will be generated without attempt to run them.
  `browser` value will print link to stdout, that you should open in a browser.
* `teavm.junit.js.decodeStack` &ndash; controls stack trace deobfuscation (`true` or `false`).
  Can slow down test runner.
* `teavm.junit.minified` &ndash; additionally compile and run each JS test with obfuscation enabled
  (`true` or `false`).
* `teavm.junit.wasm-gc` &ndash; whether WebAssembly target is enabled (`true` or `false`).
* `teavm.junit.wasm-gc.runner` &ndash; how to run WebAssembly tests.
  Same values as for `teavm.junit.js.runner`.
* `teavm.junit.wasm-gc.disasm` &ndash; generate disassembly of each WebAssembly test module
  as an HTML file (`true` or `false`).
* `teavm.junit.c` &ndash; whether C target is enabled (`true` or `false`).
* `teavm.junit.c.compiler` &ndash; command that compiles C to native code.
  This is usually path to a *shell* file that takes `all.c` file in working directory
  and produces `run_test` binary from it.
  For example:

  ```bash
  export LC_ALL=C
  SOURCE_DIR=$(pwd)
  gcc -g -O0 -lrt -lm all.c -o run_test
  ```
  
  Another way to write such runner is to run cmake in working directory:

  ```bash
  SOURCE_DIR=$(pwd)
  BUILD_DIR=$SOURCE_DIR/build
  mkdir -p $BUILD_DIR
  pushd $BUILD_DIR >/dev/null && \
  cmake -S $SOURCE_DIR -B . >/dev/null && \
  make --quiet >/dev/null && \
  popd >/dev/null && \
  rm -rf $BUILD_DIR
  ```

* `teavm.junit.c.envScript` &ndash; script that sets up environment variables for C compiler
  (for example, MSVC developer environment). It runs once, and its environment is reused for all
  compiler invocations.
* `teavm.junit.c.runWrapper` &ndash; command that is used to run compiled test binaries.
* `teavm.junit.c.lineNumbers` &ndash; generate line numbers in C code (`true` or `false`).
* `teavm.junit.c.shortFileNames` &ndash; generate shorter file names (`true` or `false`).
  Used to work around the buggy Microsoft C++ compiler.
