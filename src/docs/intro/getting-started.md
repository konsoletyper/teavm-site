Note that this tiny guide assumes that you are familiar with Gradle or Maven and Java language and
can build a simple Java project using one of these build systems.
If it's not the case, please, refer to corresponding Gradle, Maven and Java documentation.


## Maven

The easiest way to create a new TeaVM project is to type in this command:

```bash
mvn -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.teavm \
  -DarchetypeArtifactId=teavm-maven-webapp \
  -DarchetypeVersion=${teavm_version} archetype:generate
```

Now you can execute `mvn clean package` and get the generated `war` file.
Deploy this `war` in Tomcat or another container, or simply unzip it and open the `index.html` page.

Alternatively, you can run following command to generate WebAssembly GC project:

```bash
mvn -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.teavm \
  -DarchetypeArtifactId=teavm-maven-webapp-wasm-gc \
  -DarchetypeVersion=${teavm_version} archetype:generate
```

Note that browsers don't support loading `wasm` files from local file system, so the only option to 
run WebAssembly project is to serve it via HTTP.
There are several options to serve content via HTTP, for example:

* using [Jetty Plugin](https://jetty.org/docs/jetty/12/programming-guide/maven-jetty/jetty-maven-plugin.html)
* using IDEA (only Enterprise Edition)
* using python (`python -m http.server 8080`)
* and so on.

You can continue learning by exploring the generated project, which has plenty of comments and should
be self-explanatory.


## Gradle

Alternatively you can use TeaVM with gradle. Here's a minimal `build.gradle`:

```groovy
plugins {
    id "java"
    id "war"
    id "org.teavm" version "${teavm_version}"
}
repositories {
    mavenCentral()
}

// This is optional, but for real-world applications you need to interact with browser.
// This dependency provides useful wrappers.
dependencies {
    implementation teavm.libs.jsoApis
}

teavm {
    all {
        mainClass = "example.MainClass"
    }
    js {
        addedToWebApp = true

        // this is also optional, default value is <project name>.js
        targetFileName = "example.js"
    }
    wasmGC {
        addedToWebApp = true

        // this is also optional, default value is <project name>.wasm
        targetFileName = "example.wasm"
    }
}
```

If you don't need either JS or WebAssembly GC, you can remove corresponding `addedToWebApp` setting, 
or ever entire configuration section related to particular backend.

where `MainClass` could do something simple like writing "Hello, world" string in the console.
A bit more complex example of `MainClass` could be following:

```java
package example;

import org.teavm.jso.dom.html.HTMLDocument;

public class MainClass {
    public static void main(String[] args) {
        var document = HTMLDocument.current();
        var div = document.createElement("div");
        div.appendChild(document.createTextNode("TeaVM generated element"));
        document.getBody().appendChild(div);
    }
}
```

Finally, you need to add to your webapp resources `index.html` page, which includes `examples.js` and runs
`main` method, like this:

```html
<!DOCTYPE html>
<html>
  <head>
    <title>TeaVM example</title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <script type="text/javascript" charset="utf-8" src="js/example.js"></script>
  </head>
  <body onload="main()"></body>
</html>
```

or if you need WebAssembly, HTML file like this:

```html
<!DOCTYPE html>
<html>
  <head>
    <title>TeaVM WebAssembly example</title>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <script type="text/javascript" charset="utf-8" src="wasm-gc/example.wasm-runtime.js"></script>
    <script>
      async function main() {
          let teavm = await TeaVM.wasmGC.load("wasm-gc/example.wasm");
          teavm.exports.main([]);
      }
    </script>
  </head>
  <body onload="main()"></body>
</html>
```

now you can run build `gradle build` or, if you are using Gradle wrapper, `./gradlew build` or `gradlew.bat build`.
Finally, take `.war` file from `build/libs` directory and deploy it to any compatible container or
simply unzip and open `index.html`.

Note that during development you can use [Gretty](https://plugins.gradle.org/plugin/org.gretty)
plugin to serve you `.war` file via HTTP.


## Further learning

You can learn more about TeaVM from these examples:
 
* [Application gallery](/gallery.html).
* [Samples from TeaVM repository](https://github.com/konsoletyper/teavm/tree/master/samples).
