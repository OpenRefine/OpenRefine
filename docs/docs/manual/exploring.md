---
id: exploring
title: Exploring data
sidebar_label: Overview
---

## Overview

OpenRefine is a powerful tool for learning about your dataset, even if you don’t change a single character. In this section we cover different ways for sorting through, filtering, and viewing your data. 

Unlike spreadsheets, OpenRefine doesn’t store formulas and display the output of those calculations; it only shows the value inside each cell. It doesn’t import any cell colors or text formatting. 

## Cell data types

Data loaded into OpenRefine is assigned a data type. By default every cell will be considered a “string”, but you can have OpenRefine convert cell contents into other data types later.

You can see data types in action when you preview a new project: check the box that says “Attempt to parse cell text into numbers” and cells will be converted to the number type based on their contents. You’ll see numbers change from black text to green if they are recognized.

The data type will determine what you can do with the value. For example, if you want to add two values together, they must both be recognized as the “number” data type. 

You can check data types at any time by:
*   clicking “edit” on a single cell (where you can also edit the type)
*   creating a Custom Text Facet on a column, and inserting `type(value)` into the “Expression” field. This will generate the data type in the preview, and you can facet by data type if you press “OK.”

The data types supported are:
*   string (one or more text characters)
*   number (one or more characters of numbers only)
*   boolean (values of “true” or “false”)
*   date (ISO-8601-compliant extended format with time in UTC: **YYYY**-**MM**-**DD**T**HH**:**MM**:**SS**Z)
*   array (a set of comma-separated string, number, boolean, or date values)
*   error
*   null

A “date” type is created when a text column is [transformed into dates](transforming#to-date), or when individual cells are set to have the data type “date.” 

An “error” data type is created when the cell is storing an error generated during a transformation in OpenRefine.

A “null” data type is a special value which basically means “this cell has no value.” It’s used to differentiate between cells that have values such as “0” or “false” - or a cell that looks empty but has, for example, spaces in it. When you use `type(value)`, it will show you that the cell’s value is “null” and its type is “undefined.” You can opt to [show “null” values](#view) to differentiate them from empty strings, by going to “All” > “View” > “Show/Hide ‘null’ values in cells.”

Converting a cell's data type is not the same operation as transforming its contents. For example, using a column-wide transform such as “Transform” > “Common transforms …” > “to date” may not convert all values successfully, but going to an individual cell, clicking “edit” and changing the data type can successfully convert text to a date. These operations use different underlying code. 

To transform data from one type to another, see [Transforming data](transforming#transform) for information on using common tranforms, and see [Expressions](expressions) for information on using `toString()`, `toDate()`, and other functions. 

## Row types: rows vs. records

A row is a simple way to organize data: a series of cells, one cell per column. Sometimes there are multiple pieces of information in one cell, such as when a survey respondent can select more than one response. In cases where there is more than one value for a single column in one or more rows, you may wish to use OpenRefine’s records mode: this defines a single record (a survey response, for example) as potentially containing more than one row. From there you can transform cells into multiple rows, each cell containing one value you’d like to work with. 

Generally, when you import some data, OpenRefine reads that data in row mode. From there you can convert the project into records mode. OpenRefine remembers this action and will present you with records mode each time you open the project from then on. 

OpenRefine understands records based on the content of the first column. Splitting a row into a multi-row record will base all association on the first column in your dataset. If you have more than one column to split out into multiple rows, OpenRefine will keep your data associated with its original record: you can imagine this structure as a tree with many branches, all leading back to the same trunk. 

OpenRefine assigns a unique key behind the scenes, so your records don’t need a unique identifier. You can keep track of which rows are assigned to which record by the record number that appears under the “All” column.

To [split multi-valued cells](transforming#split-multi-valued-cells) and apply other operations that take advantage of records mode, see [Transforming data](transforming). 

Be careful when in records mode that you do not accidentally delete rows based on being blank in one column where there is a value in another. 