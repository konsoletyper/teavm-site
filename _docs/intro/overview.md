---
title: Overview
---

# What is Flavour

Flavour is a framework for writing single-paged web applications.
It's based on [TeaVM](https://github.com/konsoletyper/teavm), compiler of JVM bytecode to JavaScript.
TeaVM itself is a compiler which is capable of executing JAR files in the browser, and nothing more.
You can't write web applications using TeaVM, because Java standard library knows nothing about DOM and data binding.
Flavour closes this gap with HTML templating library with data binding,
which is very similar to popular JavaScript frameworks like [Angular](https://angularjs.org/),
[React](https://facebook.github.io/react/), [Vue.js](https://vuejs.org/) and so forth.
Additionally, Flavour provides library to generate, parse human-readable URLs,
as well as binding them to the browser's address line via history API.

Another reasons is: due to several reasons it's hard to support entire JDK in TeaVM,
especially things like reflection, class loading, resources, threads,
thus most Java libraries for data serialization and network communication are unavailable in TeaVM.
Flavour reimplements reasonable subsets of [Jackson](https://github.com/FasterXML/jackson) and 
[JAX-RS](https://jax-rs-spec.java.net/) client without a single line of reflection.

Flavour *is not* a server-side framework.
You are supposed to write your back-end code using "normal" JDK like OpenJDK,
Oracle JDK using your favourite framework like Spring, Java EE, Vert.x, etc.
Flavour is a client-side framework that you can use together with your back-end code.
To make this easier, Flavour tries to be as close to usual Java developers as possible.


# Motivation

Why using Flavour while there are plenty of frameworks?

If you are a JavaScript developer who is satisfied with JavaScript, TypeScript or even elm,
you probably won't need Flavour.

If you are a Java (or Kotlin) developer who used to write back-end code, TeaVM might be your choice.
It's true that a good developer (including Java developer) can learn JavaScript.
However, to become an expert you have to spend reasonable amount of your time.

Another drawback of JavaScript for you is it's a different language with different syntax, 
different build tools, different IDE experience.
Say, you have your Java/Maven build set up in Jenkins,
with Sonar Cube checking your code quality and IDEA code style settings.
You have to repeat all these things for the JavaScript ecosystem.
Finally, you have to "switch context" every time you change the code on back-end and front-end side.

Flavour allows you to use single ecosystem, and reuse as much as possible of it for
both back-end and front-end worlds.

GWT serves the similar purpose.
However, TeaVM has some advantages over GWT:

* It supports Kotlin and (probably) Scala, so you can use these languages with Flavour.
* It's faster and produces faster code.
* It supports more JDK than GWT. With TeaVM you can even start threads (which are emulated by coroutines).
  Forget about generating asynchronous interfaces for RPC!
* Flavour is more convenient and friendly to HTML concepts.
  Widget-based approach used by GWT turned out to be a bad idea.
  Use Flavour and be as happy as Angualar developers are!
* Flavour tries to follow standards.
  While GWT invents its own serialization format and its own RPC, Flavour just reuses widely used Jackson and JAX-RS.