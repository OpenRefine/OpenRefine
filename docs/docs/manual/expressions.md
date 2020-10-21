---
id: expressions
title: Expressions
sidebar_label: Expressions
---

## Overview

You can use expressions in multiple places in OpenRefine to extend data cleanup and manipulation. 

Expressions are available with the following functions:
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

In the expressions editor window you will have the opportunity to select one supported language. The default is [GREL (General Refine Expression Language)](#grel-general-refine-expression-language); OpenRefine also comes with support for [Clojure](#clojure) and [Jython](#jython). Extensions may offer support for more expressions languages. 

These languages have some syntax differences but support most of the same [variables](#variables). For example, the GREL expression `value.split(" ")[1]` would be written in Jython as `return value.split(" ")[1]`.

This page is a general reference for available functions, variables, and syntax. For examples that use these expressions for common data tasks, look at the [Recipes section on the Wiki](https://github.com/OpenRefine/OpenRefine/wiki/Documentation-For-Users#recipes-and-worked-examples). 

### Expressions

There are significant differences between OpenRefine's expressions and the spreadsheet formulas you may be used to using for data manipulation. OpenRefine does not store formulas in cells and display output dynamically: OpenRefine’s transformations are one-time operations that can change column contents or generate new columns. These are applied using variables such as `value` or `cell` to perform the same modification to each cell in a column. 

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

Note that an expression is typically based on one particular column in the data - the column whose drop-down menu is invoked. Many variables are created to stand for things about the cell in that “base column” of the current row on which the expression is evaluated. There are also variables about rows, which you can use to access cells in other columns.

### The expressions editor

When you select a function that offers the ability to supply expressions, you will see a window overlay the screen with what we call the expressions editor. 

![The expressions editor window with a simple expression: value + 10.](/img/expression-editor.png)

The expressions editor offers you a field for entering your formula and shows you a preview of its transformation on your first few rows of cells. 

There is a dropdown menu from which you can choose an expression language. The default is GREL. Jython and Clojure are also offered with the installation package, and you may be able to add more language support with third-party extensions and customizations. 

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

The [GREL functions](#grel-general-refine-expression-language) that support regex are:
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

## Variables

Most of the OpenRefine-specific variables have attributes: aspects of the variables that can be called separately. We call these attributes "member fields" because they belong to certain variables. For example, you can query a record to find out how many rows it contains with `row.record.rowCount`: `rowCount` is a member field specific to `record`, which is a member field of `row`. Member fields can be called using a dot separator, or with square brackets (`row["record"]`).

|Variable |Meaning |
|-|-|
| value | The value of the cell in the current column of the current row (can be null) |
| row | The current row |
| row.record | One or more rows grouped together to form a record |
| cells | The cells of the current row, with fields that correspond to the column names (or row.cells) |
| cell | The cell in the current column of the current row, containing value and other attributes |
| cell.recon | The cell's reconciliation information returned from a reconciliation service or provider |
| rowIndex | The index value of the current row (the first row is 0) |

### Row

The `row` variable itself is best used to access its member fields, which you can do using either a dot operator or square brackets: `row.index` or `row["index"]`.

|Member field name |Meaning |
|-|-|
| row.index | The index value of the current row (the first row is 0) |
| row.cells | The cells of the row, returned as an array |
| row.columnNames | An array of the column names of the row, i.e. the column names in the project. This will report all columns, even those with null cell values in the particular row. |
| row.starred | A boolean indicating if the row is starred |
| row.flagged | A boolean indicating if the row is flagged |
| row.record | The [record](#record) object containing the current row |

For array objects such as `row.columnNames` you can preview the array using the expressions window, and output it as a string using `toString(row.columnNames)` or with something like:

```forEach(row.columnNames,v,v).join("; ")```

### Cells

The `cells` object is used to call information from the columns in your project. For example, `cells.Foo` returns a `[cell](#cell)` object representing the cell in the column named “Foo” of the current row. If the column name has spaces, use square brackets, e.g., `cells["Postal Code"]`. There is no `cells.value` - it can only be used with member fields. To get the corresponding column value inside the `cells` variable, use `.value` at the end, for example `cells["Postal Code"].value`. 

### Cell

A `cell` object contains all the data of a cell and is stored as a single object that has two fields.

You can use `cell` on its own in the expressions editor to copy all the contents of a column to another column, including reconciliation information. Although the preview in the expressions editor will only show a small representation [object Cell], it will actually copy all the cell's data. Try this with <span class="menuItems">Edit Column</span> → <span class="menuItems">Add Column based on this column ...</span>.

|Field name |Meaning |Member fields |
|-|-|-|
| cell | An object containing the entire contents of the cell | .value, .recon, .errorMessage |
| cell.value | The value in the cell, which can be a string, a number, a boolean, null, or an error |  |
| cell.recon | An object encapsulating reconciliation results for that cell | See the reconciliation section below |
| cell.errorMessage | Returns the message of an *EvalError* instead of the error object itself (use value to return the error object) | .value |

### Reconciliation

Several of the fields here are equivalent to what can be used through [reconciliation facets](reconciling#reconciliation-facets). You must type `cell.recon`; `recon` on its own will not work.

|Field name |Meaning |Member fields |
|-|-|-|
| cell.recon.judgment | A string, either "matched", "new", "none" |  |
| cell.recon.judgmentAction | A string, either "single" or "similar" (or "unknown") |  |
| cell.recon.judgmentHistory | A number, the epoch timestamp (in milliseconds) of your judgment  |  |
| cell.recon.matched | A boolean, true if judgment is "matched" |  |
| cell.recon.match | The recon candidate that has been matched against this cell (or null) | .id, .name, .type |
| cell.recon.best | The highest scoring recon candidate from the reconciliation service (or null) | .id, .name, .type, .score |
| cell.recon.features | An array of reconciliation features to help you assess the accuracy of your matches | .typeMatch, .nameMatch, .nameLevenshtein, .nameWordDistance | 
| cell.recon.features<br />.typeMatch  | A boolean, true if your chosen type is "matched" and false if not (or "(no type)" if unreconciled) |  |
| cell.recon.features<br />.nameMatch | A boolean, true if the cell and candidate strings are identical and false if not (or "(unreconciled)") |  |
| cell.recon.features<br />.nameLevenshtein | A number, representing the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance): larger if the difference is greater between value and candidate |  | 
| cell.recon.features<br />.nameWordDistance | A number, based on the [word similarity](reconciling#reconciliation-facets) |  |
| cell.recon.candidates | An array of the top 3 candidates (default) | .id, .name, .type, .score |

The `cell.recon.candidates` and `cell.recon.best` objects have a few deeper fields: `id`, `name`, `type`, and `score`. `type` is an array of type identifiers for a list of candidates, or a single string for the best candidate. 

Arrays such as `cell.recon.candidates` and `cell.recon.candidates.type` can be joined into lists and stored as strings with something like:
```
forEach(cell.recon.candidates,v,v.name).join("; ")
```

### Record

A `row.record` object encapsulates one or more rows that are grouped together, when your project is in records mode. You must call it as `row.record`; `record` will not return values. 

|Field name |Meaning |
|-|-|
| row.record.index | The index of the current record (starting at 0) |
| row.record.cells | The cells of the row |
| row.record.fromRowIndex | The row index of the first row in the record |
| row.record.toRowIndex | The row index of the last row in the record + 1 (i.e. the next row) |
| row.record.rowCount | count of the number of rows in the record |

## GREL (General Refine Expression Language)

### Basics

GREL is designed to resemble Javascript. Formulas use variables and depend on data types to do things like string manipulation or mathematical calculations:

|Example|Output|
|---|---|
| value + " (approved)" | Concatenate two strings; whatever is in the cell gets converted to a string first |
| value + 2.239    | Add 2.239 to the existing value (if a number); append text "2.239" to the end of the string otherwise |
| value.trim().length() &nbsp; &nbsp; | Trim leading and trailing whitespace of the cell value and then output the length of the result |
| value.substring(7, 10) | Output the substring of the value from character index 7, 8, and 9 (excluding character index 10) |
| value.substring(13) | Output the substring from index 13 to the end of the string |

If you're used to Excel, note that the operator for string concatenation is + (not &).

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

The dot notation can also be used to access the member fields of [variables](#variables). For referring to column names that contain spaces (anything not a continuous string), use square brackets instead of dot notation:

|Example |Description |
|-|-|
| FirstName.cells | Access the cell in the column named “FirstName” of the current row |
| cells["First Name"] | Access the cell in the column called “First Name” of the current row |

Brackets can also be used to get substrings and sub-arrays:

|Example |Description |
|-|-|
| value[1,3] | A substring of value, starting from character 1 up to but excluding character 3 |
| "internationalization"[1,-2] | Will return “nternationalizati” (negative indexes are counted from the end) |
|row.columnNames[5]| Will return the name of the fifth column |

### Function reference

For the reference below, the function is given in full-length notation and the in-text examples are written in dot notation. Shorthands are used to indicate the kind of [data type](exploring#data-types) used in each function: s for string, b for boolean, n for number, d for date, a for array, as well as with “null” and “error.” If a function can take more than one kind of data as input or can output more than one kind of data, that is indicated with more than one letter (as with “snd”) or with o for object. We also use shorthands for substring (“sub”) and separator (“sep”). Optional arguments will say “(optional)”.


#### Boolean functions


###### and(boolean b1, boolean b2, ...etc)

Uses the logical operator AND on two or more booleans to yield a boolean. Evaluates multiple statements into booleans, then returns true if all of the statements are true. For example, `and(1 < 3, 1 < 0)` returns false because one condition is true and one is false.


###### or(boolean b1, boolean b2, ...etc)

Uses the logical operator OR on two or more booleans to yield a boolean. For example, `or(1 < 3, 1 > 7)` returns true because at least one of the conditions (the first one) is true.


###### not(boolean b)

Uses the logical operator NOT on a boolean to yield a boolean. For example, `not(1 > 7)` returns true because 1 > 7 itself is false.


###### xor(boolean b1, boolean b2, ...etc)

Uses the logical operator XOR (exclusive-or) on two or more booleans to yield a boolean. Evaluates multiple statements, then returns true if only one of them is true. For example, `xor(1 < 3, 1 < 7)` returns false because more than one of the conditions is true.


#### String functions


###### length(string s)

Returns the length of s as a number.


###### toString(o, string format (optional))

The toString function will work on any value type (string, number, date, boolean, error, null) and gives a string version of that value. You can convert between types within some limits (e.g. you can't turn the string "asdfsd" into a date or a number).

You can also use toString to convert numbers to strings with rounding, using the string format argument. For example, using the expression `value.toString("%.0f")` on a column:

|Input|Output|

|-|-|

|3.2|3|

|0.8|1|

|0.15|0|

|100.0|100|

You can also convert dates to strings, with something such as `value.toString("MMM-dd-yyyy")`. With this expression, the date value [2024-10-15T00:00:00Z]  would be converted to “Oct-15-2024”.

Note: In OpenRefine, using toString() on a null cell outputs the string "null".


##### Testing String Characteristics


###### startsWith(string s, string sub)

Returns a boolean indicating whether s starts with sub. For example, “food”.startsWith("foo") returns true, whereas “food”.startsWith("bar") returns false. 


###### endsWith(string s, string sub)

Returns boolean indicating whether s ends with sub. For example, "food".endsWith("ood") returns true, whereas “food”.endsWith("odd") returns false. 


###### contains(string s, string sub)

Returns boolean indicating whether s contains sub. For example, "food".contains("oo") returns true whereas "food".contains("ee") returns false.


###### indexOf(string s, string sub)

Returns integer indicating position of sub within s or -1 if sub is not found. For example, "food".indexOf("oo") returns 2, whereas "food".indexOf("ee") returns -1.

Returning -1 is equivalent to returning boolean false, which is very useful for finding strings that do **NOT** contain sub.


##### Basic String Modification


##### Case Conversion


###### toLowercase(string s)

Returns s converted to all lowercase characters.


###### toUppercase(string s)

Returns s converted to all uppercase characters.


###### toTitlecase(string s)

Returns s converted into titlecase. For example, toTitlecase("Once upon a midnight dreary") returns the string “Once Upon A Midnight Dreary”.


##### Trimming


###### trim(string s) 

Returns a copy of the string with leading and trailing whitespace removed. For example, " island ".trim() returns the string “island”.


###### strip(string s)

Returns a copy of the string with leading and trailing whitespace removed. For example, " island ".strip() returns the string “island”. Identical to trim().


###### chomp(string s, string sep)

Returns a copy of s with sep removed from the end if s ends with sep; otherwise, just returns s. For example, "hardly".chomp("ly") and "hard".chomp("ly") both return the string hard.



### Controls

### Constants

|Name |Meaning |
|-|-|
| true | the boolean constant true |
| false | the boolean constant false |
| PI | the constant of pi, from Java's Math.PI (i.e. 3.1415...) |


## Jython
*   Python syntax in OpenRefine
*   Installing Python packages

### Constants

|Name |Meaning |
|-|-|
| True | the boolean constant true |
| False | the boolean constant false |
| PI | the constant of pi, from Java's Math.PI (i.e. 3.1415...) |

if you return "true" the cell is populated with "1"; "false," "0."

## Clojure
*   Clojure syntax in OpenRefine
*   Using Java packages from Clojure