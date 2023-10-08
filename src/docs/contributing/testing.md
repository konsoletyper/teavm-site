Most tests are placed under `org.teavm.classlib` package, in a subpackage named after corresponding Java package.
To launch these tests, you should do extra setup.


# Setting up for C backend

C backend test runner uses gcc by default. If it's not an option for you, edit (or create, if it does not exist)
file `$HOME/.gradle/gradle.properties` and put following:

```
teavm.tests.c.compiler=/path/to/shell-file
```

shell file should be bash or Windows bat file, or whatever used in your OS, that runs compilation.
TeaVM test runner should run compilation of `all.c` file in current working directory
(see `tests/compile-c-unix-fast.sh` as example).

You can also turn off C tests by defining following property:

```
teavm.tests.c=false
```

of course, you don't necessary need to define properties in `$HOME/.gradle/gradle.properties`.
You can also pass them via `-D` command line option, please refer to Gradle documentation for details.


## Setting up for WASI backend

To run WASI tests, TeaVM uses [wasmtime](https://wasmtime.dev/) by default.
You can use another WASI runtime by defining `teavm.junit.wasi.runner`, which points to script.
This script takes two parameters: first is path to wasm file, and second is the command line argument
that TeaVM passes to tests (needed for internal purposes).
See `tests/run-wasi.sh` as example.

You can also turn off WASI tests by setting `teavm.tests.wasi` property to `false`.


## Other useful properties

* `teavm.junit.optimized` &ndash; controls whether optimized versions of tests run.
* `teavm.tests.js` &ndash; controls whether tests run in JS backend.
* `teavm.junit.js.decodeStack` &ndash; controls whether JS stacks are parsed into Java stack traces. 
  This improves debugging experience, but decreases test performance.
* `teavm.junit.wasm` &ndash; controls whether tests run in WebAssembly backend.


## Running tests manually

TeaVM runs class library tests in JVM as well as creates output files in 
`tests/build/teavm-tests/<backend-name>/path/to/Class` folder.
Depending on backend, it can be either JS, wasm or set of C files. You can run or compile these manually.
For WebAssembly and JS TeaVM also produces html files which you can open to run tests.
Note that WebAssembly by design does not work with local file system, so you need to serve 
files via HTTP (e.g. by running `python -m http.server 8080` from corresponding folder).