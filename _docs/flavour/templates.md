---
title: HTML templates
---

Flavour provides convenient way to render and update DOM via HTML templates.
All you need is to define class with data and behaviour and bind it to HTML template.
This section describes following topics:

* how to create HTML templates;
* how to use standard components;
* how to create custom components;
* how to use expression language.


# Creating a new page

To create a new page you need two things:

1. **View class** that describes data and behaviour of the page.
2. **HTML template** that displays data.

Flavour does not force you with a certain structure of the view class.
It can extend, implement anything you want, define any methods and fields, and so forth.
The only thing you need is to mark this class with 
[@BindTemplate](https://github.com/konsoletyper/teavm-flavour/blob/master/templates/src/main/java/org/teavm/flavour/templates/BindTemplate.java) 
annotation which specifies relative path to template.
The template is an HTML file that must be somewhere in project's resources (i.e. `src/main/resources` by convention).
Additionally to regular HTML elements, template file may contain some extended elements provided by Flavour.

See this example:

```java
@BindTemplate("templates/fibonacci.html")
public class Fibonacci {
    private List<Integer> values = new ArrayList<>();

    public Fibonacci() {
        values.add(0);
        values.add(1);
    }

    public List<Integer> getValues() {
        return values;
    }

    public void next() {
        values.add(values.get(values.size() - 2) + values.get(values.size() - 1));
    }
}
```

```html
<ul>
  <std:foreach var="fib" in="values">
    <li>
      <html:text value="fib"/>
    </li>
  </std:foreach>
  <li>
    <button type="button" event:click="next()">Show next</button>
  </li>
</ul>
```

This example assumes you put template to `src/main/resources/templates/fibonacci.html`.

This code does nothing.
First, you should tell Flavour to render template.
Add the following method to the `Fibonacci` class:

```java
public static void main(String[] args) {
    Templates.bind(new Fibonacci(), "application-content");
}
```

Of course, your master `index.html` should contain element with `application-content` identifier.


# Interacting with template engine

The main entry point to template engine is 
[Templates](https://github.com/konsoletyper/teavm-flavour/blob/master/templates/src/main/java/org/teavm/flavour/templates/Templates.java) class.
For basic usage you need only these methods:

* `Templates.bind(Object, HTMLElement|String)` which binds instance of view class to the element of static HTML,
  specified either as `HTMLElement` instance or by id.
* `Templates.update()` forces updating of all bound templates.
  Usually, Flavour is smart enough to automatically update DOM.
  However, sometimes it does not know enough about your code, 
  so you have to use this method to tell template engine to update explicitly.
  This includes situations when you work with timers.
  Remember, it's not very expensive to update DOM, 
  since Flavour performs dirty check and performs only as few DOM operations as possible.
* `Templates.create()` creates `Fragment` instance from given view object.

There are some other methods, but they are intended for advanced usage, primarily for creating custom components