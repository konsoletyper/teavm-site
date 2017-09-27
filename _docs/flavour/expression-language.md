---
title: Expression language
---

# Basics

Flavour provides its own expression language, similar to EL in JSP or Spring Expression Language.
Unlike these, Flavour expression language is statically typed,
which means all type errors are reported during compile time.

Expression language (further referred as EL) is basically subset of Java expressions with some syntactical sugar.
EL consists of the following elements:

* Java identifiers, usually referring to methods, fields and lambda parameters.
  Examples: `foo`, `Bar`, `camelCaseName`, `CONSTANT_NAME`, `it`.
* String literals surrounded by single quotes. Examples: `'Some string'`, `''`, `First line\nSecond line`.
* Numeric literals. Examples: `23`, `3.14159`, `6.626e-34`.
* Standard literals: `null`, `this`, `true`, `false`.
* Arithmetic operators: `+`, `-`, `/`, `%`.
* Comparison operators: `>`, `<`, `>=`, `<=`, `==`, `!=`, `gt`, `gt`, `goe`, `lt`, `loe`.
  Note that since `<` and `>` may have special meaning in HTML, EL provides aliases to Java comparison operators.
* Logical operators: `&&`, `&&`, `!`, `and`, `or`, `not`.
  Note that since `&` may have special meaning in HTML, EL provides aliases to Java logical operators.
* Conditional operator: `<condition> ? <then> : <else>`.
* Invocation operator: `functionName(argument1, argument2, ...)`.
* Collection subscript operator: `collection[index1, index2, ...]`.
* Dot operator: `.`.
* `instanceof` operator.
* Cast operator: `(Type) expression`.
* Parentheses: `(expression)`.
* Lambda expressions.
* Assignment statement: `a = b`.


# `this` object

As you [already know](Introduction-to-templating#creating-a-new-page), page consists of HTML template and view class.
To refer to instance of view class from an HTML template, you can use `this` literal.
Like in Java, you are not forced to do it.
Instead of writing `this.foo()`, you can write simply `foo()`.


# Property access syntax

EL supports Java beans convention for properties.
Instead of writing `foo.getBar()` you can write `foo.bar` 
and instead of writing `foo.setBar(newValue)` your can write `foo.bar = newValue`.


# Collection access convention

In Java, subscript operator (`[]`) is used only to access arrays.
EL allows to use the similar syntax for anything that looks like collections.
If a value to the left from `[]` contains `get` method, it will be used.
Since both `List<T>` and `Map<T>` use method named `get` to access their elements,
it's possible to write something like:

```
Arrays.asList(2, 4, 8)[1]
```

instead of

```
Arrays.asList(2, 4, 8).get(1)
```


# Importing classes

EL does not allow to import classes directly.
It's a feature of templates.
Remember, templates are merely HTML pages with special elements.
You can use the following processing instruction to make a class or a package available in expressions:

```html
<?import my.pkg1.*?>
<?import my.pkg2.ClassName?>
```

For example:

```html
<?import java.util.Arrays?>
<std:foreach var="num" in="Arrays.asList(2, 3, 5, 7, 11)">
  <div>
    <html:text value="num"/>
  </div>
</std:foreach>
```

# Lambda expressions

Most of expressions you ever write in components are in fact lambda expressions.
So, for example, the following:

```html
<std:foreach var="employee" in="employees">
  ...
</std:foreach>
```

Is a shortcut of the following full form:

```html
<std:foreach var="employee" in="() -> employees">
  ...
</std:foreach>
```

Actually, `in` attribute accepts 
[Supplier&lt;T>](https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html),
so you should pass zero-parameter lambda there.

Lambdas may be written in the following forms:

```
(Type1 param1, Type param2, ...) -> expression
Type param -> expression
```

Where types are optional, in most cases EL compiler infers them.

For example:

```
employees.stream().filter(employee -> employee.age > 30).collect(Collectors.toList())
```

EL supports some shortcuts:

* When a directive expression accepts a functional type with zero parameter, you can omit `() ->` prefix.
* When a directive expression accepts a functional type with one argument, you can omit parameter declaration.
  In this case parameter is available via `it` identifier.
* When a directive expression accepts a functional type with one argument
  and your lambda is expected to assign value to property, you may omit ` = it` suffix.
  I.e. instead of writing `newValue -> foo.bar = newValue` you can write `foo.bar`.

The latter two shortcuts are useful for event handlers, for example:

```html
<input html:value="employee.firstName" html:change="employee.firstName"/>
```

Which is a shortcut for

```html
<input html:value="() -> employee.firstName" html:change="newValue -> employee.firstName = newValue"/>
```