There are two Gradle plugins: one for TeaVM library, another for TeaVM application.
TeaVM library plugin only helps with adding JavaScript interop dependencies to your project and with
testing your Java code in the browser.
TeaVM application plugin adds tasks to build your application for various targets.

To add TeaVM support to your project, add following code to `build.gradle`

```groovy
plugins {
  id "org.teavm" version "${teavm_version}"
}
repositories {
  mavenCentral()
}
```

for application project or

```groovy
plugins {
  id "org.teavm.library" version "${teavm_version}"
}
repositories {
  mavenCentral()
}
```

for library project.


## Application DSL

TeaVM application plugin adds DSL that allows to configure compilation for different targets.

```groovy
teavm {
  all {
    // common configuration for all targets
  }
  js {
    // configuration for JavaScript target
  }
  wasmGC {
    // configuration for WebAssembly GC target
  }
  c {
    // configuration for C (native) target
  }
  tests {
    js {
      // configuration for JavaScript tests
    }
    wasmGC {
      // configuration for WebAssembly GC tests
    }
  }
}
```

You can also use shortened syntax, for example:

```groovy
teavm.js {
    // configuration for JS target only
}
```

Note that library plugin only supports `tests` section.


### Common configuration properties

* `mainClass: String` &ndash; application entry point (Java main class that contains `main(String[])` method).
  This property is required.
* `debugInformation: Boolean` &ndash; instructs to produce debugging information.
  This is TeaVM proprietary format for JS and WebAssembly. Default value is `false`.
* `fastGlobalAnalysis: Boolean` &ndash; improve compilation time by reducing costs of global analysis,
  required for interprocedural optimizations like devirtualization, class initialization elimination,
  class cast elimination, etc. Can be used for debugging purposes. Default value is `false`.
* `optimization: OptimizationLevel` &ndash; which optimization level to use during compilation.
  Available values: `NONE`, `BALANCED`, `AGGRESSIVE`.
  Note that `AGGRESSIVE` level does not give significant performance growth for JS target,
  so `BALANCED` is preferred, since with it compiler produces less code.
  `NONE` value produces code that is friendly to debuggers.
  Default value is `BALANCED` for JS and `AGGRESSIVE` for other targets.
* `properties: Map<String, String>` &ndash; specifies properties that can fine-tune TeaVM libraries.
  These are usually library specific properties, please refer to library documentation.
  Some of the properties allow to fine-tune [Java class library emulation](/docs/runtime/java-classes.html).
* `preservedClasses: List<String>` &ndash; lists classes which should be preserved by TeaVM during compilation.
  TeaVM performs global analysis to determine which classes are necessary for execution and includes
  only these classes into final binary.
  However, in some cases you may need to manually preserve some classes.
  For example, if these classes export some methods for native target, not observable by compiler.
* `outOfProcess: Boolean` &ndash; tells whether TeaVM should be executed in a separate process.
  `false` by default.
* `processMemory: Int` &ndash; in case TeaVM is executed in a separate process, how many memory, in megabytes,
  should be given to this process.
* `outputDir: Directory` &ndash; output directory. Default value is `$buildDir/generated/teavm`.


### Additional configuration properties

* `obfuscated: Boolean` (JS, WasmGC, C) &ndash; turns on obfuscation that produces less code and makes this code
  unreadable. Recommended for production. If you are developing open-source project and want others to see
  the code, you should share original Java/Kotlin/Scala code and publish source maps,
  instead of turning off obfuscation.
  For C target, removes metadata about call sites and source class/method names,
  which results in obfuscated stack traces.
  Default value is `true`.
* `strict: Boolean` (JS, WasmGC) &ndash; add into generated code more checks (like null checks, array range checks,
  etc.). Most software should not depend on the code that catches NPE, IOOBE, etc. and does something beyond just
  reporting it. If it's the case, you should turn on strict mode, which affects negatively generated code size
  and performance. Default value is `false` for JS and `true` for WasmGC.
* `sourceMap: Boolean` (JS, WasmGC) &ndash; produce source maps. Default value is `false`.
* `entryPointName: String` (JS) &ndash; name of JavaScript function that starts the main method.
  Note that it does not affect the name or the signature of a main method in Java sources (which is always `main`).
  Default value is `main`.
* `relativePathInOutputDir` (JS, WasmGC) &ndash; directory, relative to `outputDir`, where generated files
  will be written. Default value is `js` for JS backend and `wasm-gc` for WebAssembly GC backend.
* `targetFileName: String` (JS, WasmGC) &ndash; name of target file. Default value is
  `${projectName}.js` or `${projectName}.wasm` respectively.
* `addedToWebApp: Boolean` (JS, WasmGC) &ndash; used in conjunction with `war` plugin.
  Adds corresponding TeaVM task as a dependency to WAR tasks and includes TeaVM output into generated `.war` file.
  Default value is `false`.
* `maxTopLevelNames: Int` (JS) &ndash; how many names to generate at top-level. All other declarations
  are generated as properties of some additional object. The reason to limit the number of top-level declarations
  is the bug in Chromium-based browsers that throw stack overflow error.
* `sourceFilePolicy: SourceFilePolicy` (JS, WasmGC) &ndash; declares how to produce paths to source files
  when source maps are generated. Possible values:
  * `DO_NOTHING` &ndash; provide path to source files as is without any resolution. In this case the developer
    must ensure themselves that source files are served together with generated file.
  * `COPY` &ndash; copies sources to the output directory.
  * `LINK_LOCAL_FILES` &ndash; when possible, generate `file://` URLs with paths in local file system.
* `moduleType: JSModuleType` (JS) &ndash; which type of JavaScript module to use:
  * `COMMON_JS` &ndash; CommonJS (compatible with node.js);
  * `UMD` &ndash; UMD (automatically detect, at run time, AMD or CommonJS module system; behave as IIF otherwise);
  * `NONE` &ndash; no module system, all code placed in immediately-invoked function (IIF);
  * `ES2015` &ndash; [ES2015 module](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules).
* `minHeapSize: Int` (C) &ndash; minimal (initial) heap size, in megabytes. Default value is `1`.
* `maxHeapSize: Int` (C) &ndash; maximal heap size, in megabytes. Default value is `16`.
* `heapDump: Boolean` (C) &ndash; include into generated virtual machine metadata that allows
  the VM to generate heap dump on irrecoverable crash. Default value is `false`.
* `shortFileNames: Boolean` (C) &ndash; generate shorter file names. Used to work around the buggy
  Microsoft C++ compiler. Default value is `false`.


### WebAssembly GC-specific properties

* `copyRuntime: Boolean` &ndash; whether to copy the Wasm GC runtime JavaScript file alongside the
  `.wasm` output. The runtime is required to load the module. Default value is `true`.
* `modularRuntime: Boolean` &ndash; generate runtime as an ES6 module. Default value is `false`.
* `disassembly: Boolean` &ndash; generate a human-readable WAST disassembly in an HTML file,
  useful for debugging. Default value is `false`.
* `debugInfoLocation: WasmDebugInfoLocation` &ndash; where to store debug information:
  * `EMBEDDED` &ndash; embed debug info as a custom section inside the `.wasm` file;
  * `EXTERNAL` &ndash; write a separate `.teadbg` file alongside the `.wasm` file (default).
* `debugInfoLevel: WasmDebugInfoLevel` &ndash; how much debug information to generate:
  * `DEOBFUSCATION` &ndash; only enough to deobfuscate stack traces (default);
  * `FULL` &ndash; complete debug information.
* `minDirectBuffersSize: Int` &ndash; minimum linear memory size in megabytes, used for NIO direct buffers
  and JS/Wasm data transfer. Default value is `2`.


### Emscripten interop (WasmGC)

The WasmGC target supports linking native C/C++ code compiled with Emscripten. Configure it via the nested
`emscripten {}` block inside `wasmGC {}`:

```groovy
teavm.wasmGC {
    emscripten {
        enabled = true
        exportedFunctions.add("_myFunction")
        compilerArgs.add("-O2")
    }
}
```

See [Emscripten integration guide](/docs/wasm-gc-backend/emscripten.html) for a step-by-step guide and
[Loader API](/docs/wasm-gc-backend/loader.html) for the JavaScript API to load the compiled module.


## Including dependencies

TeaVM produces a number of additional libraries.
To shorten access to these libraries, you can use the TeaVM DSL object instead of providing full addresses.
These shortcuts are available in the TeaVM DSL object.

* `teavm.libs.jso` &ndash; JSO library that defines primitives for interacting with native JavaScript.
* `teavm.libs.jsoApis` &ndash; library that, based on JSO primitives, declares stubs for interacting with commonly
  used browser APIs.
* `teavm.libs.interop` &ndash; library that defines primitives for interacting with external WebAssembly modules
  and with native libraries (in case of C target).
* `teavm.libs.metaprogramming` &ndash; library that simplifies compile-time code generation,
  that can be used instead of reflection.

Example:

```groovy
dependencies {
    implementation teavm.libs.jso
}
```


## Properties

TeaVM plugin resolves its properties from three sources, in order of decreasing priority:

1. **JVM system properties** — prefix the property name with `teavm.`, e.g.
   `-Dteavm.emscripten-location=/path/to/emscripten`.
2. **TeaVM property files** — `teavm.properties` and `teavm-local.properties` in the project root,
   where definitions in `teavm-local.properties` override those in `teavm.properties`.
   `teavm-local.properties` should be added to VCS ignore so each developer can supply their own values
   without affecting the shared repository state.
3. **Gradle properties** — prefix the property name with `teavm.` in any `gradle.properties` file,
   including the user-level `~/.gradle/gradle.properties`. This is useful for machine-wide settings that
   apply to all projects, such as the Emscripten SDK location.

Plugin also allows you to define configuration by redefining properties.
Following list defines these properties:

* `js.obfuscated`
* `js.sourceMap`
* `js.strict`
* `js.addedToWebApp`
* `js.optimization`
* `js.moduleType`
* `js.sourceFilePolicy`
* `js.devServer.stackDeobfuscated`
* `js.devServer.indicator`
* `js.devServer.autoReload`
* `js.devServer.port`
* `js.devServer.proxy.url`
* `js.devServer.proxy.path`
* `js.devServer.memory`
* `wasm-gc.optimization`
* `wasm-gc.addedToWebApp`
* `wasm-gc.strict`
* `wasm-gc.obfuscated`
* `wasm-gc.copyRuntime`
* `wasm-gc.modularRuntime`
* `wasm-gc.disassembly`
* `wasm-gc.sourceMap`
* `wasm-gc.sourceFilePolicy`
* `wasm-gc.debugInformation`
* `wasm-gc.debugInformation.location`
* `wasm-gc.debugInformation.level`
* `wasm-gc.minDirectBuffersSize`
* `wasm-gc.maxDirectBuffersSize`
* `wasm-gc.emscripten.compilerArgs`
* `emscripten-location`
* `c.heapDump`
* `c.shortFileNames`
* `c.optimization`
* `debugInformation`
* `fastGlobalAnalysis`
* `outOfProcess`
* `processMemory`

You can also read values of properties via the TeaVM DSL object using following notation:
`teavm.property('propertyName')`.


# Tasks

TeaVM plugin adds a number of tasks that you can invoke directly or add to dependencies of a lifecycle task.
Here is a summary:

**JavaScript**

* `generateJavaScript` &ndash; compiles Java/Kotlin/Scala code to JavaScript.
* `javaScriptDevServer` &ndash; starts a long-running development server that caches compiler state and serves
  the compiled JS file over HTTP with hot-reload support.
* `stopJavaScriptDevServer` &ndash; stops the development server.

**WebAssembly GC**

* `generateWasmGC` &ndash; compiles code to WebAssembly GC format.
* `copyWasmGCRuntime` &ndash; copies the runtime JavaScript file (and optionally the deobfuscator) next to the
  `.wasm` output. The runtime is required to load the module in a browser or Node.js.
* `disasmWasmGC` &ndash; generates a human-readable WAST disassembly as an HTML file (requires
  `disassembly = true`).
* `emscriptenStubWasmGC` &ndash; generates a C stub header (`teavm-imports.c`) that wires Java native method
  declarations to the Emscripten-compiled C/C++ code.
* `emscriptenWasmGC` &ndash; compiles the C/C++ sources from `src/teavm/emcc/` with `emcc` and produces the
  native `.js` + `.wasm` pair.
* `buildWasmGC` &ndash; convenience task that runs `generateWasmGC`, `copyWasmGCRuntime`, `disasmWasmGC`, and
  the Emscripten tasks in the correct order.

**C / native**

* `generateC` &ndash; compiles code to C source files for further compilation with a native C/C++ compiler.

You may want to include tasks in dependencies of other lifecycle tasks, for example:

```groovy
tasks.assemble.dependsOn(tasks.buildWasmGC)
```


# Additional source set and configuration

In case you need to write a web application with TeaVM client code,
you may want to avoid presence of TeaVM-specific client libraries and class files in the `war` file.
For this purpose you can put your client code under the `teavm` source set, i.e.
in `src/teavm/java` (or `src/teavm/kotlin`, `src/teavm/scala` respectively)
instead of the usual `src/main/java`.
You can also put TeaVM-specific libraries into the `teavm` configuration instead of `implementation`,
for example:

```groovy
dependencies {
  teavm(teavm.libs.jsoApis)
}
```

C/C++ source files for Emscripten interop go in `src/teavm/emcc/` (files with extensions
`.c`, `.cpp`, `.C`, `.cc`, `.cxx`, `.c++`).


# JavaScript development server

In addition to normal builds, the development server can be used to improve developer experience.
Development server is a separate process that keeps running between builds and serves files via HTTP.
This allows the development server to cache compiler structures between rebuilds and thus speed up
subsequent builds. Also, development server injects some additional metadata that allow deobfuscating
stack traces on-the-fly. Finally, development server serves source maps together with source files,
which can improve the debugging experience.

To start development server, configure it using following DSL:

```groovy
teavm {
  js {
    devServer {
      port = port_number
      // .. other values
    }
  }
}
```

or shorter

```groovy
teavm.js.devServer {
  // configuration properties here
}
```

and run `javaScriptDevServer`. The generated file will be served from
`http://localhost:${port}/${relativePathInOutDir}/${targetFileName}.js`, where placeholders correspond to
configuration properties. Subsequent runs of the same task will just instruct the server to re-build the
JavaScript file.

As you make changes to project, run `javaScriptDevServer` again. This won't restart the server.
Instead, it will inform the server that the `.class` files were changed and the server needs to pick them
and re-build the JavaScript file.

To stop the server process, run `stopJavaScriptDevServer` task.

Available configuration properties:

* `port` &ndash; port number the HTTP server listens on.
* `stackDeobfuscated` &ndash; whether all JS stacks should be deobfuscated and proper Java stack traces
  generated.
* `indicator` &ndash; injects a small indicator in the lower-left corner of the page to
  display compilation progress right in the web page.
* `autoReload` &ndash; indicates whether the page should be reloaded as soon as compilation completes.
* `processMemory` &ndash; amount of memory, in megabytes, to allocate for the server process.
* `proxyUrl` &ndash; when specified, development server will not only serve generated JavaScript,
  but also proxy all incoming requests to the given URL.
* `proxyPath` &ndash; used in conjunction with `proxyUrl`. When specified, only requests starting with
  the specified path will be proxied.
