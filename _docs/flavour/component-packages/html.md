---
title: Standard components - html package
exclude_from_nav: true
---

This package includes following components:

* [text](#text)
* [value](#value)
* [enabled](#enabled)
* [change](#change)
* [link](#link)


# text

Evaluates expression and inserts as text.

Syntax:

```html
<html:text value="expression"/>
```

Where:

* `value="expression"` is an expression which will be evaluted.

Example:

```html
<h1>Employee #<html:text value="employee.id"/></h1>

<ul>
  <li><strong>First name</strong>: <html:text value="employee.firstName"/></li>
  <li><strong>Last name</strong>: <html:text value="employee.lastName"/></li>
  <li><strong>Salary</strong>: $<html:text value="employee.salary"/></li>
</ul>
```


# value

Binds value of input element to expression.

Syntax:

```
html:value="expression"
```

Where:

* `expression` is an expression that's been evaluated.

Example:

```html
<form>
  <div class="form-label"><label for="employee-first-name">First name</label></div>
  <div class="form-input"><input id="employee-first-name" html:value="employee.firstName"/></div>
  
  <div class="form-label"><label for="employee-first-name">Last name</label></div>
  <div class="form-input"><input id="employee-first-name" html:value="employee.lastName"/></div>
</form>
```


# enabled

Binds enabled state of input element to boolean expression.

Syntax:

```
html:enabled="expression"
```

Where:

* `expression` is a boolean expression which specifies whether element must be enabled.

Example:

```html
<div>
  Search: <input html:value="searchString"/> <button html:enabled="not searchString.empty">Go!</button>
</div>
```


# change

Sets change event handler.
This is more flexible than [event:change](/docs/flavour/component-packages/event.html), since, unlike `event:change`,
`html:change` uses Flavour change tracking mechanism, not native DOM.

Syntax:

```
html:change="expression"
```

Where:

* `expression` is a lambda expression which runs as change event is triggered.

Example:

```html
<div class="form-label"><label for="employee-first-name">First name</label></div>
<div class="form-input">
  <input id="employee-first-name" html:value="employee.firstName" html:change="employee.firstName = it"/>
</div>
```


# link

Sets link target.

Syntax:

```html
<a html:link="expression">...</a>
```

Where:

* `expression` is a lambda expression that is run to compute hyperlink target.
  This lambda takes `Comsumer<String>` as a single parameter and must call `apply()` method.
  Such strange API is required for integration with routing library.
  If you want to generate target manually, you better use [attr:href](/docs/flavour/component-packages/attr.html).

Example:

```html
<a html:link="it.apply('#employees/' + employee.id)">
  <html:text value="employee.firstName"/> <html:text value="employee.lastName"/>
</a>
```
