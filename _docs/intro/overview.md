---
title: Overview
---

TeaVM is an ahead-of-time compiler of Java bytecode to JavaScript.
It's much like GWT, however GWT takes source code, and this limits GWT to Java only.
Unlike GWT, TeaVM relies on existing compilers, be it javac, kotlinc or scalac.
These compilers produce bytecode (`*.class` or `*.jar` files),
then TeaVM takes this bytecode and produces JavaScript code.


# Purpose

TeaVM is a primarily web development tool.
It's not for getting your large existing codebase in Java or Kotlin and producing JavaScript.
Unfortunately, Java was not designed to run efficiently in the browser.
There are Java APIs that are impossible to implement without generating unefficient JavaScript.
Some of these APIs are: reflection, resources, class loaders, JNI.
TeaVM restricts usages of these APIs.
Generally, you'll have to manually rewrite your code to fit into TeaVM constraints.

TeaVM is for you, if:

  * You are a Java developer and you are going to write web front-end from scratch.
  * You already have Java-based backend and want to integrate front-end code tightly into your existing development
    infrastructure.
  * You have some Java back-end code you want to reuse in front-end.  
  * You are ready to rewrite your code to work with TeaVM.

If you have bloated applications that use Swing, you want to run these applications in web,
and you don't care about download size, start-up time and performance, you better give up;
there are more appropriate tools for you, like [CheerpJ](https://www.leaningtech.com/cheerpj/).


# Strong parts

* TeaVM tries to reconstruct original structure of a method, 
  so in most cases it produces JavaScript that you would write manually.
  No bloated while/switch statements, as naive compilers often do.
* TeaVM has a very sophisticated optimizer, which knows a lot about your code. Some examples are:
  * Dead code elimination allows to produce very small JavaScript. 
  * Devirtualization turns virtual calls into static function calls, which makes code faster.
  * TeaVM can reuse one local variables to store several local variables.
  * TeaVM renames methods to as short forms as possible; UglifyJS usually can't perform such optimization.
* TeaVM supports threads. 
  JavaScript does not provide APIs to create threads 
  (WebWorkers are not threads, since they don't allow to share state between workers).
  TeaVM is capable of transforming methods to continuation-passing style.
  This makes possible to emulate multiple logical threads in one physical thread.
  TeaVM threads are, in fact, [green threads](https://en.wikipedia.org/wiki/Green_threads).
* TeaVM is very fast, you don't need to wait for minutes until application gets recompiled.
* TeaVM produces source maps; TeaVM IDEA plugin allows to [debug code right from the IDE](/docs/tooling/debugging.html). 
* TeaVM has a nice [JavaScript interop API](/docs/runtime/jso.html).


# Flavour framework  

TeaVM has a subproject, called Flavour.
Flavour is a framework for writing single-paged web applications.
It provides HTML template engine with data binding,
which is very similar to popular JavaScript frameworks like [Angular](https://angularjs.org/),
[React](https://facebook.github.io/react/), [Vue.js](https://vuejs.org/) and so forth.
Additionally, Flavour provides facilities to generate, parse human-readable URLs,
as well as binding them to the browser's address line via history API.

Another purpose of Flavour is: due to several reasons it's hard to support entire JDK in TeaVM,
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

Why using TeaVM while there are plenty of transpilers and frameworks for web front-end development?

If you are a JavaScript developer who is satisfied with JavaScript, TypeScript or even elm,
you probably won't need TeaVM.

If you are a Java (or Kotlin, or Scala) developer who used to write back-end code, TeaVM might be your choice.
It's true that a good developer (including Java developer) can learn JavaScript.
However, to become an expert you have to spend reasonable amount of your time.

Another drawback of JavaScript for you is it's a different language with different syntax, 
different build tools, different IDE experience.
Say, you have your Java/Maven build set up in Jenkins,
with Sonar Cube checking your code quality and IDEA code style settings.
You have to repeat all these things for the JavaScript ecosystem.
Finally, you have to "switch context" every time you change the code on back-end and front-end side.

TeaVM allows you to use single ecosystem, and reuse as much as possible of it for
both back-end and front-end worlds.
