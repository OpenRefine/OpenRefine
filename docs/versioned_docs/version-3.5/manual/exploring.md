---
id: exploring
title: Exploring data
sidebar_label: Overview
---

## Overview {#overview}

OpenRefine offers lots of features to help you learn about your dataset, even if you don’t change a single character. In this section we cover different ways for sorting through, filtering, and viewing your data. 

Unlike spreadsheets, OpenRefine doesn’t store formulas and display the output of those calculations; it only shows the value inside each cell. It doesn’t support cell colors or text formatting. 

## Data types {#data-types}

Each piece of information (each cell) in OpenRefine is assigned a data type. Some file formats, when imported, can set data types that are recognized by OpenRefine. Cells without an associated data type on import will be considered a “string” at first, but you can have OpenRefine convert cell contents into other data types later. This is set at the cell level, not at the column level. 

You can see data types in action when you preview a new project: check the box next to <span class="fieldLabels">Attempt to parse cell text into numbers</span>, and cells will be converted to the “number” data type based on their contents. You’ll see numbers change from black text to green if they are recognized.

The data type will determine what you can do with the value. For example, if you want to add two values together, they must both be recognized as the number type. 

You can check data types at any time by:
*   clicking “edit” on a single cell (where you can also edit the type)
*   creating a <span class="menuItems">Custom Text Facet</span> on a column, and inserting `type(value)` into the <span class="fieldLabels">Expression</span> field. This will generate the data type in the preview, and you can facet by data type if you press <span class="buttonLabels">OK</span>.

The data types supported are:
*   string (one or more text characters)
*   number (one or more characters of numbers only)
*   boolean (values of “true” or “false”)
*   [date](#dates) (ISO-8601-compliant extended format with time in UTC: YYYY-MM-DDTHH:MM:SSZ)

OpenRefine recognizes two further data types as a result of its own processes:
*   error
*   null

An “error” data type is created when the cell is storing an error generated during a transformation in OpenRefine.

A “null” data type is a special type that means “this cell has no value.” It’s distinct from cells that have values such as “0” or “false”, or cells that look empty but have whitespace in them, or cells that contain empty strings. When you use `type(value)`, it will show you that the cell’s value is “null” and its type is “undefined.” You can opt to [show “null” values](sortview#showhide-null), by going to <span class="menuItems">All</span> → <span class="menuItems">View</span> → <span class="menuItems">Show/Hide ‘null’ values in cells</span>.

Changing a cell's data type is not the same operation as transforming its contents. For example, using a column-wide transform such as <span class="menuItems">Transform</span> → <span class="menuItems">Common transforms</span> → <span class="menuItems">To date</span> may not convert all values successfully, but going to an individual cell, clicking “edit”, and changing the data type can successfully convert text to a date. These operations use different underlying code. Learn more about date formatting and transformations in the next section. 

To transform data from one type to another, see [Transforming data](cellediting#data-type-transforms) for information on using common tranforms, and see [Expressions](expressions) for information on using [toString()](grelfunctions#tostringo-string-format-optional), [toDate()](grelfunctions#todateo-b-monthfirst-s-format1-s-format2-), and other functions. 


### Dates {#dates}

A “date” type is created when a column is [transformed into dates](transforming#to-date), when an expression is used to [convert cells to dates](grelfunctions#todateo-b-monthfirst-s-format1-s-format2-) or when individual cells are set to have the data type “date”. 

Date-formatted data in OpenRefine relies on a number of conversion tools and standards. For something to be considered a date in OpenRefine, it will be converted into the ISO-8601-compliant extended format with time in UTC: YYYY-MM-DDTHH:MM:SSZ.

When you run <span class="menuItems">Edit cells</span> → <span class="menuItems">Common transforms</span> → <span class="menuItems">To date</span>, the following column of strings on the left will transform into the values on the right:

|Input|→|Output|
|---|---|---|
|23/12/2019|→|2019-12-23T00:00:00Z|
|14-10-2015|→|2015-10-14T00:00:00Z|
|2012 02 16|→|2012-02-16T00:00:00Z|
|August 2nd 1964|→|1964-08-02T00:00:00Z|
|today|→|today|
|never|→|never|

OpenRefine uses a variety of tools to recognize, convert, and format [dates](exploring#dates) and so some of the values above can be reformatted using other methods. In this case, clicking the “today” cell and editing its data type manually will convert “today” into a value such as “2020-08-14T00:00:00Z”. Attempting the same data-type change on “never” will give you an error message and refuse to proceed.  
 
You can do more precise conversion and formatting using expressions and arguments based on the state of your data: see the GREL functions reference section on [Date functions](grelfunctions#date-functions) for more help.

You can convert dates into a more human-readable format when you [export your data using the custom tabular exporter](exporting#custom-tabular-exporter). You are given the option to keep your dates in the ISO 8601 format, to output short, medium, long, or full locale formats, or to specify a custom format. This means that you can format your dates into, for example, MM/DD/YY (the US short standard) with or without including the time, after working with ISO-8601-formatted dates in your project.  

The following table shows some example [date and time formatting styles for the U.S. and French locales](https://docs.oracle.com/javase/tutorial/i18n/format/dateFormat.html):

|Style 	|U.S. Locale 	|French Locale|
|---|---|---|
|Default 	|Jun 30, 2009 7:03:47 AM 	|30 juin 2009 07:03:47|
|Short	|6/30/09 7:03 AM 	|30/06/09 07:03|
|Medium 	|Jun 30, 2009 7:03:47 AM 	|30 juin 2009 07:03:47|
|Long	|June 30, 2009 7:03:47 AM PDT 	|30 juin 2009 07:03:47 PDT|
|Full 	|Tuesday, June 30, 2009 7:03:47 AM PDT 	|mardi 30 juin 2009 07 h 03 PDT|

## Rows vs. records {#rows-vs-records}

A row is a simple way to organize data: a series of cells, one cell per column. Sometimes there are multiple pieces of information in one cell, such as when a survey respondent can select more than one response. 

In cases where there is more than one value for a single column in one or more rows, you may wish to use OpenRefine’s records mode: this defines a single record as potentially containing more than one row. From there you can transform cells into multiple rows, each cell containing one value you’d like to work with. 

Generally, when you import some data, OpenRefine reads that data in row mode. From the project screen, you can convert the project into records mode. OpenRefine remembers this action and will present you with records mode each time you open the project from then on. 

OpenRefine understands records based on the content of the first column, what we call the “key column.” Splitting a row into a multi-row record will base all association on the first column in your dataset. 

If you have more than one column to split out into multiple rows, OpenRefine will keep your data associated with its original record, and associate subgroups based on the top-most row in each group. 

You can imagine the structure as a tree with many branches, all leading back to the same trunk. 

For example, your key column may be a film or television show, with multiple cast members identified by name, associated to that work. You may have one or more roles listed for each person. The roles are linked to the actors, which are linked to the title.

|Work|Actor|Role|
|---|---|---|
|The Wizard of Oz|Judy Garland|Dorothy Gale|
||Ray Bolger|"Hunk"|
|||The Scarecrow|
||Jack Haley|"Hickory"|
|||The Tin Man|
||Bert Lahr|"Zeke"|
|||The Cowardly Lion|
||Frank Morgan|Professor Marvel|
|||The Gatekeeper|
|||The Carriage Driver|
|||The Guard|
|||The Wizard of Oz|
||Margaret Hamilton|Miss Almira Gulch|
|||The Wicked Witch of the West|

Once you are in records mode, you can still move some columns around, but if you move a column to the beginning, you may find your data becomes misaligned. The new key column will sort into records based on empty cells, and values in the old key column will be assigned to the last row in the old record (the key value sitting above those values). 

OpenRefine assigns a unique key behind the scenes, so your records don’t need a unique identifier in the key column. You can keep track of which rows are assigned to each record by the record number that appears under the <span class="menuItems">All</span> column.

To [split multi-valued cells](transforming#split-multi-valued-cells) and apply other operations that take advantage of records mode, see [Transforming data](transforming). 

Be careful when in records mode that you do not accidentally delete rows based on being blank in one column where there is a value in another. 
