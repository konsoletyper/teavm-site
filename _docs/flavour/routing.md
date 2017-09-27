---
title: Routing
---

# Creating route interface

The main purpose of routing library in Flavour is to provide a type-safe way to generate and parse URLs.
Thus, the first thing you have to do to start using routing is creating a **route interface**.

Let's create a simple one:

```java
@PathSet
public interface HelloRoute extends Route {
    @Path("/")
    void index();
    
    @Path("/hello/{name}")
    void hello(@PathParameter("name") String name);

    @Path("/goodbye")
    void goodbye();
}
```

Here, you can see all basic things:

* Route interface must extend `Route`.
* Route interface must be marked with `@PathSet`.
* Route interface can only define `void` methods.
* Each method must be marked with `@Path` annotation specifying path template.
* To bind parameter to template, you should use `@PathParameter` annotation.


# Creating application skeleton

Our route interface is useless until we start using it, but we don't have any views to show.
Let's create them:

```java
@BindTemplate("templates/master.html")
public class Client extends ApplicationTemplate implements HelloRoute {
    public static void main(String[] args) {
        Client client = new Client();
        new RouteBinder()
                .withDefault(HelloRoute.class, r -> r.index())
                .add(client)
                .update();

        client.bind("application-content");
    }

    @Override
    public void index() {
    }

    @Override
    public void hello(String name) {
    }

    @Override
    public void goodbye() {
    }
}
```

and corresponding template `templates/master.html`

```html
<div>
  <std:insert fragment="content"/>
</div>
```

Then create views and templates for pages:

```java
@BindTemplate("templates/index.html")
public class IndexView {
    private String name;

    public void setName(String name) {
        this.name = name;
    }
}
```

```html
<div>What's your name?</div>

<div>
  <input type="text" html:change="name"/>
  <button type="button">Hello</button>
</div>
```

```java
@BindTemplate("templates/hello.html")
public class HelloView {
    private String name;

    public HelloView(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

```html
<div>Hello, <html:text value="name"/></div>
```

```java
@BindTemplate("templates/goodbye.html")
public class GoodbyeView {
}
```

```html
<div>Goodbye!</div>
```


# Binding URL hashes to pages

Now we want to display pages as user changes URL in address bar.
It's easy, we already have bound URLs to actions, but left these actions empty.
Notice `index`, `hello` and `goodbye` methods of `Client` class, they don't do anything.
Flavour calls these methods automatically on every URL update.
Let's make these methods to do something useful:

```java
    @Override
    public void index() {
        setView(new IndexView());
    }

    @Override
    public void hello(String name) {
        setView(new HelloView(name));
    }

    @Override
    public void goodbye() {
        setView(new GoodbyeView());
    }
```

The magic behind `setView` is simple: our base class, `ApplicationTemplate` takes objects passed to `setView`, 
creates DOM fragments from them and supplies these fragments via `content` property,
which we just displayed in `master.html`.

You can run application and append `#/`, `#/hello/anonymous` and `#/goodbye` to test our new application.


# Generating links

There are two ways of generating links:

* insert them to the web page;
* generate them programmatically and manually update URL hash.

Let's update our application to use both ways.
First, add following code to `IndexView`:

```java
    public void sayHello() {
        Routing.open(HelloRoute.class).hello(name);
    }
```

and of course, bind this method to `click` event in `index.html`:

```html
  <button type="button" event:click="sayHello()">Hello</button>
```

Now, pressing the button on the first page of our application transfers user to corresponding page
*and* updates URL in browser's location bar.

Second, add the following code to `HelloView`:

```java
    public HelloRoute route(Consumer<String> consumer) {
        return Routing.build(HelloRoute.class, consumer);
    }
```

and the following markup to `hello.html`:

```html
<div>
  <a html:link="route(it).goodbye()">Goodbye!</a>
</div>
```

Now, hello page has a link to goodbye page.


# Advanced topics

## Path parameters
 
You can place path parameters everywhere, they are not constrained to be separated by `/` character.
I.e. the following pattern is valid:

```java
@Path("/hello-{firstName}-{lastName}")
```

Note that this pattern is ambiguous. For example, consider this input string:

```
hello-a-b-c
```

Both `firstName="a-b", secondName="c"` and `firstName="a", secondName="b-c"` are possible.
Flavour applies greedy algorithm, so the first option will be returned.

## Using regular expressions

For `String` path parameters it's possible to provide custom regular expressions.
All you need is to put `@Pattern` annotation on a corresponding method parameter.

```java
    @Path("/hello/{name}")
    void hello(@Pattern("[A-Za-z]+( +[A-Za-z]+)?") @PathParameter("name") String name);
```


This only affects parser, not generator.

## Mapping to Java types

Path parameters may have only following types:

* **String**.
* **byte**, **short**, **int**, **float**, **double**, either boxed and unboxed.
* **BigInteger**, **BigDecimal**.
* **java.util.Date**, 
  matches either date (`yyyy-MM-dd`) or timestamp (`yyyy-MM-ddThh:mm:ss`) format.
  Always produces timestamp format.
* Enum fields. Represented by their names.