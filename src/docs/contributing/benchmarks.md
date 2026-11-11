TeaVM has two sets of benchmarks:

* `benchmarks` module measures performance of code *produced* by TeaVM;
* `compiler-benchmarks` module measures performance of the TeaVM compiler itself.


# Benchmarks of generated code

Benchmarks are ordinary [JMH](https://github.com/openjdk/jmh) benchmarks.
TeaVM's own tool (`tools/perf` module) compiles each benchmark with TeaVM and runs it in a browser
(JavaScript, WebAssembly GC) or as a native executable (C).
The same benchmarks can be run with JMH on JVM, so results can be compared.

To run benchmarks:

```shell
./gradlew :benchmarks:teavmBenchmark
```

Reports are written to `benchmarks/build/reports/teavm-perf`, including a JMH-like text table,
JSON results in JMH format, an HTML report and a Markdown table convenient to paste into pull requests.

Benchmarks are configured by `benchmark.*` Gradle properties, for example:

```shell
./gradlew :benchmarks:teavmBenchmark -Pbenchmark.backends=js,wasm-gc,c -Pbenchmark.compareJvm=true \
    -Pbenchmark.include=StringSplit,Pi
```

Instead of passing them every time, copy `benchmarks/gradle-local-template.properties` to
`benchmarks/gradle-local.properties` (ignored by git) and uncomment the properties you need.
The template lists all supported properties with descriptions.


## Measuring effect of a change

There are two scripts that run benchmarks twice and produce a report that compares results:

* `benchmarks/compare-stash.sh [gradle args...]` compares uncommitted changes with `HEAD`;
* `benchmarks/compare-commit.sh <commit> [gradle args...]` compares the current working tree with
  the given commit.

For example:

```shell
benchmarks/compare-commit.sh HEAD~1 -Pbenchmark.backends=js,wasm-gc -Pbenchmark.include=String
```

Note that benchmarks take time, and results are only reliable when nothing else consumes CPU.
Use `benchmark.include` to select benchmarks that are relevant to your change.


## Writing benchmarks

Put benchmarks into `benchmarks/src/main/java`.
They are written with JMH annotations, but only a subset of JMH is supported
(`@Benchmark`, `@State`, `@Param`, `@Setup`/`@TearDown` with trial and iteration levels,
`@BenchmarkMode`, `@Warmup`, `@Measurement`, `@Fork`, `Blackhole` and some others).
Keep in mind that the code is compiled by TeaVM, so it can only use APIs supported by TeaVM class library.

See [README](https://github.com/konsoletyper/teavm/blob/master/benchmarks/README.md) for the full list
of supported features, settings, profiling and other details.


# Benchmarks of the compiler

`compiler-benchmarks` module contains JMH benchmarks that run TeaVM compiler on JVM.
Use them when you change the compiler and want to make sure it does not become slower or consume more memory.

```shell
./gradlew :compiler-benchmarks:jmh
```

Settings are configured in the same way, see `compiler-benchmarks/gradle-local-template.properties`
for the list of properties, including how to save a baseline and compare with it.
