---
title: Form validation
---

# Design

Form validation components were designed with following goals in mind:

* To be compatible with any UI library and CSS framework.
* To not force users to follow certain guidelines, let them follow their guidelines instead.
* To be type safe, like the rest of the framework.
* To be flexible.

Validation components are components for the template engine,
therefore they can only be used from HTML templates.


# Usage

To start using validation components, you should first include the corresponding component package in the template.
Additionally, importing the `Converter` interface may be useful:

```html
<?import org.teavm.flavour.validation.Converter?>
<?use v:org.teavm.flavour.validation?>
```

The first component, called `validator`, is used to define validation logic.
It has the following syntax:

```html
<v:validator as="validatorName">
  <v:validation of="dataField1" as="fieldName1" convert="converter1">
    <v:check rule="validationExpression" as="ruleName"/>
    <!-- etc -->
  </v:validation>
  <v:validation of="dataField2" as="fieldName2" convert="converter2">
    <v:check rule="validationExpression" as="ruleName"/>
    <!-- etc -->
  </v:validation>
  <!-- more v:validation entries -->
  
  <!-- body -->
</v:validator>
```

Where:

* `as="validationName""` name of the variable that provides [validator state object](#validator-state).
* `of="dataFieldN"` path to a *mutable* field or property of a view object that stores actual data.
* `as="fieldNameN"` name of the variable that provides [field state object](#field-state)
* `convert="converterN"` expression that yields a `Converter` instance that should be used
  to convert data to and from string.
* `rule="validationExpression"` expression that yields a boolean value, `true` if validation passes.
* `as="ruleName"` name of the variable that provides a `boolean` value which indicates whether the rule passes.

The second component, `bind`, is used to bind validations to input fields:

```
v:bind="fieldName"
```

where `fieldName` points to field state object defined by the containing `v:validator`.


# Standard converters

Standard converters are available via static methods of the `Converter` interface:

* `Converter<String> stringFormat()` does not perform any conversion.
* `Converter<Integer> integerFormat()`
* `Converter<Double> doubleFormat(String formatString)` 
  where *formatString* is a string accepted by `DecimalFormat` class.
* `Converter<Date> dateFormat(String formatString)`
  where *formatString* is a string accepted by `SimpleDateFormat` class.
* `Converter<Date> dateFormat()` uses the default locale-specific date format.
* `Converter<Date> mediumDateFormat()` uses the locale-specific medium date format.
* `Converter<Date> shortDateFormat()` uses the locale-specific medium date format.
* `Converter<Date> shortDateFormat()` uses the locale-specific medium date format.
* `Converter<Date> dateFormat(DateFormat dateFormat)` uses a custom date format.
  
  
# Validator state

Validator state is available via the `ValidatorState` class which has the following API:

* `boolean isValid()` indicates whether the form passed validation, i.e. all fields are valid. 
* `void submit(Runnable action)` validates the form and performs the given action if form is valid.


# Field state

Field state is available via the `Validation` class which has the following API:

* `boolean isValidFormat()` indicates whether the converter could successfully recognize value in a text field.
* `boolean isValid()` indicates whether all validation rules pass.


# Example

```html
<?import org.teavm.flavour.validation.Converter?>
<?use v:org.teavm.flavour.validation?>
<v:validator as="validator">
  <v:validation of="title" as="titleField" convert="Converter.stringFormat()">
    <v:check rule="!it.empty" as="titleSpecified"/>
  </v:validation>
  <v:validation of="startDate" as="startDateField" convert="Converter.dateFormat('yyyy-MM-dd')">
    <v:check rule="it != null" as="startDateSpecified"/>
    <v:check rule="endDate == null or it.before(endDate)" as="startDateConsistent"/>
  </v:validation>
  <v:validation of="endDate" as="endDateField" convert="Converter.dateFormat('yyyy-MM-dd')">
    <v:check rule="it != null" as="endDateSpecified"/>
    <v:check rule="startDate == null or startDate.before(it)" as="endDateConsistent"/>
  </v:validation>

  <div>
    <div><label>Title</label></div>
    <div attr:class="titleField.valid ? '' : 'error'"><input type="text" v:bind="titleField"/></div>
    <std:if condition="!titleSpecified"><div>Please, specify title</div></std:if>
  </div>

  <div>
    <div><label>Start date</label></div>
    <div attr:class="startDateField.valid ? '' : 'error'"><input type="text" v:bind="startDateField"/></div>
    <std:if condition="!startDateSpecified"><div>Please, specify start date</div></std:if>
  </div>
  <div>
    <div><label>End date</label></div>
    <div attr:class="endDateField.valid ? '' : 'error'"><input type="text" v:bind="endDateField"/></div>
    <std:if condition="!endDateSpecified"><div>Please, specify end date</div></std:if>
  </div>

  <std:if condition="!startDateConsistent"><div>Start date must be later than end date</div></std:if>

  <button type="button" event:click="validator.submit(() -> save())" html:enabled="validator.valid">Submit</button>

</v:validator>
```

