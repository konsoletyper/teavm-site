TeaVM does not support JNI. Instead, it comes with its own set of annotations and built-in classes
to describe native libraries for Java code.

Note that the API described here *does not* allow you to define how to link external libraries.
It only generates C code and assumes that you do everything necessary to set up your linker
to link with these libraries.


# Defining native functions

To define a native function, you should define a static native method and annotate it with `@Import` annotation.
Additionally, you can supply which include file contains these function by adding `@Include` annotation.
For example:

```java
@Include("string.h")
@Import(name = "memcpy")
public static native void memcpy(Address target, Address source, long size);
```

Note that you can also put `@Include` annotation on class. This allows to define libraries,
where you put single `@Include` annotation on class and just define set of static native methods with 
`@Import` annotation on them.

Java integer primitives *are not* mapped to `int`, `long`, etc. in C code. Instead, they are
mapped to `int32_t`, `int_64t` and so on.


# Working with native memory

To work with memory, you can use `Address` class. Just use it in signatures of native methods
definition to represent pointers (usually, `void*`, but in C there's not strong typing rules applied to pointer types).

You can:

* do pointer arithmetics, see `add`/`isLessThan`
* read/write memory: see `get*` and `put*` methods
* cast to/from integer values: `toInt`/`toLong`/`fromInt`/`fromLong`

Note that `Address` class is *special*. Upcasting it to `Object`, calling `equals`/`hashCode`/`toString` 
and so on will produce unpredictable result, often causing runtime error.

You also should not declare arrays of `Address`;


# Defining structures

To interact with native structures, you can extend `Structure` class.
In your subclass define public fields that correspond to fields in C structure.
For example:

```java
public class Tm extends Structure {
    public int sec;
    public int min;
    public int hour;
    public int mday;
    public int mon;
    public int year;
    public int wday;
    public int yday;
    public int isdst;
}

public class Time extends Structure {
}

@Include(value = "time.h", isSystem = true)
public class TimeLib  {
    @Import(name = "localtime")
    public static native Tm localtime(Time timep);
}
```

And use it like this

```java
var tm = TimeLib.localtime(time);
System.out.println(tm.hour + ":" + tm.min + ":" + tm.sec);
```

*You can't map to Java a structure passed by value!* In case you need, consider creating an adapter in C.

With structure you can:

* do some pointer arithmetics, see `add` method
* cast to/from `Address`, see `toAddress`
* cast to other structure types
* read/write fields of a structure

Note that `Structure` subclasses are special like `Address`. 
Don't use them in `Object` context, don't define arrays of structures.


# Defining pointers to functions

To map a pointer to function, declare an abstract subclass of `Function` class.
Define exactly one public abstract method with the desired signature.

For example:

```java
@Include(value = "signal.h", isSystem = true)
public class Signal {
    public static abstract class Handler extends Function {
        public abstract void handle(int signum);
    }
    
    @Import(name = "signal")
    public static native Handler signal(int signum, Handler handler);
}
```

To take address of static Java method and turn it into pointer to function,
use `Function.get`. Note that you only allowed to pass literals to this method.

For example:

```java
public class Example {
    public static void handle(int number) {
        System.out.println("Handled signal " + number);
    }

    public static void example() {
        var handler = Function.get(Signal.Handler.class, Example.class, "handle");
        Signal.signal(2 /* SIGINT */, handler);
    }
}
```


# Taking address of arrays and buffers

`Address` class provides set of `ofData` methods for all Java array types
and NIO buffers. Use it with care!

* When you pass address of array data to C code, make sure that this C code
  never writes outside of array, otherwise heap will be corrupted and
  GC will crash.
* GC relocates objects, including arrays. This means that you should not
  pass address of array data to a C function that stores it and fill eventually.
  Make sure that this C function reads/write the array immediately and then
  forgets the address.
* Due to relocation, you should also make sure that you never store 
  pointers to arrays in Java code.

If you need to have non-relocatable storage, consider mapping `malloc`/`free` functions.
Additionally, you can allocate direct NIO buffers - they are allocated off heap
and will never be relocated by the GC. One thing to notice: direct NIO buffers
don't have an API to free memory, which means extensive allocation of direct NIO
buffers from Java can cause memory leaks, since GC frees these NIO buffers
at unpredictable moments.


# Allocating structures in Java

Java does not allow to specify structures by value. That means that you can't
just write

```java
MyStruct m;
```

to allocate instance of `MyStruct`, like you would in C.
To solve this, you can either use `malloc` and `free` to allocate `MyStruct`
on C heap, or use following trick, if you have only short-living structure:

```java
var storage = new byte[Structure.sizeOf(MyStruct.class)];
MyStruct m = Array.ofData(storage).toStructure();
m.foo = 23;
passStructToNativeLib(m);
Address.pin(storage);
```

You need `Address.pin` to make sure that between actual allocation in the first line
and the call to native function `storage` is never released.