The easiest way to run TeaVM is to use `teavm-maven-plugin`.
Here is an example of how to use it:

```xml
<project>
  ...
  <dependencies>
    <!-- This dependency is required by TeaVM to emulate subset of Java class library -->
    <dependency>
      <groupId>org.teavm</groupId>
      <artifactId>teavm-classlib</artifactId>
      <version>${teavm_version}</version>
    </dependency>
  </dependencies>
  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>${teavm_version}</version>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
            </goals>
            <phase>process-classes</phase>
            <configuration>
              <mainClass>org.teavm.samples.HelloWorld</mainClass>
              <debugInformationGenerated>true</debugInformationGenerated>
              <sourceMapsGenerated>true</sourceMapsGenerated>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```


# The `compile` goal

Here is the list of parameters supported by the goal.
Most of them can also be set via Maven properties (shown in parentheses), e.g. `-Dteavm.asyncEnabled=true`.

**General**

* **mainClass** (`teavm.mainClass`) &ndash; a fully qualified name of a class that contains
  the `main(String[])` method.
* **targetType** (`teavm.targetType`) &ndash; what code to generate. Following options are supported:
  * *JAVASCRIPT* (default)
  * *WEBASSEMBLY_GC*
  * *C*
* **targetDirectory** &ndash; a path to a directory where TeaVM produces all its output.
  The default value is `\${project.build.directory}/javascript`.
* **targetFileName** (`teavm.targetFileName`) &ndash; a name of a produced file.
  The default value is `classes.js`, `classes.wasm` or `classes.c`, depending on the target type.
* **classFiles** &ndash; a path to a directory where TeaVM should take class files produced by Java compiler.
  The default value is `\${project.build.outputDirectory}`. The default value should be used in most cases.
* **compileScopes** &ndash; list of Maven dependency scopes, which are included into the classpath of TeaVM.
* **properties** &ndash; properties, in standard Maven properties format, that will be passed to the TeaVM builder.
  These properties are available to TeaVM plugins and [compiler extensions](/docs/runtime/compiler-extensions.html).
* **classesToPreserve** &ndash; list of classes which should be preserved by TeaVM during compilation,
  even if TeaVM does not see that they are used.
* **stopOnErrors** (`teavm.stopOnErrors`) &ndash; whether build should fail if TeaVM reports errors.
  The default value is `true`.

**Code generation**

* **minifying** (`teavm.minifying`) &ndash; whether TeaVM should produce obfuscated code.
  The default value is `true`. Obfuscation decreases the size of the generated code two or three times,
  so it should be preferred in most cases. Prefer using source maps for debugging.
* **optimizationLevel** (`teavm.optimizationLevel`) &ndash; how strong should TeaVM optimize your code.
  Following options are supported:
  * *SIMPLE* (default) &ndash; perform only basic optimizations, remain friendly to the debugger;
    recommended for development;
  * *ADVANCED* &ndash; perform more optimizations, sometimes may stick debugger; recommended for production;
  * *FULL* &ndash; perform aggressive optimizations; increase compilation time, sometimes can make code
    even slower; recommended for WebAssembly GC and C.
* **fastGlobalAnalysis** (`teavm.fastGlobalAnalysis`) &ndash; improve compilation time by reducing costs
  of global analysis, at the cost of less efficient code. The default value is `false`.
* **strict** (`teavm.strict`) &ndash; add more runtime checks (like null checks, array range checks)
  into generated code, which makes it more conforming to JVM behaviour at the cost of performance.
  The default value is `false`.
* **asyncEnabled** (`teavm.asyncEnabled`) &ndash; enables support for coroutines, which is required to use
  threads and blocking APIs like `Thread.sleep` or `Object.wait`. When disabled, TeaVM reports an error for each
  reachable method that requires coroutines. Coroutines have significant negative impact on size and performance
  of the whole application, see [Coroutines](/docs/runtime/coroutines.html) for details.
  The default value is `false`.
* **assertionsRemoved** (`teavm.assertionsRemoved`) &ndash; removes `assert` statements from the generated code.
  Classes and methods that are only reachable from assertions are not included into the output either.
  Recommended for production builds. The default value is `false`.
* **transformers** &ndash; an array of fully qualified class names.
  Each class must implement [ClassHolderTransformer](/javadoc/${teavm_branch}/core/org/teavm/model/ClassHolderTransformer.html)
  interface and have a public no-argument constructor. These transformers are used to transform `ClassHolder`s,
  that are SSA-based representation of JVM classes.
  Transformers run right after parsing JVM classes and producing SSA representation.
* **incremental** (`teavm.incremental`) &ndash; whether TeaVM should build code incrementally.
  Incremental build speeds up the building process by caching results of some build phases.
  However, TeaVM is unable to perform several optimizations during incremental build, so this feature is
  not recommended for production build. The default value is `false`.
* **cacheDirectory** &ndash; a path to a directory that is used to keep cache of incremental builder.
  The default value is `\${project.build.directory}/teavm-cache`.

**Debugging**

* **debugInformationGenerated** (`teavm.debugInformationGenerated`) &ndash; whether TeaVM should produce
  debug information in its own format, used by IDEA debugger and by stack trace deobfuscator.
  The default value is `false`.
* **sourceMapsGenerated** (`teavm.sourceMapsGenerated`) &ndash; whether TeaVM should produce
  [source maps](https://developer.mozilla.org/en-US/docs/Glossary/Source_map). The default value is `false`.
* **sourceFilesCopied** (`teavm.sourceFilesCopied`) &ndash; whether TeaVM should copy source files into
  the output directory. This option is useful in conjunction with the **sourceMapsGenerated**.
  TeaVM won't copy all the sources, but only sources of classes that are included into the output.
  The default value is `false`.
* **sourceDirectory** &ndash; directory with source files of the project, used together with
  **sourceFilesCopied**. The default value is `\${project.build.sourceDirectory}`.

**JavaScript-specific**

* **jsModuleType** (`teavm.jsModuleType`) &ndash; which type of JavaScript module to use:
  * *COMMON_JS* &ndash; CommonJS (compatible with node.js);
  * *UMD* (default) &ndash; UMD (automatically detect, at run time, AMD or CommonJS module system;
    behave as IIF otherwise);
  * *NONE* &ndash; no module system, all code placed in immediately-invoked function (IIF);
  * *ES2015* &ndash; [ES2015 module](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules).
* **entryPointName** (`teavm.entryPointName`) &ndash; name of JavaScript function that starts the main method.
  The default value is `main`.
* **maxTopLevelNames** (`teavm.maxTopLevelNames`) &ndash; how many names to generate at top-level.
  All other declarations are generated as properties of some additional object.
  The reason to limit the number of top-level declarations is the bug in Chromium-based browsers that throw
  stack overflow error. The default value is `80000`.

**WebAssembly-specific**

* **wasmDebugInfoLocation** (`teavm.debugInfoLocation`) &ndash; where to store debug information:
  * *EXTERNAL* (default) &ndash; write a separate `.teadbg` file alongside the `.wasm` file;
  * *EMBEDDED* &ndash; embed debug info as a custom section inside the `.wasm` file.
* **minDirectBuffersSize** (`teavm.minDirectBuffersSize`) &ndash; minimum linear memory size in megabytes,
  used for NIO direct buffers and JS/Wasm data transfer. The default value is `2`.
* **sharedBuffer** (`teavm.sharedBuffer`) &ndash; use a shared `ArrayBuffer` (backed by `SharedArrayBuffer`)
  for the linear memory region used by direct NIO buffers and JS/Wasm data transfer.
  The default value is `false`.

**C-specific**

* **minHeapSize** (`teavm.minHeapSize`) &ndash; minimal (initial) heap size, in megabytes.
  The default value is `4`.
* **maxHeapSize** (`teavm.maxHeapSize`) &ndash; maximal heap size, in megabytes. The default value is `128`.
* **heapDump** (`teavm.heapDump`) &ndash; include into generated virtual machine metadata that allows
  the VM to generate heap dump on irrecoverable crash. The default value is `false`.
* **shortFileNames** (`teavm.shortFileNames`) &ndash; generate shorter file names. Used to work around the buggy
  Microsoft C++ compiler. The default value is `false`.

**Running compiler**

* **outOfProcess** (`teavm.outOfProcess`) &ndash; whether TeaVM should be executed in a separate process.
  The default value is `false`.
* **processMemory** (`teavm.processMemory`) &ndash; in case TeaVM is executed in a separate process,
  how much memory, in megabytes, should be given to this process. The default value is `512`.
* **jdkToolchain** &ndash; requirements for the JDK toolchain used to run TeaVM compiler (e.g. `<version>`).
  When a matching toolchain is found, the compiler is launched out of process using that JDK.
  When omitted, the toolchain selected by the `maven-toolchains-plugin` (if any) is used.


# The `copy-webassembly-gc-runtime` goal

Copies the JavaScript runtime, required to load WebAssembly GC module, to the output directory.
Supported parameters:

* **targetDirectory** &ndash; where to copy the runtime. The default value is `\${project.build.directory}/javascript`.
* **runtimeFileName** (`teavm.wasmGC.runtime.fileName`) &ndash; name of the runtime file.
  The default value is `classes.wasm-runtime.js`.
* **minified** (`teavm.wasmGC.runtime.minified`) &ndash; whether to copy minified version of the runtime.
  The default value is `true`.
* **modular** (`teavm.wasmGC.runtime.modular`) &ndash; whether to copy the runtime as an ES module.
  The default value is `false`.
* **deobfuscator** (`teavm.wasmGC.runtime.deobfuscator`) &ndash; whether to copy the stack trace deobfuscator.
  The default value is `false`.
* **deobfuscatorFileName** (`teavm.wasmGC.deobfuscator.fileName`) &ndash; name of the deobfuscator file.
  The default value is `classes.wasm-deobfuscator.wasm`.
