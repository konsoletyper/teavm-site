---
title: Standard components - std package
exclude_from_nav: true
---


This package includes following components:

* [foreach](#foreach)
* [if](#if)
* [choose](#choose)
* [with](#with)
* [let](#let)
* [insert](#insert)


# foreach

Syntax:

```html
<std:foreach var="variableName" index="variableName" in="expression">
  <!-- body -->
</std:foreach>
```

Where:

* `var="variableName"` variable that can be used in expressions inside body. 
  This variable takes value of collection's item for each occurrence of body.
* `index="variableName"` is an optional attribute which specifies name of variable
  which takes index of current item.
* `in="expression"` is an expression of `Iterable<T>` or `T[]` type. 
* `<!-- body -->` is an HTML fragment which will be repeated for each item of collection.
  
Example:

```html
<table>
  <thead>
    <tr>
      <th>#</th>
      <th>First name</th>
      <th>Last name</th>
      <th>Salary</th>
    </tr>
  </thead>
  <tbody>
    <std:foreach index="index" var="employee" in="employees">
      <tr>
        <td><html:text value="index"/></td>
        <td><html:text value="employee.firstName"/></td>
        <td><html:text value="employee.lastName"/></td>
        <td>$<html:text value="employee.salary"/></td>
      </tr>
    </std:foreach>
  </tbody>
</table>
```


# if

Syntax

```html
<std:if condition="expression">
  <!-- body -->
</std:if>
```

Where:

* `condition="expression"` boolean expression that specifies whether to show body of the component.
* `<!-- body -->` is an HTML fragment which will be rendered if condition holds.

Example:

```html
<std:if condition="employees.empty">
  There are no employees found in the database
</std:if>
```


# choose

Shows one of the specified fragments depending on given condition.

Syntax:

```html
<std:choose>
  <std:option when="expression1">
    <!-- body 1 -->
  </std:option>
  <std:option when="expression2">
    <!-- body 2 -->
  </std:option>
  ...
  <std:otherwise>
    <!-- body by default -->
  </std:otherwise>
</std:choose>
```

Where:

* `when="expressionN"` are boolean expressions that specify which body to show.
* `<!-- body N -->` are HTML fragments, one of which will be shown depending on condition.
* `<!-- body by default -->` HTML fragment that will be shown if non of the conditions hold.


# with

Evaluates expression and binds it to a variable.

Syntax:

```html
<std:with var="variableName" value="expression">
 <!-- body -->
</std:with>
```

Where:

* `var="variableName"` is a name of varable to hold value of expression.
* `value="expression"` is an expression to evaluate.
* `<!-- body -->` an HTML fragment which can refer to *variableName*.

Example:

```html
<std:with var="company" value="employee.businessUnit.company">
  <div><b>Company:</b> <html:text value="company.name + ' at ' + company.country.name"/></div>
</std:with>
```


# let

Evaluates several expressions and binds them to variables.

Syntax:

```html
<std:let>
  <std:var name="variableName1" value="expression1"/>
  <std:var name="variableName2" value="expression2"/>
  ...
  <std:in>
   <!-- body -->
  </std:in>
</std:let>
```

Where:

* `name="variableNameN"` is the name of a variable that receives value of expression.
* `value="expressionN""` is an expression to evaluate.
* `<!-- body -->` is an HTML fragment that can refer to defined variables.


# insert

Inserts HTML fragment.

Syntax:

```html
<std:insert fragment="expression"/>
```

Where:

* `fragment="expression"` which page to insert.

Example:

```html
<p>Choose which function to show</p>
<ul>
  <li>
    <button type="button" event:click="showFibonacci()">Fibonacci</button>
  </li>
  <li>
    <button type="button" event:click="showPrime()">Prime number</button>
  </li>
</ul>
<div>
  <std:insert fragment="function"/>
</div>
```

```java
@BindTemplate('functions.html')
class FunctionsView {
    private Object function;
    
    public Fragment getFunction() {
        return function;
    }
    
    public void showFibonacci() {
        function = Templates.create(new Fibonacci());
    }
    
    public void showPrime() {
        function = Templates.create(new PrimeNumbers());
    }
}
```

