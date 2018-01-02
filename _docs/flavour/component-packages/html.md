---
title: Standard components - html package
exclude_from_nav: true
---

This package includes following components:

* [text](#text)
* [value](#value)
* [checked](#checked)
* [enabled](#enabled)
* [change](#change)
* [checked-change](#checked-change)
* [bidir-value](#change)
* [bidir-checked](#change)
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

# checked

Binds checked attribute of input element (with `type="checkbox"`) to expression.

Syntax:

```
html:checked="expression"
```

Where:

* `expression` is an expression that's been evaluated.

Example:

```html
<form>
  <div class="form-label"><label for="employee-coffee">Free coffee</label></div>
  <div class="form-input"><input type="checkbox" id="employee-coffee" html:checked="employee.receivingFreeCooffee"/></div>
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


# checked-change

Sets event handler for changing `checked` attribute.

Syntax:

```
html:checked-change="expression"
```

Where:

* `expression` is a lambda expression which runs as change event is triggered.

Example:

```html
<div class="form-label"><label for="employee-free-coffee">First name</label></div>
<div class="form-input">
  <input type="checkbox" id="employee-free-coffee" html:checked="employee.receivingFreeCoffee" 
         html:checked-change="employee.receivingFreeCoffee"/>
</div>
```

# bidir-value

Acts as both [value](#value) and and [change](#change).
Used to establish bidirectional binding between property and `input` element.

Syntax:

```
html:bidir-value="expression.propertyName"
```

Where:

* `expression` is a property owner.
* `property` is a property name.

Example:

```html
<div class="form-label"><label for="employee-first-name">First name</label></div>
<div class="form-input">
  <input id="employee-first-name" html:bidir-value="employee.firstName"/>
</div>
```


# bidir-checked

Acts as both [checked](#checked) and and [checked-change](#checked-change).
Used to establish bidirectional binding between boolean property and `input type="checkbox""` element.

Syntax:

```
html:bidir-checked="expression.propertyName"
```

Where:

* `expression` is a property owner.
* `property` is a property name.

Example:

```html
<div class="form-label"><label for="employee-free-coffee">Free coffee</label></div>
<div class="form-input">
  <input type="checkbox" id="employee-free-coffee" html:bidir-checked="employee.receivingFreeCoffee"/>
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
  This lambda takes `Comsumer<String>` as a single parameter and must call `accept()` method.
  Such strange API is required for integration with routing library.
  If you want to generate target manually, you better use [attr:href](/docs/flavour/component-packages/attr.html).

Example:

```html
<a html:link="it.accept('/employees/' + employee.id)">
  <html:text value="employee.firstName"/> <html:text value="employee.lastName"/>
</a>
```
