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

teavm.js {
    addedToWebApp = true
    mainClass = "example.MainClass"
    
    // this is also optional, default value is <project name>.js
    targetFileName = "example.js"
}
```

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

now you can run build `gradle build` or, if you are using Gradle wrapper, `./gradlew build` or `gradlew.bat build`.
Finally, take `.war` file from `build/libs` directory and deploy it to any compatible container or
simply unzip and open `index.html`.


## Further learning

You can learn more about TeaVM from these examples:
 
* [Application gallery](/gallery.html).
* [Samples from TeaVM repository](https://github.com/konsoletyper/teavm/tree/master/samples).
