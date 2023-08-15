JavaScript code, generated with TeaVM, can be tested in the browser by using JUnit.
Currently only JUnit 4 supported
(unfortunately, JUnit 5 does not provide necessary extension points for purposes of TeaVM).
This requires some extra efforts.

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

Second, you should put `@RunWith(TeaVMTestRunner.class)` and `@WholeClassCompilation` annotations
on your test classes. 
The second annotation is not necessary, it only allows to improve compilation time.
If for some reason you want each test method to be compiled in a separate VM,
you may avoid this annotation.
Example:

```java
@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class SimpleTest {
    @Test
    public void simpleFields() {
        var x = 2;
        var y = 3;
        assertEquals(5, x + y);
    }
}
```

You can additionally put `@JvmSkip` annotation on class or on individual methods if you want
tests run only in JS (or other TeaVM target).

Finally, you need to specify additional system properties to JUnit runner.
For maven include following configuration:

```xml
<plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.1.2</version>

    <configuration>
      <systemProperties>
        <teavm.junit.target>\${project.build.directory}/js-tests</teavm.junit.target>
        <teavm.junit.js.runner>browser-chrome</teavm.junit.js.runner>
      </systemProperties>
    </configuration>
  </plugin>
</plugins>
```

For Gradle, use TeaVM DSL:

```groovy
teavm.tests.js {
    enabled = true
    runner = TeaVMWebTestRunner.CHROME
}
```


# Configuring runner using system properties

Here is the list of available system properties:

* `teavm.junit.target` &ndash; target directory where test files will be generated.
* `teavm.junit.js` &ndash; whether JS target is enabled (`true` or `false`).
  JS target is enabled by default.
* `teavm.junit.js.runner` &ndash; how to run JS tests.
  Available values: `htmlunit`, `browser`, `browser-chrome`, `browser-firefox`, `none`.
  None means that only JavaScript files will be generated without attempt to run them.
  `browser` value will print link to stdout, that you should open in a browser.
* `teavm.junit.js.decodeStack` &ndash; controls stack trace deobfuscation (`true` or `false`).
  Can slow down test runner.
* `teavm.junit.wasm` &ndash; whether WebAssembly target is enabled (`true` or `false`).
* `teavm.junit.wasm.runner` &ndash; how to run WebAssembly tests.
  Same as `teavm.junit.js.runner`, except for `htmlunit` value is not supported.
* `teavm.junit.c` &ndash whether C target is enabled (`true` or `false`).
* `teavm.junit.c.compiler` &ndash; command that compiles C to native code.
  This is usually path to a *shell* file that takes `all.c` file in working directory 
  and produces `run_tests` binary from it.
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

* `teavm.junit.wasi` &ndash; whether WebAssembly (WASI) target is enabled (`true` or `false`).
* `teavm.junit.wasi.runner` &ndash; command that executes WebAssembly module in WASI environment.
  This is usually a *shell* file that takes two arguments: path to `*.wasm` file and
  one string command line argument to this file. For example:
  
  ```bash
  ~/.wasmtime/bin/wasmtime run --mapdir /::target/wasi-testdir $1 $2
  ```


#  Configuring runner using Gradle DSL

`teavm.tests.js` and `test.tests.wasm` objects have the following properties:

* `enabled: Boolean` &ndash; enable corresponding target.
* `runner: TeaVMWebTestRunner` &ndash; use corresponding runner.

Also, `teavm.tests.js` provides `decodeStack` boolean property to enable stack trace deobfuscation.