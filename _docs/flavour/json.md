---
title: JSON
---


# Getting started

Flavour provides ability to serialize and deserialize Java objects to and from JSON.
Java types system does not map to JSON clearly, so additional conventions and annotations are required.
[Jackson](https://github.com/FasterXML/jackson) is a de-facto standard for Java-to-JSON serialization,
hence Flavour supports reasonable subset of Jackson annotations.
This helps to achieve good adoption for developers who already use Java on server and
familiar with Jackson.
But most of all, the goal of implementing subset of Jackson was to allow developers
reuse annotated classes both on client and server side.

The main use-case for JSON serialization is communication to server.
In this case Flavour has [built-in solution](/docs/flavour/rest-client.html),
so you don't have to deal with JSON API directly.
But there are some cases when you want to call JSON API.
The following example shows how to use it:

```java
    MyClass a = new MyClass();
    a.foo = "hello";
    a.bar = 23;
    String json = JSON.serialize(a).stringify();
    System.out.println(json);

    MyClass b = JSON.deserialize(Node.parse(json), MyClass.class);
    System.out.println(b.foo + "," + b.bar);
```

where MyClass is defined like this:

```java
public class MyClass {
    public String foo;
    public int bar;
}
```

this code must be self-descriptive.
You can find that main entry points to JSON API are two methods from `JSON` class:
`serialize` and `deserialize`.
Both operate on JSON nodes represented by `Node` class,
so some additional code required to convert to and from string.


# Supported subset of Jackson

To learn Jackson annotations, you can read its
[official documentation](https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations).
Here is the list of supported annotations:

* [@JsonProperty](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonProperty.html)
* [@JsonIgnore](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonIgnore.html)
* [@JsonIgnoreProperties](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonIgnoreProperties.html)
* [@JsonAutoDetect](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonAutoDetect.html)
* [@JsonCreator](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonCreator.html)
* [@JsonTypeInfo](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonTypeInfo.html)
* [@JsonTypeName](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonTypeName.html)
* [@JsonSubTypes](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonSubTypes.html)
* [@JsonIdentityInfo](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonIdentityInfo.html)
  (without `resolver` and `scope` fields)
* [@JsonFormat](http://fasterxml.github.io/jackson-annotations/javadoc/2.2.0/com/fasterxml/jackson/annotation/JsonFormat.html)
  (only for `Date` properties, only when `shape = Shape.STRING`)

Note that TeaVM optimizer often remove classes which are unreachable from anywhere.
For example, it removes a class if it is never instantiated from reachable methods.
In case of inheritance, when a subclass can only be instantiated by deserializer,
TeaVM may remove the class.
To prevent TeaVM from removing classes, you should always use `@JsonSubTypes`
when you want polymorphic deserialization.