---
title: Standard components - attr package
exclude_from_nav: true
---

`attr` component package consists of exactly one attribute component which is denoted as `attr:*`,
which means you can put any identifier for `*`.
This allows to use `attr:` notation to compute attribute based on expression.

Syntax:

```
attr:attributeName="expression"
```

Where:

* `attributeName` is the name of an attribute to set.
* `expression` is an expression whose value will be set to given attribute. 

Example:

```html
<div attr:style="'color: \'' + (hasError ? 'red' : 'black') + '\''">
  ...
</div>
```