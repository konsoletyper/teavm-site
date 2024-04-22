The easiest way to run TeaVM is to use `teavm-maven-plugin`.
Here is an example of how to use it:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>${teavm_version}</version>
        <dependencies>
          <!-- This dependency is required by TeaVM to emulate subset of Java class library -->
          <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-classlib</artifactId>
            <version>${teavm_version}</version>
          </dependency>
        </dependencies>
        <executions>
          <execution>
            <goals>
              <goal>compile</goal>
            </goals>
            <phase>process-classes</phase>
            <configuration>
              <mainClass>org.teavm.samples.HelloWorld</mainClass>
              <mainPageIncluded>true</mainPageIncluded>
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


# The `build-javascript` goal

Here is the list of properties supported by the goal:

* **mainClass** &ndash; a fully qualified name of a class that contains the `main(String[])` method. 
  When this property specified, TeaVM produces `main` function in global scope that runs the translated
  version of the `main(String[])` method in the browser.
* **targetDirectory** &ndash; a path to a directory where TeaVM produces all its output.
  The default value is `\${project.build.directory}/javascript`.
* **targetFileName** &ndash; a name of a produced JavaScript file. The default value is `classes.js`.
* **minifying** (`true`|`false`) &ndash; whether TeaVM should produce minified (obfuscated) JavaScript.
  The default value is `true`. Minification decreases the size of the generated file two or three times,
  so it should be preferred in most cases, unless you are going to debug JavaScript manually.
  However, it is better to debug using source maps or TeaVM eclipse debugger.
* **debugInformationGenerated** (`true`|`false`) &ndash; whether TeaVM should produce a debug information file for
  its Eclipse plugin. The default value is `false`.
* **sourceMapsGenerated** (`true`|`false`) &ndash; whether TeaVM should produce
  [source maps](http://www.html5rocks.com/en/tutorials/developertools/sourcemaps/). The default value is `false`.
* **sourceFilesCopied** (`true`|`false`) &ndash; whether TeaVM should copy source files into the output directory.
  This option is useful in conjunction with the **sourceMapsGenerated**.
  TeaVM won't copy all the sources, but only sources of classes being decompiled. The default value is `false`.
* **incremental** (`true`|`false`) &ndash; whether TeaVM should build JavaScript incrementally.
  Incremental build speeds up the building process by caching results of some build phases.
  However, TeaVM is unable to perform several optimizations during incremental build, so this feature is
  not recommended for production build. The default value is `false`.
* **cacheDirectory** &ndash; a path to a directory that is used to keep cache of incremental builder.
  The default value is `\${project.build.directory}/teavm-cache`.
* **classFiles** &ndash; a path to a directory where TeaVM should take class files produced by Java compiler.
  The default value is `\${project.build.outputDirectory}`. The default value should be used in most cases.
* **properties** &ndash; properties, in standard Maven properties format, that will be passed to the TeaVM builder.
  These properties are available to TeaVM plugins.
* **transformers** &ndash; an array of fully qualified class names.
  Each class must implement [ClassHolderTransformer](/javadoc/${teavm_branch}/core/org/teavm/model/ClassHolderTransformer.html)
  interface and have a public no-argument constructor. These transformers are used to transform `ClassHolder`s,
  that are SSA-based representation of JVM classes.
  Transformers run right after parsing JVM classes and producing SSA representation.
* **optimizationLevel** &ndash; how strong should TeaVM optimize your code. Following options are supported:
  * *SIMPLE* &ndash; perform only basic optimizations, remain friendly to the debugger; recommended for development;
  * *ADVANCED* &ndash; perform more optimizations, sometimes may stick debugger; recommended for production;
  * *FULL* &ndash; perform aggressive optimizations; 
    increase compilation time, sometimes can make code even slower; 
    recommended for WebAssembly.
* **targetType** &ndash; what code to generate. Following options are supported:
  * *JAVASCRIPT*
  * *WEBASSEMBLY*
  * *WEBASSEMBLY_WASI*
  * *C*
* **jsModuleType** &ndash; which type of JavaScript module to use:
  * *COMMON_JS* &ndash; CommonJS (copatible with node.js);
  * *UMD* &ndash; UMD (automatically detect, at run time, AMD or CommonJS module system; behave as IIF otherwise);
  * *NONE* &ndash; no module system, all code placed in immediately-invoked function (IIF);
  * *ES2015* &ndash; [ES2015 module](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules). 