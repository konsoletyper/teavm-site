---
title: Getting started
---

There are several options for using TeaVM. One is a Maven build.
The easiest way to create a new TeaVM project is to type in this command:

```bash
mvn -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.teavm \
  -DarchetypeArtifactId=teavm-maven-webapp \
  -DarchetypeVersion={{ site.teavm_version }} archetype:generate
```

Now you can execute `mvn clean package` and get the generated `war` file.
Deploy this `war` in Tomcat or another container, or simply unzip it and open the `index.html` page.


## Using the Flavour framework

Another option is to use Flavour by using another archetype:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.teavm.flavour \
  -DarchetypeArtifactId=teavm-flavour-application \
  -DarchetypeVersion={{ site.flavour_version }}
```

This should generate a new minimal Flavour application.

You can build the generated project as usual by `mvn package`.
After successful build you should be able to open `target/hello-1.0-SNAPSHOT/index.html` in your browser.


## Further learning

You can learn more about TeaVM from these examples:
 
* [Application gallery](/gallery.html).
* [Samples from TeaVM repository](https://github.com/konsoletyper/teavm/tree/master/samples).
* [Flavour example application](https://github.com/konsoletyper/teavm-flavour/tree/master/example).
