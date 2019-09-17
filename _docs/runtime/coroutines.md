---
title: Coroutines
---

Unlike C++ or Java, JavaScript does not support threads.
WebWorkers aren't threads, since they don't allow shared state between each other.
They are more like processes: they are isolated and can communicate via asynchronous channels.
There's also no synchronization primitives in WebWorkers.

However, Java relies on threads heavily.
There's synchronous `Thread.sleep` method, but no asynchronous alternative in standard library.
Most of classic IO is synchronous.
There are libraries which use threads.
And even if you write a new code, specially for TeaVM, it's still idiomatic for Java developer to create threads to perform several computations simultaneously.

Fortunately, TeaVM comes with solution for this.
TeaVM supports [coroutines](https://en.wikipedia.org/wiki/Coroutine).
Coroutines are special kinds of methods that can be suspended by runtime in a certain point and then resumed from this point.
In particular, TeaVM uses coroutines to emulate `Thread` class together with few simple synchronization primitives.

In most cases you don't need anything special to use coroutines.
You just create a new thread and start it.
You can use `synchronized` keyword, `Object.wait`, `Object.notify`, and so on.
The only case you should care of is interaction with JavaScript API.
The remaining part of this page describes some low-level details of TeaVM coroutines to help you to use them properly in some corner cases.


## `@Async` annotation

Most of JavaScript APIs are asynchronous, while Java often provides synchronous APIs.
For example, let's we have a JavaScript function `foo` that's been called like this:

```javascript
foo("some argument", function(result) {
    console.log(result);
})
```

We want to call it synchronously from Java.
In this case we need `@Async` annotation:

```java
@Async
public static native String foo(String arg);
private static void foo(String arg, AsyncCallback<String> callback) {
    fooAsync(arg, result -> callback.complete(result));
}

@JsBody(params = { "arg", "callback" }, script = "return foo(arg, callback);")
public static native void fooAsync(String arg, JsConsumer<String> callback);
```

So, to expose an asynchronous JavaScript API as a synchronous Java API, you need:

* to declare a native method;
* to mark this method with `@Async` annotation;
* to declare a second method in the same class with the same name, and almost same signature, except for the second method should return `void` and should take additional parameter of `AsyncCallback<T>` class (where `T` should correspond to return type of the first method);
* call either `callback.complete` or `callback.error` when operation completes.

The following example shows how to perform HTTP requests synchronously:


```java
public class Ajax {
    @Async
    public static native String get(String url) throws IOException;
    private static void get(String url, AsyncCallback<String> callback) {
        XMLHttpRequest xhr = XMLHttpRequest.create();
        xhr.open("get", url);
        xhr.setOnReadyStateChange(() -> {
            if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                return;
            }
            
            int statusGroup = xhr.getStatus() / 100;
            if (statusGroup != 2 && statusGroup != 3) {
                callback.error(new IOException("HTTP status: " + 
                        xhr.getStatus() + " " + xhr.getStatusText()));
            } else {
                callback.complete(xhr.getResponseText());
            }
        });
        xhr.send();
    }
}
```

## Interaction with JavaScript.

Sometimes JavaScript API expects you to pass function.
For example, `Array.prototype.map` expects mapping function.
This API expects passed function to complete immediately and produce
result.
However, when passing Java lambda to such function,
this lambda may be a coroutine.
To prevent this, TeaVM prohibits to pass coroutine to a JavaScript function.
See following example:

```java
@JsBody(params = { "array", "mapper" }, body = "return array.map(mapper);")
static native <T, S> JsArray<S> map(JsArray<T> array, JsMapper<T, S> mapper);

static JsArray<String> getAll(JsArray<String> urls) {
    map(urls, url -> {  // <-- TeaVM throws exception here
        return Ajax.get(url);
    });
}
```
