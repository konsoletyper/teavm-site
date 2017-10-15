---
title: DukeScript
---

[DukeScript](http://dukescript.com) is a set of **Java APIs** that allow
development of cross device **Java** or **Kotlin** applications. The applications run on
[desktop](https://github.com/dukescript/dukescript-presenters#the-webkit-presenter),
[Android](https://dukescript.com/best/practices/2017/06/11/AndroidStudio.html),
[iOS](https://dukescript.com/javadoc/presenters/com/dukescript/presenters/iOS.html)
using *the best JVM* available for given platform. In addition to that the code
can be transpiled to JavaScript by **TeaVM**
(using [teavm-html4j](https://github.com/konsoletyper/teavm/tree/d4903d460bc8667bd3318c9fb21390eddb2799b3/html4j) plugin)
or by [Bck2Brwsr](http://wiki.apidesign.org/wiki/Bck2Brwsr) VM.

[DukeScript](http://dukescript.com) usually display its
*UI using WebView* and thus major part of [its API](http://bits.netbeans.org/html+java/)
is focused on effective, portable and cross platform
[Java to JavaScript interaction](https://github.com/apache/incubator-netbeans-html4j#readme).
This part of the system has been recently
[donated to Apache Foundation](https://github.com/apache/incubator-netbeans-html4j).
