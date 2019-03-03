---
title: REST client
---


The REST client module allows you to easily make AJAX queries to REST services.
It uses annotations from JAX-RS, which is a part of the Java EE specification.
There are several implementations of JAX-RS, like Jersey and CXF.
This means that you can write full-stack applications,
reusing JAX-RS declarations both to declare REST services and use them from client code.  
This saves time implemnting boilerplate code in the client to call services.  
It also enables IDE renaming refactorings to apply all the way from the server to the client, 
which reduces errors.
 

# Getting started

Let's start by creating a client for a server-side math library.
For now this library only supports addition of integer numbers.
Suppose someone has implemented a server that accepts GET queries at this address:

```
/api/math/integers/sum?a={number}&b={number}
```

and responds with integer number.

Let's create a definition for the service:

```java
@Path("math")
public interface MathService {
    @GET
    @Path("integers/sum")
    int sum(@QueryParam("a") int a, @QueryParam("b") int b);
}
```

Now we need to create an instance of this interface.
This is trivial:

```java
MathService math = RESTClient.factory(MathService.class).createResource("api");
```

Now you can use this instance like so:

```java
System.out.println(math.sum(2, 3));
```

By the way, if the author of the service implemented it in Java, it would be something like:

```java
public class MathServiceImpl implements MathService {
    @Override
    public int sum(int a, int b) {
        return a + b;
    }
}
```

so, the whole thing looks like just calling `MathServiceImpl.sum()`.
Writing JAX-RS services is out of scope this manual, and of scope of Flavour.
There are many manuals and tutorials available.
For example, you can read [this one](https://jersey.java.net/documentation/latest/getting-started.html).


# Supported JAX-RS subset

Flavour supports a reasonable subset of JAX-RS annotation.
It does not implement JAX-RS client API, instead Flavour provides its own simple API.

Here is the list of supported JAX-RS annotations:

* Method mapping
  * [@Path](https://docs.oracle.com/javaee/7/api/javax/ws/rs/Path.html)
  * [@Consumes](https://docs.oracle.com/javaee/7/api/javax/ws/rs/Consumes.html)
  * [@Produces](https://docs.oracle.com/javaee/7/api/javax/ws/rs/Produces.html)
* Parameter mapping
  * [@PathParam](https://docs.oracle.com/javaee/7/api/javax/ws/rs/PathParam.html)
  * [@QueryParam](https://docs.oracle.com/javaee/7/api/javax/ws/rs/QueryParam.html)
  * [@HeaderParam](https://docs.oracle.com/javaee/7/api/javax/ws/rs/HeaderParam.html)
  * [@BeanParam](https://docs.oracle.com/javaee/7/api/javax/ws/rs/BeanParam.html)
* HTTP methods
  * [@GET](https://docs.oracle.com/javaee/7/api/javax/ws/rs/GET.html)
  * [@PUT](https://docs.oracle.com/javaee/7/api/javax/ws/rs/PUT.html)
  * [@DELETE](https://docs.oracle.com/javaee/7/api/javax/ws/rs/DELETE.html)
  * [@POST](https://docs.oracle.com/javaee/7/api/javax/ws/rs/POST.html)
  * [@HEAD](https://docs.oracle.com/javaee/7/api/javax/ws/rs/HEAD.html)
  * [@OPTIONS](https://docs.oracle.com/javaee/7/api/javax/ws/rs/OPTIONS.html)

Currently, Flavour supports only JSON bodies.


# Client API

The REST client API has the following entry point:

```java
ResourceFactory<T> RESTClient.factory(Class<T> type)
```

`factory` accepts type of service interface.

By calling `ResourceFactory.createResource(String basePath)` you get instance of corresponding service.
`basePath` specifies location of a the service,
it's merely a string which will be used as a prefix to request address.

Additionally, you can call one of `ResourceFactory.add` methods to manually
process all requests and responses.
It can be useful in some use-cases, like adding security token to requests,
perform additional error processing of return statuses, and so forth.
