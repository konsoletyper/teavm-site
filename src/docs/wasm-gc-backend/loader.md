# TeaVM Wasm GC loader

This document describes the JavaScript API for loading and running TeaVM-compiled WebAssembly GC modules in a browser, a Web Worker, or Node.js.

## Overview

TeaVM compiles Java (and Kotlin) code to WebAssembly GC. For each compiled `.wasm` file it also generates a companion `<name>.wasm-runtime.js` file that contains the loader and all runtime support code. The loader is exposed as `TeaVM.wasmGC` on the global object.

The three public functions are:

| Symbol | Purpose |
|---|---|
| `TeaVM.wasmGC.load(src, options?)` | Load, compile, and instantiate a `.wasm` file. The main entry point. |
| `TeaVM.wasmGC.defaults(imports, userExports, stringBuiltins)` | Low-level helper that fills an import object with all built-in TeaVM imports. Used when you need to instantiate the module yourself. |
| `TeaVM.wasmGC.wrapImport(obj)` | Wraps a plain JS object so that each property is exposed as a `WebAssembly.Global(externref)`. Needed when passing JS objects as `externref` imports. |

The two latter methods *can* be used if you want to link TeaVM method dynamically to your runtime. Their deep observation is out of scope for this document.


## Quick start

```html
<!-- 1. Load the runtime that TeaVM generated alongside the .wasm file -->
<script src="wasm-gc/app.wasm-runtime.js"></script>

<script type="module">
  // 2. Load the module
  const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm");

  // 3. Call exported Java methods
  teavm.exports.main([]);
</script>
```


## Including the runtime

Every `.wasm` file produced by TeaVM is accompanied by a `<name>.wasm-runtime.js` file. You must load that file before calling `TeaVM.wasmGC.load`. It registers `TeaVM.wasmGC` on the global object.

Note that in Maven and Gradle plugins, task/goal for producing Wasm binary does not copy runtime file. There are separate dedicated task/goal for that.

* Gradle registers this task (`copyWasmGCRuntime`) automatically. You only need to run it *or* you can run `buildWasmGC`, which runs both `generateWasmGC` and `copyWasmGCRuntime`.
* In Maven, you need to setup the goal manually. Please, create a project from Maven archetype as described in "Getting started" section and see generated `pom.xml`.

The rationale for this is following: in certain cases you may need several Wasm modules. Wasm GC backend does not generate any JS, but relies on static runtime, which makes this possible.

**In a browser:**

```html
<script src="wasm-gc/app.wasm-runtime.js"></script>
```

**In a Web Worker (classic worker):**

```js
importScripts("wasm-gc/app.wasm-runtime.js");
```

**As an ES module** — see *Advanced: ES module usage*.


## `load(src, options?)` — loading a module

```ts
TeaVM.wasmGC.load(src: string | BufferSource, options?: LoadOptions): Promise<TeaVMInstance>
```

Compiles and instantiates the Wasm module. Returns a promise that resolves with a `TeaVMInstance` once the module is ready to use.

`src` is either:
- A **URL string** — the `.wasm` file is fetched (using `fetch()` in browsers, `fs.open()` in Node.js). You *SHOULD* prefer this method for most cases, it works much faster and allows browser to utilize caching.
- A **`BufferSource`** (`ArrayBuffer` / typed array) — the bytes are compiled directly without a network request.

```js
// From a URL
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm");

// From pre-fetched bytes
const response = await fetch("wasm-gc/app.wasm");
const bytes = await response.arrayBuffer();
const teavm = await TeaVM.wasmGC.load(bytes);
```


## `TeaVMInstance` — the loaded module

The promise returned by `load()` resolves with a `TeaVMInstance`:

```ts
interface TeaVMInstance {
  exports: Record<string, unknown>; // Java @JSExport methods and globals
  instance: WebAssembly.Instance;   // raw WebAssembly instance
  module: WebAssembly.Module;       // compiled WebAssembly module
}
```

### `exports`

`exports` contains every symbol that the Java code exports to JavaScript (via `@JSExport` or the JSO API). Call Java `static` methods directly:

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm");
teavm.exports.main([]);                       // void main(String[] args)
const result = teavm.exports.calculate(42);   // int calculate(int n)
```

### `instance` and `module`

The raw `WebAssembly.Instance` and `WebAssembly.Module` are exposed for advanced use cases such as introspection or sharing the module across workers.


## `LoadOptions` reference

All fields are optional.

```ts
interface LoadOptions {
  installImports?:    (imports: Record<string, unknown>) => void;
  stackDeobfuscator?: DeobfuscatorOptions;
  memory?:            MemoryOptions;
  stack?:             number;
  emscriptenModules?: Record<string, { pathToJs: string; pathToWasm: string }>;
  nodejs?:            boolean;
  noAutoImports?:     boolean;
}
```

### `installImports`

```ts
installImports?: (imports: Record<string, unknown>) => void
```

A callback invoked with the complete Wasm import object *after* all built-in TeaVM imports have been filled in but *before* the module is instantiated. Use it to:

- Override built-in behaviour (e.g. redirect stdout/stderr).
- Add new import namespaces that your Java code calls via JSO or native methods.

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  installImports(imports) {
    // Override stdout so output goes somewhere custom
    imports.teavmConsole.putcharStdout = (charCode) => myOutput(charCode);

    // Expose your own namespace to Java native methods
    imports.myNative = {
      add: (a, b) => a + b,
      now: () => Date.now(),
    };
  }
});
```

The `imports` object is structured as `{ [namespace]: { [functionName]: fn } }`, matching the Wasm import section. All built-in TeaVM namespaces (`teavmConsole`, `teavmDate`, `teavmMath`, etc.) are already present when the callback runs, so you can read and selectively replace individual entries.

### `stackDeobfuscator`

```ts
interface DeobfuscatorOptions {
  enabled?:          boolean;
  infoLocation?:     "auto" | "embedded" | "external";
  path?:             string;
  externalInfoPath?: string | Int8Array;
}
```

When enabled, Wasm function addresses in exception stack traces are translated back into Java class / method / file / line information.

| Field | Default | Description                                                                                                                                                                   |
|---|---|---|
| `enabled` | `false` | Set to `true` to activate deobfuscation.                                                                                                                                      |
| `infoLocation` | `"auto"` | Where to look for debug info: `"embedded"` reads from a custom section inside the `.wasm` file itself; `"external"` reads a separate `.teadbg` file; `"auto"` tries both.     |
| `path` | `<src>-deobfuscator.wasm` | Path to the deobfuscator Wasm module (bundled with TeaVM, Gradle and Maven task, responsible for copying runtime, can also include this deobfuscator into output directory). |
| `externalInfoPath` | `<src>.teadbg` | Path to the external debug info file, or an already-loaded `Int8Array` of its bytes.                                                                                          |

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  stackDeobfuscator: {
    enabled: true
    // infoLocation defaults to "auto" — tries embedded, then external .teadbg
  }
});
```

When deobfuscation is active, exceptions thrown from Java code will have readable stack traces with class names, method names, and source line numbers.

If the deobfuscator Wasm or its data cannot be loaded, a warning is printed to the console and execution continues with raw addresses.

For production use, prefer to *disable* deobfuscation:

* it forces user to download extra files
* it can reveal internal code structure (which is not what you want, unless your project is open source).

If you need to collect error reports from users, you can take deobfuscator and, for example, run it in Node.js on the server, which collects crash reports.

### `memory`

```ts
interface MemoryOptions {
  external?: WebAssembly.Memory;
  minSize?:  number;
  maxSize?:  number;
  shared?:   boolean;
  onResize?: () => void;
}
```

Controls the linear `WebAssembly.Memory` used by the module.

Note that although TeaVM relies on Wasm GC objects, it still uses linear memory for representing direct NIO buffers and for transferring data from/to JS ArrayBuffer and typed arrays.

| Field | Description |
|---|---|
| `external` | Provide a pre-existing `WebAssembly.Memory`. Useful when sharing memory between the main thread and a worker. If omitted, a new memory is created automatically. |
| `minSize` | Minimum memory size in bytes. The runtime rounds up to the nearest Wasm page (64 KiB). |
| `maxSize` | Maximum memory size in bytes. Defaults to 2 GiB. |
| `shared` | Set to `true` to create a `SharedArrayBuffer`-backed memory (requires COOP/COEP HTTP headers). |
| `onResize` | Called whenever the heap grows. Useful for updating views into the memory buffer, since `SharedArrayBuffer`-backed memories do not invalidate existing `ArrayBuffer` views on growth, but plain memories do. |

```js
// Shared memory for communication between main thread and worker
const memory = new WebAssembly.Memory({ initial: 256, maximum: 256, shared: true });

const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  memory: {
    external: memory,
    shared: true
  }
});
```

### `stack`

```ts
stack?: number
```

Size of the C stack reserved for Emscripten interop modules, in bytes. Defaults to `2 MiB` (2 × 2²⁰). Ignored when no `emscriptenModules` are used.

### `emscriptenModules`

```ts
emscriptenModules?: Record<string, { pathToJs: string; pathToWasm: string }>
```

Links one or more native C libraries that were compiled with Emscripten in *relocatable* mode. The key is the import namespace name that the Java code uses; the value provides paths to the Emscripten-generated `.js` and `.wasm` files.

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  emscriptenModules: {
    "native": {
      pathToJs:   "./native-lib.js",
      pathToWasm: "wasm-gc/native-lib.wasm"
    }
  }
});
```

The TeaVM runtime handles memory layout, heap alignment, and the C stack automatically. See [Emscripten interop](#emscripten-interop) for the required compilation flags.


### `nodejs`

```ts
nodejs?: boolean
```

Forces Node.js mode: the loader reads `.wasm` files from the filesystem using `node:fs/promises` instead of `fetch()`. When running inside a Node.js process this is detected automatically, so you only need this flag if you are in an unusual environment where that heuristic fails.


### `noAutoImports`

```ts
noAutoImports?: boolean
```

By default the loader scans the Wasm module's import section for `externref`-typed globals whose module name looks like an ES module specifier (e.g. `"./utils.js"`) and automatically imports them with a dynamic `import()`. Set `noAutoImports: true` to disable this behaviour and supply all such imports yourself via `installImports`.

## Overriding console output

Java's `System.out` and `System.err` are routed through two functions in the `teavmConsole` import namespace:

| Function | Direction |
|---|---|
| `putcharStdout(charCode: number)` | One UTF-16 code unit from `System.out` |
| `putcharStderr(charCode: number)` | One UTF-16 code unit from `System.err` |

The default implementation buffers characters and flushes each line to `console.log` / `console.error` on newline (`\n`, code 10).

To redirect output — for example, to append it to a DOM element — replace these functions via `installImports`:

```js
const outputEl = document.getElementById("output");

const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  installImports(imports) {
    let buffer = "";
    const flush = (line) => {
      outputEl.textContent += line + "\n";
    };
    const putchar = (charCode) => {
      if (charCode === 10) { flush(buffer); buffer = ""; }
      else buffer += String.fromCharCode(charCode);
    };
    imports.teavmConsole.putcharStdout = putchar;
    imports.teavmConsole.putcharStderr = putchar;
  }
});
```

## Adding custom imports

If your Java code calls native methods backed by JavaScript (declared with `@Import`), those methods appear as Wasm imports. Provide implementations in `installImports` under the matching namespace:

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  installImports(imports) {
    imports.myLibrary = {
      fetchData: async (url) => { /* ... */ },
      timestamp: () => performance.now(),
    };
  }
});
```

The namespace name and function names must match what the Java code declares.

Note that `@Import` annotation is low-level and not recommended for regular use. Instead,
you most likely need [JSO API](/docs/runtime/jso.html) to interact with JS APIs.

## Error handling

Java exceptions that propagate past the Wasm boundary are rethrown as JavaScript `Error` objects. The `message` property of the error contains the Java exception message (obtained by calling `getMessage()` on the original exception).

```js
try {
  teavm.exports.riskyOperation();
} catch (e) {
  console.error("Java threw:", e.message);
}
```

With `stackDeobfuscator.enabled: true`, the `.stack` property contains the deobfuscated Java stack trace.

## Web Worker usage

Load the runtime with `importScripts` and call `TeaVM.wasmGC.load` exactly as in the main thread:

```js
// worker.js
importScripts("wasm-gc/app.wasm-runtime.js");

const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm");
teavm.exports.runInWorker();
```

For a module worker, use the ES module form — see *Advanced: ES module usage*.

## Shared memory (SharedArrayBuffer)

To share a `WebAssembly.Memory` between the main thread and a worker, create the memory on one side, pass its `SharedArrayBuffer` to the other, and reconstruct the `WebAssembly.Memory` there. Both sides pass the same `WebAssembly.Memory` via the `memory.external` option.

The page must be served with the COOP/COEP headers required for `SharedArrayBuffer`:

```
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Embedder-Policy: require-corp
```

```js
// main.js
const memory = new WebAssembly.Memory({ initial: 256, maximum: 256, shared: true });

const worker = new Worker("worker.js");
worker.postMessage({ sab: memory.buffer });

const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  memory: { external: memory, shared: true }
});
teavm.exports.main([]);
```

```js
// worker.js
importScripts("wasm-gc/app.wasm-runtime.js");

self.addEventListener("message", async ({ data }) => {
  const memory = new WebAssembly.Memory({
    initial: 256, maximum: 256, shared: true,
    ...{ buffer: data.sab }   // reconstruct from the transferred SAB
  });
  const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
    memory: { external: memory, shared: true }
  });
  teavm.exports.initWorker();
});
```

## Node.js usage

The loader detects a Node.js environment. You can also set `nodejs: true` explicitly. The `wasm-runtime.js` file should be loaded with `require()` or a dynamic `import()`.

```js
import { createRequire } from "module";
import { fileURLToPath } from "url";
import path from "path";

const require = createRequire(import.meta.url);
const __dirname = path.dirname(fileURLToPath(import.meta.url));

// Load the runtime (sets up global TeaVM.wasmGC)
require("./wasm-gc/app.wasm-runtime.js");

const teavm = await TeaVM.wasmGC.load(
  path.join(__dirname, "wasm-gc/app.wasm"),
  { nodejs: true }
);
teavm.exports.main([]);
```

Alternatively, if the runtime is published as an ES module package, import it directly:

```js
import { load } from "./wasm-gc/app.wasm-runtime.js";

const teavm = await load("./wasm-gc/app.wasm", { nodejs: true });
teavm.exports.main([]);
```

## Emscripten interop

Native C/C++ libraries compiled with Emscripten can be linked into a TeaVM Wasm GC module. The C code must be compiled with the following flags:

```
emcc \
  -s MODULARIZE \
  -s RELOCATABLE \
  -s EXPORT_ES6=1 \
  -s ALLOW_MEMORY_GROWTH=1 \
  -s EXPORTED_FUNCTIONS=_your_function,_malloc,_free,_realloc \
  -s STACK_OVERFLOW_CHECK=0 \
  -s MALLOC=none \
  --no-entry
```

Point the loader to the generated `.js` and `.wasm` files:

```js
const teavm = await TeaVM.wasmGC.load("wasm-gc/app.wasm", {
  emscriptenModules: {
    "native": {
      pathToJs:   "./native.js",
      pathToWasm: "wasm-gc/native.wasm"
    }
  }
});
teavm.exports.main([]);
```

The loader automatically handles memory layout (aligning the native heap after Java data), the C stack, and the function table. The key `"native"` becomes the Wasm import namespace for all functions exported from the native library.


## Advanced: ES module usage

When the runtime is consumed as an ES module (the module-wrapper build), the three functions are named exports instead of properties on `TeaVM.wasmGC`:

```js
import { load } from "./wasm-gc/app.wasm-runtime.js";

const teavm = await load("wasm-gc/app.wasm");
teavm.exports.main([]);
```

This also works inside a module `Worker`:

```js
// worker.mjs
import { load } from "./wasm-gc/app.wasm-runtime.js";

const teavm = await load("wasm-gc/app.wasm");
teavm.exports.runInWorker();
```
