---
id: expressions
title: Expressions
sidebar_label: Expressions
---

## Overview

You can use expressions in multiple places in OpenRefine to extend data cleanup and manipulation. 

The expressions editor window is available for the following functions:
* <span class="menuItems">Facet</span>:

    *   <span class="menuItems">Custom text facet...</span>
    *   <span class="menuItems">Custom numeric facet…</span>
    *   You can also manually “change” most Customized facets after they have been created, which will bring up an expressions window. 
* <span class="menuItems">Edit cells</span>: 

    *   <span class="menuItems">Transform…</span>
    *   <span class="menuItems">Split multi-valued cells…</span>
    *   <span class="menuItems">Join multi-valued cells…</span>
* <span class="menuItems">Edit column</span>: 

    *   <span class="menuItems">Split</span>
    *   <span class="menuItems">Join</span>
    *   <span class="menuItems">Add column based on this column</span>
    *   <span class="menuItems">Add column by fetching URLs</span>

In each expressions window you will have the opportunity to select one supported language. The default is [GREL (General Refine Expression Language)](#grel-general-refine-expression-language); OpenRefine also comes with support for [Clojure](#clojure) and [Jython](#jython). Extensions may offer support for more expressions languages. These languages have some syntax differences but support most of the same variables. For example, the GREL expression `value.split(" ")[1]` would be written in Jython as `return value.split(" ")[1]`.

This page is a general reference for available functions, variables, and syntax. For examples that use these expressions for common data tasks, look at the [Recipes section on the Wiki](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Users#recipes-and-worked-examples). 

### Expressions

There are significant differences between OpenRefine's expressions and the spreadsheet formulas you may be used to using for data manipulation. OpenRefine does not store formulas in cells and display output dynamically; OpenRefine’s transformations are one-time operations that can change column contents or generate new columns. These are applied using variables such as `value` or `cell` to perform the same modification to each cell in a column. 

Take the following example:

|ID|Friend|Age|
|---|---|---|
|1.|John Smith|28|
|2.|Jane Doe|33|

Were you to apply a transformation to the “friend” column with the expression

```
 value.split(" ")[1]
```

OpenRefine would work through each row, splitting the “friend” values based on a space character. `value` for row 1 would be “John Smith” so the output would be “Smith” (as "[1]" selects the second part of the created output); `value` for row 2 would be “Jane Doe” so the output would be “Doe.” Using variables, a single expression yields different results for different rows. The old information would be discarded; you couldn't get "John" and "Jane" back unless you undid the operation in the History tab.

For another example, if you were to create a new column based on your data using the expression `row.starred`, it would generate a column of true and false values based on whether your rows were starred at that moment. If you were to then star more rows and unstar some rows, that data would not dynamically update - you would need to run the operation again to have current true/false values. 

Note that an expression is typically based on one particular column in the data -- the column whose drop-down menu is invoked. Many variables are created to stand for things about the cell in that “base column” of the current row on which the expression is evaluated. But there are still variables about the whole row, and through them, you can access cells in other columns.

### The expressions editor

When you select a function that offers the ability to supply expressions, you will see a window overlay the screen showing what we call the expressions editor. 

![The expressions editor window with a simple expression.](/img/expression-editor.png)

The expressions editor offers you a field for entering your formula and shows you a preview of its transformation on your first ten rows of cells. 

There is a dropdown menu from which you can choose an expression language. The default is GREL. Jython and Clojure are also offered with the default installation, and you may be able to add more language support with third-party extensions and customizations. 

There are also tabs for:
*   History, which shows you formulas you’ve recently used from across all your projects
*   Starred, which shows you formulas from your History that you’ve starred for reuse
*   Help, a quick reference to GREL functions.

Starring formulas you’ve used in the past can be very helpful for repetitive tasks you’re performing in batches. 

You can also choose how formula errors are handled: replicate the original cell value, output an error message into the cell, or ouput a blank cell.

### Regular expressions

OpenRefine offers several fields that support the use of regular expressions (regex), such as in a <span>Text filter</span> or a <span>Replace…</span> operation. GREL and other expressions can also use regular expression markup to extend their functionality. 

If this is your first time working with regex, you may wish to read [this tutorial specific to the Java syntax that OpenRefine supports](https://docs.oracle.com/javase/tutorial/essential/regex/). We also recommend this [testing and learning tool](https://regexr.com/).

#### GREL-supported regex

To write a regular expression inside a GREL expression, wrap it between a pair of forward slashes (/) much like the way you would in Javascript. For example, in

```
value.replace(/\s+/, " ")
```

the regular expression is `\s+`, and the syntax used in the expression wraps it with forward slashes (`/\s+/`). Though the regular expression syntax in OpenRefine follows that of Java (normally in Java, you would write regex as a string and escape it like "\\s+"), a regular expression within a GREL expression is similar to Javascript.

Do not use slashes to wrap regular expressions outside of a GREL expression.

[GREL functions](#grel-general-refine-expression-language) that support regex:
*   replace
*   match
*   partition
*   rpartition
*   split

#### Jython-supported regex

You can also use [regex with Jython expressions](http://www.jython.org/docs/library/re.html), instead of GREL, for example with a Custom Text Facet: 

```
python import re g = re.search(ur"\u2014 (.*),\s*BWV", value) return g.group(1)
```

#### Clojure-supported regex

[Clojure](https://clojure.org/reference/reader) uses the same regex engine as Java, and can be invoked with [re-find](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/re-find), [re-matches](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/re-matches), etc. You can use #"pattern" reader macro. For example, to get the nth element of a returned sequence, you can use the nth function:

```
clojure (nth (re-find #"\u2014 (.*),\s*BWV" value) 1)
```

## Constants and variables

### Constants

|Name |Meaning |
|-|-|
| true | the boolean constant true |
| false | the boolean constant false |
| PI | the constant of pi, from Java's Math.PI (i.e. 3.1415...) |

### Variables

Most of the OpenRefine-specific variables have attributes, aspects of the objects that can be called separately. We call these attributes "member fields" because they belong to certain variables. For example, you can query a record to find out how many rows it contains with `record.rowCount`: `rowCount` is a member field specific to `record`. 

|Variable |Meaning |
|-|-|
| value | the value of the cell in the current column of the current row (can be null) |
| row | the current row |
| cells | the cells of the current row, with fields that correspond to the column names |
| cell | the cell in the current column of the current row, containing value and other attributes |
| recon | the cell's reconciliation information returned from a reconciliation service or provider |
| record | one or more rows grouped together to form a record |
| rowIndex | the index value of the current row (the first row is 0) |

#### Row

The `row` variable itself is not very useful, but you can use the row's member fields, which can be accessed with a dot operator or with square brackets: `row.index` or `row["index"]`.

|Member field name |Meaning |
|-|-|
| row.index | the index value of the current row (the first row is 0) |
| row.cells | the cells of the row, |
| row.columnNames | the column names of the row, i.e., the column names in the project. This will report all columns, even those with null cell values in the particular row. |
| row.starred | boolean, indicating if the row is starred |
| row.flagged | boolean, indicating if the row is flagged |
| row.record | the Record object containing the current row, same as the record variable above |

#### Cells

The `cells` object, which can also be accessed as `row.cells`, has fields that correspond to the data column names. For example, `cells.Foo` returns a `cell` object representing the cell in the column named “Foo” of the current row. If the column name has spaces, use the square bracket method, e.g., cells["Postal Code"].

When you need to get the value of the cells variable itself, you need `.value` at the end, e.g.,

```
cells["column name"].value
```

When you need to set or mass edit the values of the columns cells, then you can simply use a GREL expression within quotes, such as just `"San Francisco Bay"`.

Alternatively, you can use faceting to edit large quantities of identical cell values, using the “edit” button that appears in the facet display.

#### Cell

A `cell` object contains all the data of a cell and stored as a single object that has two fields.

You can use `cell` on its own in the Expression editor to copy all the contents of a column to another column, including reconciliation information. Although the preview in the expressions editor will only show a small representation [object Cell], it will actually copy all the cell's data. Try this with "Edit Column -> Add Column based on this column ..."

|Field name |Meaning |Member fields |
|-|-|-|
| cell | an object containing the entire contents of the cell | .value, .recon, .errorMessage |
| cell.value | the value in the cell, which can be null, a string, a number, a boolean, or an error |  |
| cell.recon | an object encapsulating the reconciliation results for that cell |  |
| cell.errorMessage | returns the message of an EvalError instead of the error object itself. Use cell.value to return the error object if needed |  |

#### Reconciliation

|Field name |Meaning |Member fields |
|-|-|-|
| cell.recon.judgment | a string that is one of: "matched", "new", "none" |  |
| cell.recon.judgmentAction | a string that is one of: "single", "similar" (single means the reconcilation judgement was only applied to that cell, while similar means the judgement was applied to all similar cells |  |
| cell.recon.judgmentHistory | a number that is the epoch timestamp (in milliseconds) of when the reconcilation judgement was made for the cell |  |
| cell.recon.matched | a boolean, true if judgment is "matched" |  |
| cell.recon.match | null, or the recon candidate that has been matched against this cell | .id, .name, .type |
| cell.recon.best | null, or the highest scoring recon candidate from the reconciliation service | .id, .name, .type, .score |
| cell.recon.features | an object encapsulating reconciliation features | .typeMatch, .nameMatch, .nameLevenshtein, .nameWordDistance |
| cell.recon.candidates | an object encapsulating the default 3 candidates | .id, .name, .type, .score |

The `cell.recon.candidates` array can be accessed with something like:
```
forEach(cell.recon.candidates,v,v.id).join(",")
```

The `recon.candidates` and `recon.best` objects have a few deeper fields: id, name, type, and score. `type` is an array of type IDs for a list of candidates, or a single string for the best candidate. The `id` of the best candidate can be accessed as any one of
*   `recon.best.id`
*   `cell.recon.best.id`
*   `row.cells["Column name"].recon.best.id`

etc.

A `features` object has the following fields:
*   `typeMatch`, `nameMatch`: booleans, indicating whether the best candidate matches the intended reconciliation type and whether the best candidate's name matches the cell's text exactly
*   `nameLevenshtein`, `nameWordDistance`: numbers computed by comparing the best candidate's name with the cell's text; larger numbers mean bigger difference

#### Record

A `record` object encapsulates one or more rows that are grouped together. A `record` object has a few fields, which can be accessed with a dot operator or with square brackets: `record.index` or `record["index"]`.

|Field name |Meaning |Example |
|-|-|-|
| record.index | zero-based index of the current record | evaluating row.record.index on row 2 returns 0 |
| record.cells | the cells of the row | evaluating row.record.cells.book.value on row 2 returns [ "Anathem", "Snow Crash" ] |
| record.fromRowIndex | zero-based index of the first row in the record | evaluating row.record.fromRowIndex on row 2 returns 0 |
| record.toRowIndex | index of the last row in the record + 1 (i.e. the next row) | evaluating row.record.toRowIndex on row 2 returns 2 |
| record.rowCount | count of the number of rows in the record | evaluating row.record.rowCount on row 2 returns 2 |

## GREL (General Refine Expression Language)

### Basics

GREL is designed to resemble Javascript. Formulas use variables and depend on data types to do things like string manipulation or mathematical calculations:

|Example|Description|
|---|---|
| value + " (approved)" | concatenate two strings; whatever is in value gets converted to a string first |
| value + 2.239 | add 2.239 to the existing value (if a number); append text "2.239" to the end of the string otherwise |
| value.trim().length() | trimming leading and trailing whitespace of value and then take the length of the result |
| value.substring(7, 10) | take the substring of value from character index 7 up to and excluding character index 10 |
| value.substring(13) | take the substring of value from character index 13 until the end of the string |

#### Syntax

In OpenRefine expression language function can use either of these two forms:
*   functionName(arg0, arg1, ...)
*   arg0.functionName(arg1, ...)

The second form is a shorthand to make expressions easier to read. It simply pulls the first argument out and appends it to the front of the function, with a dot:

|Dot notation |Full notation |
|-|-|
| value.trim().length() | length(trim(value)) |
| value.substring(7, 10) | substring(value, 7, 10) |

So, in the dot shorthand, the functions occur from left to right in the order of calling, rather than in the reverse order with parentheses.

The dot notation can also be used to access variables that are related to other variables (what we call “member fields”):

|Example |Description |
|-|-|
| cell.recon.match | the matched value of a reconciled cell |
| row.index | index of the current row |

For referring to column names that contain spaces (anything not a continuous string), use square brackets instead of the dot notation:

|Example |Description |
|-|-|
| FirstName.cells | access the cell in the column named “FirstName” of the first row |
| cells["First Name"] | access the cell in the column called "First Name" of the current row |

Brackets can also be used to get substrings and sub-arrays:

|Example |Description |
|-|-|
| value[1,3] | a substring of value, starting from character 1 up to but excluding character 3 |
| "internationalization"[1,3] | will return “nt” |
| "internationalization"[1,-2] | will return “nternationalizati” (negative indexes are counted from the end) |

If you're used to Excel, note that the operator for string concatenation is + (not &).

### Function reference

#### Boolean functions

and(boolean b1, boolean b2, ...etc)

Logically AND two or more booleans to yield a boolean. For example, and(1 < 3, 1 > 0) returns true because both conditions are true.

or(boolean b1, boolean b2, ...etc)

Logically OR two or more booleans to yield a boolean. For example, or(1 < 3, 1 > 7) returns true because at least one of the conditions (the first one) is true.

not(boolean b)

Logically NOT a boolean to yield another boolean. For example, not(1 > 7) returns true because 1 > 7 itself is false.

xor(boolean b1, boolean b2, ...etc)

Logically XOR (exclusive-or) two or more booleans to yield a boolean. For example, xor(1 < 3, 1 > 7) returns true because only one of the conditions (the first one) is true. xor(1 < 3, 1 < 7) returns false because more than one of the conditions is true.




#### String functions

#### Format functions (JSON, HTML, XML)

#### Array functions

#### Date functions

#### Math functions

#### Other functions

### Controls


## Jython
*   Python syntax in OpenRefine
*   Installing Python packages


## Clojure
*   Clojure syntax in OpenRefine
*   Using Java packages from Clojure