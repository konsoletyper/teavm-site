There are several options for using TeaVM. One is a Maven build.
The easiest way to create a new TeaVM project is to type in this command:

```bash
mvn -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.teavm \
  -DarchetypeArtifactId=teavm-maven-webapp \
  -DarchetypeVersion=${teavm_version} archetype:generate
```

Now you can execute `mvn clean package` and get the generated `war` file.
Deploy this `war` in Tomcat or another container, or simply unzip it and open the `index.html` page.


## Gradle

Alternatively you can use TeaVM with gradle. Here's a minimal `build.gradle`:

```groovy
plugins {
    id "java"
    id "war"
    id "org.teavm" version "${teavm_dev_version}"
}
repositories {
    maven { url = uri("https://teavm.org/maven/repository") }
    mavenCentral()
}
teavm.js {
    addedToWebApp = true
    mainClass = "fully.qualified.name.of.MainClass"
}
```

and `settings.gradle`

```groovy
pluginManagement {
    repositories {
        maven { url = uri("https://teavm.org/maven/repository") }
        mavenCentral()
        gradlePluginPortal()
    }
}
```


## Further learning

You can learn more about TeaVM from these examples:
 
* [Application gallery](/gallery.html).
* [Samples from TeaVM repository](https://github.com/konsoletyper/teavm/tree/master/samples).
