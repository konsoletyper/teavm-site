## Generating source maps

The preferred way of building TeaVM is to build it by Maven.
Generating source maps in this case is straightforward: in the `teavm-maven-plugin`
set the `sourceMapsGenerated` property to `true`.
Also, you will need your source files to be included in your web application.
If you don't want to copy the source files manually, you can also set the `sourceFilesCopied` property.
See the following snippet:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>${teavm_version}</version>
        <executions>
          <execution>
            ...
            <configuration>
              ...
              <sourceMapsGenerated>true</sourceMapsGenerated>
              <sourceFilesCopied>true</sourceFilesCopied>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  ...
</project>
```

or from Gradle:

```kotlin
teavm {
  js {
    sourceMap = true
  }
  wasmGC {
    sourceMap = true
  }
}
```

These two properties are set by default if you generate the project by the `teavm-maven-webapp` archetype.

Note that IDEA builder automatically configured from Maven, so if you have configured Maven plugin to
generate source maps, IDEA builder will generate them as well.


## Disabling obfuscation

This step will provide more human-readable names for functions and variables in generated output.
In case of JS this turns off obfuscation and enable generation of human-readable names.
In case of WebAssembly this turn on generation of `name` section, which provides human-readable names
to WebAssembly functions.
In both cases, don't disable obfuscation for production, since it will add significant overhead to generated file.

So, for Maven add following to `<configuration>` section of plugin execution:

```xml
<minifying>false</minifying>
```

or for Gradle:

```kotlin
teavm {
  js {
    obfuscated = false
  }
  wasmGC {
    obfuscated = false
  }
}
```


## Using IDEA debugger

*Note that as for now (release 0.11.0) WebAssembly is not support in IDEA debugger.*

TeaVM has its own built-in debugger.
It requires a special file, that contains mapping between JVM and JavaScript locations, names, etc.
You should enable generation of this file in your Maven configuration.
Simply set the `debugInformationGenerated` property to `true`, as in the following snippet:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>${teavm_version}</version>
        <executions>
          <execution>
            ...
            <configuration>
              ...
              <debugInformationGenerated>true</debugInformationGenerated>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  ...
</project>
```

or in Gradle

```kotlin
teavm {
  js {
    debugInformation = true
  }
}
```

Note that enabling this option *does not* affect the generated JavaScript file, so you can debug code
without performance penalty.

Debugger connects to a backend that can control execution of JavaScript code and translates your
commands into commands to this backend, mapping all locations and names.
For now only backend for remote Google Chrome available.
This backend simply exposes Google Chrome remote debug protocol over WebSocket.

To debug TeaVM application, you should first install
[TeaVM debug agent](https://chrome.google.com/webstore/detail/teavm-debugger-agent/jmfipnkacgdmdhapfciejmfgfhfonfgl)
for Google Chrome.

To start debug session, you need to launch a TeaVM debug configuration.
In menu, select *Run* -> *Edit configurations...*.
Click on the *plus* button and pick *TeaVM debug server*, a new launch configuration should appear.
Close configuration window by pressing *OK* and start a new debug session.

Now you should connect Google Chrome to TeaVM debug server.
Open your application in Google Chrome and press the *TeaVM debugger agent* button beside the address bar
(the green teapot icon).
Put breakpoints on your Java code and when they hit, you can see the execution state of your code and
issue debug commands.

Notice that in the *Debug* view you have two threads: main and JavaScript.
They are not threads, but different representations of the debug process.


## Decoding stack traces in exceptions

*Note that out-of-the box this feature works only in WebAssembly GC backend.*

By default, TeaVM won't fill exceptions with stack traces.
This is done by purpose, since generating stack traces requires significant amount of metadata.
Given that for regular users exception stacks provide no useful information, 
it makes no sense to force them to download additional binary files just for the feature
they don't use.

*Sometimes you may want to collect exception stack traces from users either automatically or manually.
In this case you can collect native WebAssembly stack traces (use `JSExceptions.getJSException` method)
from `teavm-jso` module. There's no out-of-the box solution or some documentation for this, but
you can start by learning 
[source code](https://github.com/konsoletyper/teavm/blob/cc218fcd503f60ca83e4c45602f28c93da4fb7be/tools/deobfuscator-wasm-gc/src/main/java/org/teavm/tooling/deobfuscate/wasmgc/DeobfuscatorFactory.java)
of development mode deobfuscator and ask question on forum.*

To enable stack traces, you need to generate debug information.
In maven apply following configuration:

```xml
<project>
  ...
  <build>
    <plugins>
      <plugin>
        <groupId>org.teavm</groupId>
        <artifactId>teavm-maven-plugin</artifactId>
        <version>${teavm_version}</version>
        <executions>
          <execution>
            ...
            <configuration>
              ...
              <debugInformationGenerated>true</debugInformationGenerated>
              <wasmDebugInfoLocation>EXTERNAL</wasmDebugInfoLocation>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  ...
</project>
```

In Gradle apply following configuration:

```kotlin
teavm {
  wasmGC {
    debugInformation = true
    debugInfoLocation = WasmDebugInfoLocation.EXTERNAL
  }
}
```

`debugInfoLocation` is optional.
You can pass `WasmDebugInfoLocation.EMBEDDED` to make TeaVM to embed debug information into WebAssembly file. 

Then, in your HTML or JS file pass additional options to initialize TeaVM module:

```js
let teavm = await TeaVM.wasmGC.load("path/to/file.wasm", {
    stackDeobfuscator: {
        enabled: true,
        infoLocation: "auto", 
        externalInfoPath: "path/to/file.wasm.tdbg"
    }
});
```

`infoLocation` is optional. Other possible values are `"external"` and `"embedded"`.
By default, TeaVM will try to auto-detect debug information file location.

`externalInfoPath` is also optional. By default, is will match the name chosen by Gradle or Maven plugin.