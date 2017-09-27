---
title: Getting started
---

There are several options of using TeaVM. One is the Maven build.
The easiest way to create a new TeaVM project is to type in the command line:

```bash
mvn -DarchetypeCatalog=local \
  -DarchetypeGroupId=org.teavm \
  -DarchetypeArtifactId=teavm-maven-webapp \
  -DarchetypeVersion=0.5.0 archetype:generate
```

Now you can execute `mvn clean package` and get the generated `war` file.
Deploy this `war` in Tomcat or another container, or simply unzip it and open the `index.html` page.


## Using Flavour framework

Another option is to use Flavour, you should specify another archetype:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=org.teavm.flavour \
  -DarchetypeArtifactId=teavm-flavour-application \
  -DarchetypeVersion=0.1.0
```

This should generate a new minimal Flavour application.

You can build the generated project as usual by `mvn package`.
After successful build you should be able to open `target/hello-1.0-SNAPSHOT/index.html` in your browser.