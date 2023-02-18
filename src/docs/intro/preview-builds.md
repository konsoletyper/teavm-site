You may want to access new features and don't want to wait until stable release is published on Maven Central.
In this case you can get latest development build from TeaVM repository.
Recent version is displayed by the following badge: ![download](https://teavm.org/maven/latestBadge.svg).

Actual steps depend on how you using TeaVM. 


## Maven 

All you need is to put the following in your `pom.xml`:

```xml
  <repositories>
    <repository>
      <id>teavm-dev</id>
      <url>https://teavm.org/maven/repository</url>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>teavm-dev</id>
      <url>https://teavm.org/maven/repository</url>
    </pluginRepository>
  </pluginRepositories>
```


## Gradle

In `settings.gradle`

```kotlin
pluginManagement {
  repositories {
    maven { url = uri("https://teavm.org/maven/repository") }
    mavenCentral()
    gradlePluginPortal()
  }
}
gradle.allprojects {
  repositories {
    maven { url = uri("https://teavm.org/maven/repository") }
    mavenCentral()
  }
}
```

or in `build.gradle`

```kotlin
repositories { 
  maven { url = uri("https://teavm.org/maven/repository") }
  mavenCentral()
}
```


## IDEA

IDEA plugin is also available in preview builds. You need to add corresponding repository manually to IDEA.
Open *Settings* -> *Plugins* -> *Browse repositories...* -> *Manage repositories...*, click *Add* button
and enter `https://teavm.org/idea/dev/teavmRepository.xml`.
Then get back to *Browse repositories* and pick TeaVM plugin from list. 
