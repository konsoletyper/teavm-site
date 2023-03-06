There are two Gradle plugins: one for TeaVM library, another for TeaVM application.
TeaVM library plugin only helps with adding JavaScript interop dependencies to you project and with
testing your Java code in the browser.
TeaVM application plugin adds tasks to build your application for various targets.

To add TeaVM support to your project, add following code to `build.gradle`

```groovy
plugins {
    id "org.teavm" version "${teavm_version}"
}
```

for application project or

```groovy
plugins {
    id "org.teavm.library" version "${teavm_version}"
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
        // configuration for JS target
    }
    wasm {
        // configuration for WebAssembly (browser) target
    }
    wasi {
        // configuration for WebAssembly (WASI) target
    }
    c {
        // configuration for C (native) target
    }
    tests {
        js {
            // configuration for JS tests
        }
        wasm {
            // configuration for WebAssembly (browser) tests
        }
    }
}
```

you can also use shortened syntax, for example:

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
  default value is `BALANCED` for JS and `AGGRESSIVE` for other targets.
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
* `outputDir: File` &ndash; output directory. Default value is `$buildDir/generated/teavm`.


### Additional configuration properties

* `obfuscated: Boolean` (JS, C) &ndash; turns on obfuscation that produces less code and makes this code unreadable.
  Recommended for production. If you are developing open-source project and want others to see
  the code, you should share original Java/Kotlin/Scala code and publish source maps, 
  instead of turning off obfuscation.
  For C target, removes metadata about call sites and source class/method names,
  which results in obfuscated stack traces (additional tables will be generated to deobfuscate these
  stack traces externally).
  Mostly usual for debugging purposes. Default value is `true`. 
* `strict: Boolean` (JS) &ndash; add into generated core more checks (like null checks, array range checks, etc.).
  Most software should not depend on the code that catches NPE, IOOBE, etc. and does something beyond just
  reporting it. If it's the case, you should turn on strict mode, which affects negatively generated code size
  and performance. Default value is `false`.
* `sourceMap: Boolean` (JS) &ndash; produce JavaScript source maps. Default value is `false`.
* `entryPointName: String` (JS) &ndash; main of JavaScript function that starts the main method.
  Note that it does not affect the name or the signature of a main method in Java sources (which is always `main`). 
  Default value is `main`.
* `targetFileName: String` (JS, Wasm, WASI) &ndash; name of target file. Default value is 
  `\${projectName}.js` or `\${projectName}.wasm` respectively.
* `addedToWebApp: Boolean` (JS, Wasm) &ndash; used in conjunction with `war` plugin.
  Adds corresponding TeaVM task as a dependency to WAR tasks and includes TeaVM output into generated `.war` file.
  Default value is `false`.
* `minHeapSize: Int` (Wasm, WASI, C) &ndash; minimal (initial) heap size, in megabytes, of resulting virtual machine.
  Default value is `1`.
* `maxHeapSize: Int` (Wasm, WASI, C) &ndash; maximal heap size, in megabytes, of resulting virtual machine.
  Default value is `16`.
* `heapDump: Boolean` (C) &ndash; include into generated virtual machine metadata, that allows
  this VM to generate heap dump on irrecoverable crash. Default value is `false`.
* `shortFileNames: Boolean` (C) &ndash; generate shorter file names. Used to work-around buggy Microsoft C++ compiler.
  Default value is `false`.


## Including dependencies

TeaVM produces number of additional libraries.
To shorten access to these libraries, you can use TeaVM DSL object instead of providing full addresses.
These shortcuts are available in TeaVM DSL object.

* `teavm.libs.jso` &ndash; JSO library that defined primitives for interacting with native JavaScript.
* `teavm.libs.jsoApis` &ndash; library that, based on JSO primitives, declares stubs for interacting with commonly used
  browser APIs.
* `teavm.libs.interop` &ndash; library that defines primitives for interacting with external WebAssembly modules
  and with native libraries (in case of C target).
* `teavm.libs.metaprogramming` &ndash; library that simplifies compile-time code generation,
  that can be used instead of reflection.

example:

```groovy
dependencies {
    implementation teavm.libs.jso
}
```


## Properties

TeaVM plugin provides its own properties that can be used instead of standard `gradle.properties` file.
Unlike `gradle.properties`, TeaVM properties read from two files: `teavm.properties` and `teavm-local.properties`,
where definitions in the second file override properties from the first one.
`teavm-local.properties` should usually be added to VCS ignore configuration,
that allows developer to tune build configuration without affecting VCS state.

Plugin also allows to define configuration by redefining properties.
Following list defines these properties:

* `js.obfuscated`
* `js.sourceMap`
* `js.strict`
* `js.addedToWebApp` 
* `js.optimization`
* `wasm.addedToWebApp`
* `wasi.optimization`
* `c.heapDump`
* `c.shortFileNames`
* `c.optimization`
* `debugInformation`
* `fastGlobalAnalysis`
* `outOfProcess`
* `processMemory`

You can also read values of properties via TeaVM DSL object using following notation:
`teavm.property('propertyName')`.


# Tasks

TeaVM plugin adds number of tasks that you can invoke directly or add to dependencies of lifecycle task.
Here's the list of these tasks:

* `generateJavaScript`
* `generateWasm`
* `generateWasi`
* `generateC`

Usually you may want to include them to dependencies of other tasks, for example:

```groovy
tasks.assemble.dependsOn(tasks.generateWasi)
```


# Additional source set and configuration

In case you need to write a web application with support of TeaVM,
you may want to avoid presence of TeaVM-specific client libraries and class files in `war` file.
For this purpose you can put your client code under `teavm` source set, i.e.
in `src/teavm/java` (or `src/teavm/kotlin`, `src/teavm/scala` respectively) 
instead of usual `src/main/java`.
You can also put TeaVM-specific libraries into `teavm` configuration instead of `implementation`, for example:

```groovy
dependencies {
    teavm(teavm.libs.jsoApis)
}
```

