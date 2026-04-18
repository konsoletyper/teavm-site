TeaVM's WebAssembly GC backend can link native C/C++ code compiled with Emscripten into the same
page as your Java application. The Gradle plugin automates the build pipeline; you write Java and
C/C++ and wire them together with the `@Import` annotation.

A working example is in
[samples/emscripten](https://github.com/konsoletyper/teavm/tree/master/samples/emscripten).
This page explains the concepts behind that example in detail.


## How it works

TeaVM compiles Java to a `.wasm` file using the WebAssembly GC instruction set. Separately,
Emscripten compiles C/C++ to a *relocatable* WebAssembly module. The TeaVM runtime stitches both
modules together at load time. The two modules share a single region of linear memory — NIO direct
buffers live there alongside the C heap. This shared linear memory is the only bridge for passing
data between Java and C.


## Configuring the Emscripten SDK location

You must tell it where the Emscripten SDK is
installed by setting the `emscripten-location` property to the directory containing the `emcc`
binary (typically `<emsdk>/upstream/emscripten`).

Because the SDK path differs per machine, the recommended place is either the project-local
`teavm-local.properties` file (which should be git-ignored) or the user-level
`~/.gradle/gradle.properties`:

```properties
# teavm-local.properties  (or ~/.gradle/gradle.properties with a teavm. prefix)
emscripten-location=/path/to/emsdk/upstream/emscripten
```

```properties
# ~/.gradle/gradle.properties
teavm.emscripten-location=/path/to/emsdk/upstream/emscripten
```

See the [Gradle plugin reference](/docs/tooling/gradle.html#properties) for the full property
resolution order.


## C/C++ source directory

C/C++ files go in the `emcc` subdirectory of whichever source set contains your TeaVM Java code.
If you use the `teavm` source set (recommended when you have a mixed server + client project, to
keep client-only dependencies out of the WAR), the directory is `src/teavm/emcc/`. If your Java
code lives in the ordinary `main` source set, use `src/main/emcc/` instead.

The Gradle plugin documentation explains when and why to use the `teavm` source set versus `main`.
The `emcc` convention works the same way regardless of which source set you choose.

All `.c`, `.cpp`, `.C`, `.cc`, `.cxx`, and `.c++` files found there are compiled together in a
single `emcc` invocation, so you can split native code across as many files as you like.


## Gradle configuration

```kotlin
teavm.wasmGC {
    addedToWebApp = true
    mainClass = "com.example.Main"
    emscripten {
        enabled = true
        exportedFunctions.add("_addInBuffer")
        compilerArgs.add("-O2")  // optional: passed directly to emcc
    }
}
```

`exportedFunctions` lists every C function Java will call. The **leading underscore** is an
Emscripten convention and is required. `compilerArgs` forwards arbitrary flags to `emcc` — useful
for optimization levels, extra include paths, or debug info.


## Declaring native methods with `@Import`

Every C function that Java calls must be declared as a `static native` method annotated with
`@Import`:

```java
import org.teavm.interop.Import;

@Import(module = "native", name = "addInBuffer")
private static native void cppAdd(IntBuffer buffer);
```

* **`name`** — must match the C function name exactly (after `extern "C"` stripping of name
  mangling).
* **`module`** — an arbitrary string that identifies which Emscripten module provides the function.
  This same string is used as the key in `emscriptenModules` when loading the Wasm module from
  JavaScript (see *Loading in HTML* below). Multiple `@Import` annotations in the
  same class can share the same `module` value if they all come from the same compiled binary.


## Passing data between Java and C

### Primitive types

Primitive numeric types map directly to the corresponding WebAssembly value types:

| Java type               | C type      | Wasm type |
|-------------------------|-------------|-----------|
| `boolean`, `byte`, `short`, `char`, `int` | `int32_t`   | `i32`     |
| `long`                  | `int64_t`   | `i64`     |
| `float`                 | `float`     | `f32`     |
| `double`                | `double`    | `f64`     |

Sub-`int` types (`boolean`, `byte`, `short`, `char`) are widened to `i32` before the call.
On the C side you may receive them as `int32_t` (or as the narrower C type if you want — the
value fits). Return types follow the same mapping; `void` is also valid.

Example — a C function that adds two integers and returns the result:

```cpp
extern "C" {
    int32_t add(int32_t a, int32_t b) {
        return a + b;
    }
}
```

```java
@Import(module = "native", name = "add")
private static native int add(int a, int b);
```

### Passing structured data via NIO direct buffers

Java objects live in GC-managed memory and cannot be passed as raw pointers to C. The only way to
exchange structured or bulk data is through the **shared linear memory**, which NIO direct buffers
are allocated in.

`ByteBuffer.allocateDirect(n)` allocates `n` bytes in linear memory. You can obtain typed views
with `.asIntBuffer()`, `.asLongBuffer()`, `.asFloatBuffer()`, etc. When such a buffer (or its
typed view) is passed to an `@Import` method, TeaVM passes the underlying memory address to C as
a pointer. The C function receives a plain pointer into linear memory and can read or write through
it freely.

```java
// Java: allocate 3 ints in shared linear memory
var buffer = ByteBuffer.allocateDirect(3 * Integer.BYTES).asIntBuffer();
buffer.put(0, 23);
buffer.put(1, 42);
cppAdd(buffer);       // pass as IntBuffer
int result = buffer.get(2);
```

```cpp
// C: receive as a pointer to int32_t in linear memory
extern "C" {
    void addInBuffer(int32_t* args) {
        args[2] = args[0] + args[1];
    }
}
```

The C type of the pointer should match the element type of the Java buffer view:

| Java buffer type | C pointer type |
|-----------------|----------------|
| `ByteBuffer`    | `int8_t*` / `void*` |
| `ShortBuffer`   | `int16_t*`     |
| `IntBuffer`     | `int32_t*`     |
| `LongBuffer`    | `int64_t*`     |
| `FloatBuffer`   | `float*`       |
| `DoubleBuffer`  | `double*`      |

Note that NIO buffer positions and limits are Java-side metadata — C sees only the raw base pointer.
If you use a buffer view obtained with `.asIntBuffer()`, the pointer passed to C already accounts
for the byte offset of that view inside the original `ByteBuffer`.

> **Important:** Only `ByteBuffer.allocateDirect()` allocates in shared linear memory. Heap-backed
> buffers (`ByteBuffer.wrap(...)`, `ByteBuffer.allocate(...)`) are ordinary Java objects and cannot
> be passed to C code.


## Loading in HTML

```html
<script src="wasm-gc/app.wasm-runtime.js"></script>
<script>
  async function launch() {
    const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
      emscriptenModules: {
        "native": {
          pathToJs:   "app.wasm-native.js",
          pathToWasm: "wasm-gc/app.wasm-native.wasm"
        }
      }
    });
    teavm.exports.main([]);
  }
</script>
```

The key `"native"` in `emscriptenModules` must match the `module` value used in all `@Import`
annotations that refer to this binary. You can load multiple Emscripten modules simultaneously by
adding more entries to `emscriptenModules`, each with a distinct key.

For the full JavaScript loading API see [Loader API](/docs/wasm-gc-backend/loader.html).


## Troubleshooting

* **`emcc` not found** — verify that `emscripten-location` points to the directory containing the
  `emcc` binary. Check the
  [property resolution order](/docs/tooling/gradle.html#properties) if the value does not seem
  to be picked up.
* **Undefined symbol at link time** — the function name in `exportedFunctions` must have a leading
  underscore and match the C function name exactly.

