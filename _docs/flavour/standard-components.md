---
title: Standard components
---


# Overview

All Flavour **components** are denoted as HTML tags or attributes named like this: `<prefix>:<component-name>`.
For example:

```html
<std:foreach var="fib" in="values">
  <li>
    <html:text value="fib"/>
  </li>
</std:foreach>
```

This fragment uses `std:foreach` and `html:text` component.
All components are grouped in **component packages**, denoted by prefix.
Flavour provides the number of standard component packages:

* [std](/docs/flavour/component-packages/std.html) &ndash; contains components that correspond to control flow statements of 
  programming languages, for example `std:foreach`, which renders its body multiple times and `std:if`,
  which renders its body if a certain condition holds.
* [attr](/docs/flavour/component-packages/attr.html) &ndash; contains components to declare attributes with dynamically computed values.
* [event](/docs/flavour/component-packages/event.html) &ndash; contains components to bind events to methods of the view class.
* [html](/docs/flavour/component-packages/html.html) &ndash; contains components to work with HTML-specific DOM.

Note that there's no magic behind these components,
they are built upon the same API you can use to create [custom components](custom-components.html).
If you want to learn standard components a little deeper, you can examine their 
[source code](https://github.com/konsoletyper/teavm-flavour/tree/master/templates/src/main/java/org/teavm/flavour/components).

