# How you can help

First, there is a list of 
[roadmap issues](https://github.com/konsoletyper/teavm/issues?q=is%3Aissue+is%3Aopen+label%3Aroadmap) on GitHub.
Take the list, and if you are interested, please post comments and let know that you are ready to work
on a particular issue.
Sometimes there's only very brief description, so you should discuss how and what exactly should be implemented.

Second, you can find some issues yourself.
In this case, add an issue on GitHub and post a comment that you want to fix the issue yourself.

There's always lack of documentation, so you can contribute with additional sections, that describe
some aspect in detail, fix spelling, add examples and tutorials.
Anyway, open issue first so that communication with you could start.

There's always lack of Java class library classes and wrappers for JavaScript objects.


# Your motivation to help

Perhaps, you are using TeaVM in your pet project, or even sell product that uses TeaVM internally.
You may want some features, that is not high prioritized.
Of course, you can always [donate](https://github.com/sponsors/konsoletyper), so that someone implements
the issue for you. 
Alternative approach is to implement issue yourself. This will not satisfy your needs,
but make you familiar with TeaVM internals, so you can be sure that if author abandons project,
you'll have a chance to pick it up.

Perhaps, you don't use TeaVM at all, but you are looking for a way to improve your experience with
compiler engineering, technical writing or IT evangelism. TeaVM is a good way to do it.
With such projects in portfolio you increase your chance to be hired by a better employer.
For example, TeaVM author was hired by [JetBrains](https://www.jetbrains.com/) thanks to the project.
TeaVM author also written few blog posts and participated as a speaker in [Joker](https://jokerconf.com/en/)
IT conference.
If you are looking for an interesting topic for you blog or talk, TeaVM is a good choice.

Contributing to TeaVM can bring you money, not much however. Please, contact me (username konsoletyper) in
project Gitter and we can discuss terms personally.
Current contributions to project don't allow to pay even small part of an amount that you would deserve, 
but in addition to other ones, this is a good reason.


# How to start with TeaVM code base

There are two repositories:

* [main TeaVM source code repository](https://github.com/konsoletyper/teavm) 
* [documentation repository](https://github.com/konsoletyper/teavm-site) (this site).

Both are built with Gradle. You don't need local Gradle installation, Gradle wrapper is included.


# Verifying your contribution

In case you contribute to the source code, you must follow code conventions (which is mostly derived from
SUN code conventions). 
To make sure code satisfies code conventions, you can run following:

```
./gradlew build -x test -x javadoc --stacktrace
```

This will run compilation, verification and other build-related tasks.


# Testing your code

Before creating a PR, make sure you added tests. For class library, tests are usually located in `tests` module.
There are some tricks and tips about these tests, see [Testing](/docs/contributing/testing.html) section for full description