# Compile time errors

Please note, that TeaVM tries its best to approximate call stacks for each point where it finds an error.
This is done for purpose: an error may exist in some library, that is used by another library, and so on,
that is used by your code. TeaVM just tries to help you with locating the reason.

Please, don't confuse these stack traces with actual runtime stack traces.


## Compiler reports '[element] was not found'

Element here can be 'class', 'field' or 'method'.

TeaVM is an AOT compiler, so it can handle some errors that in JVM occur at runtime, at compile time.
This is the case. This means exactly: a method was found that references a method or class or field
that does not exist. Usually, this happens on broken classpath: some library is missing, or
it has wrong version, incompatible with your code.

With TeaVM there's another possible reason: TeaVM implements only limited subset of Java class library.
If your code of one of libraries you use try to use parts of Java class library not emulated by TeaVM,
you end up with this error.

Example:

```
Class java.util.concurrent.locks.ReentrantLock was not found
    at org.teavm.TestClass.run(TestClass.java:61)
    at org.teavm.TestClass.main(TestClass.java:25)
```

## Compiler reports 'Method ... is async, but async support is disabled'

Example:

```
Method java.lang.Thread.sleep(J)V is async, but async support is disabled. Either avoid calling this method
or enable async support (asyncEnabled option). Note that async support has many quirks and has negative impact
on the performance of the whole application.
    at org.example.Client.poll(Client.java:42)
    at org.example.Client.main(Client.java:15)
```

Since version 0.17, support for coroutines (which TeaVM uses to emulate threads and blocking APIs,
like `Thread.sleep`, `Object.wait`, `Thread.join` or blocking queues) is disabled by default.
TeaVM reports this error for each such method, reachable from your code.
Use the reported call stack to find out how your code (or a library you use) reaches this method.

There are two ways to fix this:

* avoid calling the blocking API, for example by rewriting the code using callbacks or JavaScript promises
  (recommended);
* enable async support with `asyncEnabled` option.

Before choosing the second option, please, read about the [cost of coroutines](/docs/runtime/coroutines.html):
it affects size and performance of the whole application, not only of the code that calls blocking methods.
