Most tests are placed under `org.teavm.classlib` package, in a subpackage named after corresponding Java package.
Tests are run with `./gradlew :tests:test`.
Running all tests takes a lot of time, so during development you may want to run only some of them, e.g.:

```
./gradlew :tests:test --tests org.teavm.classlib.java.io.BufferedWriterTest
```


# Configuring tests

Tests are configured with `teavm.tests.*` properties.
Instead of passing them every time with `-P`, copy `tests/gradle-local-template.properties` to
`tests/gradle-local.properties` (ignored by git) and uncomment properties you need.
The template lists all supported properties.
`-P` options passed in command line take priority over `gradle-local.properties`.

Most useful properties:

* `teavm.tests.js`, `teavm.tests.wasm-gc`, `teavm.tests.c` &ndash; enable or disable tests for
  corresponding backend (`true` or `false`).
* `teavm.tests.optimized` &ndash; additionally compile and run each test with optimizations.
* `teavm.tests.minified` &ndash; additionally compile and run each JS test with obfuscation.
* `teavm.tests.browser` &ndash; browser to run JS and WebAssembly GC tests:
  `browser-chrome` (default), `browser-firefox`, `browser` (print a link to open manually),
  `none` (only generate files).
* `teavm.tests.decodeStack` &ndash; controls whether JS stacks are parsed into Java stack traces.
  This improves debugging experience, but decreases test performance.
* `teavm.tests.wasm-gc.disasm` &ndash; generate WebAssembly GC disassembly (see below).


# Setting up for C backend

C backend test runner uses gcc by default. If it's not an option for you, set following property:

```
teavm.tests.c.compiler=/path/to/shell-file
```

shell file should be bash or Windows bat file, or whatever used in your OS, that runs compilation.
TeaVM test runner should run compilation of `all.c` file in current working directory
(see `tests/compile-c-unix-fast.sh` as example).
See `tests/gradle-local-template.properties` for other C-related properties.

You can also turn off C tests by defining following property:

```
teavm.tests.c=false
```


# Running tests manually

TeaVM runs class library tests in JVM as well as creates output files in 
`tests/build/teavm-tests/<backend-name>/path/to/Class` folder.
Depending on backend, it can be either JS, wasm or set of C files. You can run or compile these manually.
For WebAssembly and JS TeaVM also produces html files which you can open to run tests.
Note that WebAssembly by design does not work with local file system, so you need to serve 
files via HTTP (e.g. by running `python -m http.server 8080` from corresponding folder).

When `teavm.tests.wasm-gc.disasm` is enabled, TeaVM also writes a human-readable disassembly of each
WebAssembly GC module (`*.wast.html` file) next to the module.
It is written even when the test fails, or when the produced module is broken,
which is useful for debugging code generation issues.
