---
title: Components advanced topics
---

# Making attributes optional

To make an attribute of an element component optional,
you may add `@OptionalBinding` annotation to the corresponding setter method.
For example:

```java
    @BindAttribute(name = "name")
    @OptionalBinding
    public void setNameSupplier(Supplier<String> nameSupplier) {
        this.nameSupplier = nameSupplier;
    }
```


# Exposing variables to the body

A component may compute a value and make it available to its body (i.e. a fragment bound to `@BindContent`).
You should use `@BindAttribute` for this purpose.
Unlike parameters, which are represented by setter methods with one parameter,
variable methods must take zero parameters and return the actual value of a variable.
For example, look at the [std:with](std-component-package#with) implementation:

```java
    @BindAttribute(name = "var")
    public T getVariable() {
        return variable;
    }

    @BindAttribute(name = "value")
    public void setValue(Supplier<T> value) {
        this.value = value;
    }

    @Override
    public void render() {
        if (contentRenderer == null) {
            contentRenderer = content.create();
            getSlot().append(contentRenderer.getSlot());
        }
        variable = value.get();
        contentRenderer.render();
    }
```


# Inner components

Sometimes there is a component that can't function alone.
For example, [std:choose](/docs/flavour/component-packages/std.html#choose) is implemented as
one parent component and a child component for each clause.
It's possible to create inner components by applying `@BindElement` to methods.
See this excerpt from the `std:choose` implementation:

```java
    @BindElement(name = "option")
    public void setClauses(List<ChooseClause> clauses) {
        this.clauses = clauses;
    }

    @BindElement(name = "otherwise")
    @OptionalBinding
    public void setOtherwiseClause(OtherwiseClause otherwiseClause) {
        this.otherwiseClause = otherwiseClause;
    }
```


# Components with multiple names

It's possible to bind more than one name to a component.
First, note that both `@BindAttributeComponent` and `@BindElement` accept arrays in their `name` attribute.
Second, names can be finished by `*` character, which means that given element or attribute name
may be followed by any character sequence.

To get the actual name of element or attribute used to bind component to DOM,
create a method which takes single `String` parameter and mark it with `@BindElementName`.
Example:

```java
@BindElement(name = { "foo", "bar", "baz-*" })
class MyComponent extends AbstractComponent {
    private String actualName;
    
    @BindElementName
    public void setActualName(String actualName) {
        this.actualName = actualName;
    }
}
```


# Passing multiple parameters to attribute component

Component bound to attribute is only limited to take one parameter.
However, it's sometimes necessary to pass multiple parameters.
In this case you need an intermediate class that holds values of these parameters.
This class must be marked with `@SettingsObject` and define a setter for each parameter.

To use such component, you should use JSON-like syntax to pass parameter values.

See the example:

```java
@BindAttributeComponent(name = "my-component")
public class MyComponent implements Renderable {
    private ModifierTarget target;
    private Supplier<MyOptions> optionsSupplier;

    public MyComponent(ModifierTarget target) {
        this.target = target;
    }

    @BindContent
    public void setOptionsSupplier(Supplier<String> optionsSupplier) {
        this.optionsSupplier = optionsSupplier;
    }

    @Override
    public void render() {
        target.getElement().setAttribute("class", 
                optionsSupplier.get().getFirst() + optionsSupplier.get().getSecond());
    }

    @Override
    public void destroy() {
    }
}
```

```java
@SettingsObject
public class MyOptions {
    private String first;
    private String second;
    
    public String getFirst() {
        return first;
    }
    
    public void setFirst(String first) {
        this.first = first;
    }
    
    public String getSecond() {
        return second;
    }
    
    public void setSecond(String second) {
        this.second = second;
    }
}
```

which can be used as follows:

```html
<div prefix:my-component="{ first: 'foo', second: 'bar' }">Nothing useful here</div>
```


# Bidirectional binding

You can emulate bidirectional binding by mapping same attribute or attribute content 
to two different component properties.
Due to expression shortcuts (i.e. `param -> foo = param` and `() -> foo` both have a shorter form `foo`),
Flavour will handle this case properly.
Example:

```java
@BindAttributeComponent(name = "bidir-binding")
public class BidirExampleComponent implements Renderable {
    private ModifierTarget target;
    private Supplier<String> fooSupplier;
    private Consumer<String> fooConsumer;

    public BidirExampleComponent(ModifierTarget target) {
        this.target = target;
    }

    @BindContent
    public void setFooSupplier(Supplier<String> fooSupplier) {
        this.fooSupplier = fooSupplier;
    }
    
    @BindContent
    public void setFooConsumer(Consumer<String> fooConsumer) {
        this.fooConsumer = fooConsumer;
    }

    @Override
    public void render() {
        // Use fooSupplier and fooConsumer here
    }

    @Override
    public void destroy() {
    }
}
```

```html
<div prefix:bidir-binding="myProperty">Nothing useful here</div>
```
