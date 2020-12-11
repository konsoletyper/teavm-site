---
title: Standard components - event package
exclude_from_nav: true
---

`event` component package consists of similar components:

* `event:click`
* `event:async-click`
* `event:dblclick`
* `event:async-dblclick`
* `event:mouseup`
* `event:async-mouseup`
* `event:mousedown`
* `event:async-mousedown`
* `event:change`

All these components allow to specify event handler.
`async-*` versions allow to launch "long-running" actions like AJAX requests or IndexedDB API usage.
 
Syntax:

```
event:eventname="expression"
event:async-eventname="expression"
```

Where

* `eventname` is the name of a JavaScript event to bind handler to.
* `expression` is an lambda expression that's evaluated when event triggers.
  The lambda takes native `Event` object as a parameter.

Example:

```html
<div event:click="visible = not visible">
  <div>Click me to toggle!</div>
  <div attr:style="'display: ' + (visible ? 'block' : 'none')">Lorem ipsum ...</div>
</div>
```
