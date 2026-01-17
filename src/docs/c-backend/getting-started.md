The primary goal of this backend is to cover native platforms which are not supported by GraalVM Native Image
or similar tools. C compiler exists everywhere, and TeaVM's C backend tries to emit standard C only,
without any hacks and compiler-specific extensions.

Unfortunately, TeaVM relies on handwritten runtime, which should be maintained for all possible platforms,
which are not available to the maintainers. If you want to use TeaVM with a platform which it does not support,
feel free to open a PR. Usually, it's not a rocket science, and you don't need to learn all 
internal compiler infrastructure to make the runtime compatible with your platform.
The runtime code can be found at 
[core/src/main/resources/org/teavm/backend/c](https://github.com/konsoletyper/teavm/tree/master/core/src/main/resources/org/teavm/backend/c).


# Step 1. Generate C code with Gradle

Here's a minimal `build.gradle`:

```groovy
plugins {
    id "java"
    id "war"
    id "org.teavm" version "${teavm_version}"
}
repositories {
    mavenCentral()
}

// This is optional, but for real-world applications you need to interact with native libs.
// This dependency provides useful wrappers.
dependencies {
    implementation teavm.libs.interop
}

teavm {
    all {
        mainClass = "example.MainClass"
    }
    c {
        minHeapSize = 2   // optional
        maxHeapSize = 128 // optional
    }
}
```

where `MainClass` could do something simple like writing "Hello, world" string in the console, for example:

```java
package example;

public class MainClass {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
```

Now run 

```sh
./gradlew generateC
```

and you can find generated C code in `build/generated/teavm/c`.


# Step 2. Generate native code

Just for educational/evaluation purpose you can start with compilation of `all.c`, though it's not
recommended for daily use. For example, if you are using `gcc`, you can go to `build/generated/teavm/c`
and run:

```sh
gcc all.c
```

and `a.out` appears in the same dir.

Then, you can improve your development experience by using particular C build tool, like CMake.
You need to parse `all.txt`, which contains path to a source file per line. For example, in CMake:

```cmake
cmake_minimum_required(VERSION 3.31)
project(teavm_example C)

set(CMAKE_C_STANDARD 11)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY
    ${CMAKE_SOURCE_DIR}/build/dist/
)

if(NOT CMAKE_CONFIGURATION_TYPES AND NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
endif()

set(TEAVM_C_DIR ${CMAKE_SOURCE_DIR}/build/generated/teavm/c)

file(STRINGS
    ${TEAVM_C_DIR}/all.txt
    TEAVM_C_SOURCES_REL
)

set(TEAVM_C_SOURCES "")
foreach(src ${TEAVM_C_SOURCES_REL})
    list(APPEND TEAVM_C_SOURCES ${TEAVM_C_DIR}/${src})
endforeach()

set_property(DIRECTORY APPEND PROPERTY
    CMAKE_CONFIGURE_DEPENDS
    ${TEAVM_C_DIR}/all.txt
)

set_source_files_properties(
    ${TEAVM_C_SOURCES}
    PROPERTIES
        GENERATED TRUE
)

add_executable(teavm_example ${TEAVM_C_SOURCES})
target_compile_definitions(teavm_example PRIVATE _XOPEN_SOURCE=700)
target_compile_definitions(teavm_example PRIVATE __USE_XOPEN)
target_compile_definitions(teavm_example PRIVATE _GNU_SOURCE)


target_compile_features(teavm_example PRIVATE c_std_11)

target_compile_options(teavm_example PRIVATE
    $<$<CONFIG:Debug>:-g>
    $<$<CONFIG:Release>:-O3>
)

set_target_properties(teavm_example PROPERTIES
    INTERPROCEDURAL_OPTIMIZATION_RELEASE ON
)
```

Then run 

```
cmake .
make
```

and you'll get executable in `build/dist`

You can also configure Gradle to build executable. Just add


```groovy
def cmakeTask = tasks.register("cmake", Exec) {
    dependsOn tasks.generateC
    inputs.files tasks.generateC.outputDir.map { new File(it, "all.txt") }
    inputs.files layout.projectDirectory.file("CMakeLists.txt")
    outputs.file layout.projectDirectory.file("Makefile")
    workingDir = layout.projectDirectory
    commandLine = [ "cmake", "." ]
}

def buildExecutableTask = tasks.register("buildExecutable", Exec) {
    group "build"
    dependsOn cmakeTask
    dependsOn tasks.generateC
    inputs.files fileTree(tasks.generateC.outputDir)
    outputs.file layout.buildDirectory.dir("dist/teavm_example")
    commandLine = [ "make" ]
}

tasks.build {
    dependsOn buildExecutableTask
}
```

Now you can run

```sh
./gradlew buildExecutable
```

or even

```sh
./gradlew build
```

to get executable.

You can take a look at [example](https://github.com/konsoletyper/teavm/tree/master/samples/benchmark)
that shows how to write a GTK+ application with Java and TeaVM.