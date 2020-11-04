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

[Clojure](https://clojure.org/reference/reader) uses the same regex engine as Java, and can be invoked with [re-find](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/re-find), [re-matches](http://clojure.github.io/clojure/clojure.core-api.html#clojure.core/re-matches), etc. You can use the #"pattern" reader macro as described [in the Clojure documentation](https://clojure.org/reference/other_functions#regex). For example, to get the nth element of a returned sequence, you can use the nth function:

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

|Field |Meaning |
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

The `cells` object is used to call information from the columns in your project. For example, `cells.Foo` returns a [cell](#cell) object representing the cell in the column named “Foo” of the current row. If the column name has spaces, use square brackets, e.g., `cells["Postal Code"]`. There is no `cells.value` - it can only be used with member fields. To get the corresponding column value inside the `cells` variable, use `.value` at the end, for example `cells["Postal Code"].value`. 

### Cell

A `cell` object contains all the data of a cell and is stored as a single object that has two fields.

You can use `cell` on its own in the expressions editor to copy all the contents of a column to another column, including reconciliation information. Although the preview in the expressions editor will only show a small representation [object Cell], it will actually copy all the cell's data. Try this with <span class="menuItems">Edit Column</span> → <span class="menuItems">Add Column based on this column ...</span>.

|Field |Meaning |Member fields |
|-|-|-|
| cell | An object containing the entire contents of the cell | .value, .recon, .errorMessage |
| cell.value | The value in the cell, which can be a string, a number, a boolean, null, or an error |  |
| cell.recon | An object encapsulating reconciliation results for that cell | See the reconciliation section below |
| cell.errorMessage | Returns the message of an *EvalError* instead of the error object itself (use value to return the error object) | .value |

### Reconciliation

Several of the fields here are equivalent to what can be used through [reconciliation facets](reconciling#reconciliation-facets). You must type `cell.recon`; `recon` on its own will not work.

|Field|Meaning |Member fields |
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

|Field|Meaning |
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

If you're used to Excel, note that the operator for string concatenation is + (not &). Evaluating conditions uses symbols such as <, >, *, /, etc. To check whether two objects are equal, use two equal signs (`value=="true"`).

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

Brackets can also be used to get substrings and sub-arrays, and single items from arrays:

|Example |Description |
|-|-|
| value[1,3] | A substring of value, starting from character 1 up to but excluding character 3 |
| "internationalization"[1,-2] | Will return “nternationalizati” (negative indexes are counted from the end) |
|row.columnNames[5]| Will return the name of the fifth column |

Any function that outputs an array can use square brackets to select only one part of the array to output as a string (remember that the index of the items in an array starts with 0). For example, partition() would normally output an array of three items: the part before your chosen fragment, the fragment you've identified, and the part after. Selecting the third part with "internationalization".partition("nation")[2] will output “alization” (and so will [-1], indicating the final item in the array).

### Function reference

For the reference below, the function is given in full-length notation and the in-text examples are written in dot notation. Shorthands are used to indicate the kind of [data type](exploring#data-types) used in each function: s for string, b for boolean, n for number, d for date, a for array, as well as with “null” and “error.” 
If a function can take more than one kind of data as input or can output more than one kind of data, that is indicated with more than one letter (as with “snd”) or with o for object. 
We also use shorthands for substring (“sub”) and separator (“sep”). 
Optional arguments will say “(optional)”.
In places where OpenRefine will accept a string or a regex pattern, you can supply a string by putting it in quotes. If you wish to use any regex notation, wrap the pattern in forward slashes.

#### Boolean functions

###### and(b1, b2, ...)

Uses the logical operator AND on two or more booleans to yield a boolean. Evaluates multiple statements into booleans, then returns true if all of the statements are true. For example, `and(1 < 3, 1 < 0)` returns false because one condition is true and one is false.

###### or(b1, b2, ...)

Uses the logical operator OR on two or more booleans to yield a boolean. For example, `or(1 < 3, 1 > 7)` returns true because at least one of the conditions (the first one) is true.

###### not(b)

Uses the logical operator NOT on a boolean to yield a boolean. For example, `not(1 > 7)` returns true because 1 > 7 itself is false.

###### xor(b1, b2, ...)

Uses the logical operator XOR (exclusive-or) on two or more booleans to yield a boolean. Evaluates multiple statements, then returns true if only one of them is true. For example, `xor(1 < 3, 1 < 7)` returns false because more than one of the conditions is true.

#### String functions

###### length(s)

Returns the length of string s as a number.

###### toString(o, string format (optional))

Takes any value type (string, number, date, boolean, error, null) and gives a string version of that value. You can convert between types, within limits (for example, you can't turn the string "asdfsd" into a date or a number, but you can convert the number "123" into a string).

You can also use toString() to convert numbers to strings with rounding, using an optional string format. For example, if you applied the expression `value.toString("%.0f")` to a column:

|Input|Output|
|-|-|
|3.2|3|
|0.8|1|
|0.15|0|
|100.0|100|

You can also convert dates to strings, using date parsing syntax built in to OpenRefine (see [the toDate() function for details](#todateo-boolean-month_first--format1-format2--)). For example,  `value.toString("MMM-dd-yyyy")` would convert the date value [2024-10-15T00:00:00Z] to “Oct-15-2024”.

Note: In OpenRefine, using toString() on a null cell outputs the string "null".

##### Testing String Characteristics

###### endsWith(s, sub)

Returns a boolean indicating whether s ends with sub. For example, `"food".endsWith("ood")` returns true, whereas `“food”.endsWith("odd")` returns false. 

###### contains(s, sub or p)

Returns a boolean indicating whether s contains sub. For example, `"food".contains("oo")` returns true whereas `"food".contains("ee")` returns false. 

You can search for regex by wrapping a regex pattern in forward slashes rather than quotes: `"rose is a rose".contains(/\s+/)` returns true. startsWith() and endsWith() can only take strings, while contains() can take a regex pattern, so you can use contains() to look for beginning and ending string patterns.  

###### indexOf(s, sub)

Returns an integer indicating position of sub within s or -1 if sub is not found. For example, `"food".indexOf("oo")` returns 2, whereas `"food".indexOf("ee")` returns -1. Returning -1 is equivalent to returning boolean false, which is useful for finding strings that do not contain sub.

##### Basic String Modification

##### Case Conversion

###### toLowercase(s)

Returns string s converted to all lowercase characters.

###### toUppercase(s)

Returns string s converted to all uppercase characters.

###### toTitlecase(s)

Returns string s converted into titlecase. For example, `"Once upon a midnight dreary".toTitlecase()` returns the string “Once Upon A Midnight Dreary”.

##### Trimming

###### trim(s) 

Returns a copy of the string s with leading and trailing whitespace removed. For example, `" island ".trim()` returns the string “island”. Identical to strip().

###### strip(s)

Returns a copy of the string s with leading and trailing whitespace removed. For example, `" island ".strip()` returns the string “island”. Identical to trim().

###### chomp(s, sep)

Returns a copy of string s with the string sep removed from the end if s ends with sep; otherwise, just returns s. For example, `"hardly".chomp("ly")` and `"hard".chomp("ly")` both return the string hard.

##### Substring

###### substring(s, n from, n to (optional))

Returns the substring of s starting from character index from, and up to (excluding) character index to. If the to argument is omitted, substring will output to the end of s. For example, `"profound".substring(3)` returns the string “found”, and `"profound".substring(2, 4)` returns the string “of”.

Character indexes start from zero. Negative character indexes count from the end of the string. For example, `"profound".substring(1, -1)` returns the string rofoun.

###### slice(s, n from, n to (optional))

Identical to substring() in relation to strings. Also works with arrays; see [Array functions section](#slicea-n-from-n-to-optional).

###### get(s, n from, n to (optional))

Identical to substring() in relation to strings. Also works with named fields. Also works with arrays; see [Array functions section](#geta-n-or-s-from-n-to-optional).

##### Find and Replace

###### indexOf(s, sub)

Returns the first character index of sub as it first occurs in s; or, returns -1 if s does not contain sub. For example, `"internationalization".indexOf("nation")` returns 5, whereas `"internationalization".indexOf("world")` returns -1.

###### lastIndexOf(s, sub)

Returns the first character index of sub as it last occurs in s; or, returns -1 if s does not contain sub. For example, `"parallel".lastIndexOf("a")` returns 3 (pointing at the second "a").

###### replace(s, sp find, s replace)

Returns the string obtained by replacing the find string with the replace string in the inputted string. For example, `"The cow jumps over the moon and moos".replace("oo", "ee")` returns the string “The cow jumps over the meen and mees”. Find can be a regular expression; if so, replace can also contain capture groups declared in find. 

You cannot find or replace nulls with this, as null is not a string. You can instead:

1. Facet by null and then bulk-edit them to a string, or
2. Transform the column with an expression such as `if(value==null,'new',value)`

###### replaceChars(s, s find, s replace)

Returns the string obtained by replacing a character in s, identified by find, with the corresponding character identified in replace. For example, `"all the big cows lumber".replaceChars("aeiou", "A!IOU")` returns the string “All th! bIg cOws lUmb!r”. You cannot use this to replace a single character with more than one character.

###### match(s, p)

Attempts to match the string s in its entirety against the [regex](#grel-supported-regex) pattern p and, if it matches, returns an array of the desired substrings (found in order). For example, `"230.22398, 12.3480".match(/.*(\d\d\d\d)/)` returns an array of 1 substring: [3480]. It does not find 2239 as the first sequence with four digits, because the regex indicates the four digits must come at the end of the string.

You will need to convert the array to a string to capture it in a cell, with a function such as toString(). An empty array [] is output when there is no match to the desired substrings. A null is output when the entire regex does not match.

Remember to enclose your regex in forward slashes, and to cancel out characters and use parentheses as needed. Parentheses are required to denote a desired substring; for example, “.*(\d\d\d\d)” would return one array value, while “(.*)(\d\d\d\d)” would return two. So, if you are looking for a desired substring anywhere within a string, use the syntax `value.match(/.*(desired-substring-regex).*/)`.

For another example, if the cell contains value “hello 123456 goodbye”:

|Expression|Result|
|-|-|
|value.match(/\d{6}/) |null (does not match the full string)|
|value.match(/.*\d{6}.*/) |[ ] (no indicated substring)|
|value.match(/.*(\d{6}).*/) |[ "123456" ] (array with one value)|
|value.match(/(.*)(\d{6})(.*)/) |[ "hello ", "123456", " goodbye" ] (array with three values)|

###### find(s, p)

Outputs, into an array, all consecutive substrings inside string s that match the [regex](#grel-supported-regex) pattern p. Unlike match(), find() can return several occurrences of the same pattern in a string, because it is not evaluating the regex against the string in its entirety. For example, `"abeadsabmoloei".find(/[aeio]+/)` would result in [ "a", "ea", "a", "o", "oei" ].

You can supply a string instead of p, by putting it in quotes, and OpenRefine will compile it into a regex pattern. Anytime you supply quotes, OpenRefine interprets the contents as a string, not regex. If you wish to use any regex notation, wrap the pattern in forward slashes, for example: `"OpenRefine is Awesome".find(/fine\sis/)` would return [ “fine is” ].

##### String Parsing and Splitting

###### toNumber(o)

Returns o converted to a number.

###### split(s, sp sep)

Returns the array of strings obtained by splitting s by the separator sep. The sep can be either a string or a regex pattern. For example, `"fire, water, earth, air".split(",")` returns an array of 4 strings: [ "fire", " water", " earth”, “ air” ]. Note that the space characters are retained but the separator is removed.

###### splitByLengths(s, n1, n2, ...)

Returns the array of strings obtained by splitting s into substrings with the given lengths. For example, `"internationalization".splitByLengths(5, 6, 3)` returns an array of 3 strings: [ inter, nation, ali ]. Excess characters are discarded.

###### smartSplit(s, sp sep (optional))

Returns the array of strings obtained by splitting s by the separator sep, or by guessing either tab or comma separation if there is no sep given. Handles quotes properly. The sep can be either a string or a regex pattern. For example, `value.smartSplit("\n")` will split at a carriage return or a new-line character.

Note: `value.[escape](#escapes-s-mode)('javascript')` is useful for previewing unprintable characters prior to using smartSplit().

###### splitByCharType(s)

Returns an array of strings obtained by splitting s into groups of consecutive characters each time the characters change unicode types. For example, `“HenryCTaylor”.splitByCharType()` will result in an array of [ H, enry, CT, aylor ].

It is useful for separating letters and numbers: `"BE1A3E".splitByCharType()` will result in [ BE, 1, A, 3, E ].

###### partition(s, sp fragment, b omitFragment (optional))

Returns an array of strings [ a, fragment, z ] where a is the substring within s before the first occurrence of fragment, and z is the substring after fragment. Fragment can be a string or a regex. For example, `"internationalization".partition("nation")` returns 3 strings: [ inter, nation, alization ]. If s does not contain fragment, it returns an array of [ s, "", "" ] (the original unpartitioned string, and two empty strings). 

If the omitFragment boolean is true, for example with `"internationalization".partition("nation", true)`, the fragment is not returned. The output is [ "inter", "alization" ].

You can use regex for your fragment. The expresion `"abcdefgh".partition(/c.e/)` will output  [“abc”, "cde", defgh” ]. 

###### rpartition(s, sp fragment, b omitFragment (optional))

Returns an array of strings [ a, fragment, z ] where a is the substring within s before the last occurrence of fragment, and z is the substring after the last instance of fragment. (Rpartition means “reverse partition.”) For example, `"parallel".rpartition("a")` returns 3 strings: [ par, a, llel ]. 

Otherwise works identically to partition() above.

##### Encoding and Hashing

###### diff(sd1, sd2, s timeUnit (optional))

Takes two strings or two dates and compares them, returning a string. The two objects must be the same data type. For strings, diff() returns the remainder of s2 starting with the first character where they differ. For example, `diff("cacti", "cactus")` returns "us". For dates, see [Date functions](#diffd1-d2-s-timeunit).

###### escape(s, s mode)

Escapes s in the given escaping mode. The mode can be one of: html, xml, csv, url, javascript. See the [recipe here](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#question-marks--showing-in-your-data) for an example.

###### unescape(s, s mode)

Unescapes s in the given escaping mode. The mode can be one of: html, xml, csv, url, javascript. See the [recipe here](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#atampampt----att) for an example. 

###### md5(o)

Returns the [MD5 hash](https://en.wikipedia.org/wiki/MD5) of an object. If fed something other than a string (array, number, date, etc.), md5() will convert it to a string and deliver the hash of the string. For example, `"[ and, or, not ]".md5()` will return 80fd34c2da7787a20c6c5e32e4899459.

###### sha1(o)

Returns the [SHA-1 hash](https://en.wikipedia.org/wiki/SHA-1) of an object. If fed something other than a string (array, number, date, etc.), sha1() will convert it to a string and deliver the hash of the string. For example, `"[ and, or, not ]".sha1()` will return a6664fc5476a043cabc179da1e3ce736e3959bbc.

###### phonetic(s, s encoding)

Returns a phonetic encoding of a string, based on an available phonetic algorithm. See the [section on phonetic clustering](cellediting#clustering-methods) for more information. Can be one of the following supported phonetic methods: [metaphone, doublemetaphone, metaphone3](https://www.wikipedia.org/wiki/Metaphone), [soundex](https://en.wikipedia.org/wiki/Soundex), [cologne](https://en.wikipedia.org/wiki/Cologne_phonetics). For example, “Ruth Prawer Jhabvala“.phonetic(metaphone3) outputs the string “R0PRRJPF”.

###### reinterpret(s, s encoder)

Returns s reinterpreted through the given character encoder. You must supply one of the [supported encodings](http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html). When an OpenRefine project is started, data is imported and interpreted. A specific character encoding is identified or manually selected at that time (such as UTF-8). You can reinterpret a column into another specificed encoding using this function. This function may not fix your data; it may be better to use this in conjunction with new projects to test the interpretation, and pre-format your data as needed. 

###### fingerprint(s)

Returns the fingerprint of s, a string that is the first step in [fingerprint clustering methods](cellediting#clustering-methods): it will trim whitespaces, convert all characters to lowercase, remove punctuation, sort words alphabetically, etc. For example, `“Ruth  Prawer    Jhabvala”.fingerprint()` outputs the string “jhabvala prawer ruth”.

###### ngram(s, n)

Returns an array of the word n-grams of s. That is, it lists all the possible consecutive combinations of n words in the string. For example, `“Ruth Prawer Jhabvala“.ngram(2)` would output the array [ "Ruth Prawer", "Prawer Jhabvala" ]. A word n-gram of 1 simply lists all the words in original order; a larger n-gram will only return the original string inside an array (e.g. `“Ruth Prawer Jhabvala“.ngram(4)` would simply return [“Ruth Prawer Jhabvala“]).

###### ngramFingerprint(s, n)

Returns the [n-gram fingerprint](cellediting#clustering-methods) of s. For example, `“banana”.ngram(2)` would output “anbana”, after first generating the 2-grams “ba an na an na”, removing duplicates, and sorting them alphabetically.

###### unicode(s)

Returns an array of strings describing each character of s in their full unicode notation. For example, `“Bernice Rubens”.unicode()` outputs [ 66, 101, 114, 110, 105, 99, 101, 32, 82, 117, 98, 101, 110, 115 ].

###### unicodeType(s)

Returns an array of strings describing each character of s by their unicode type. For example, `“Bernice Rubens”.unicodeType()` outputs [ "uppercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "space separator", "uppercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter" ].

#### Format-based functions (JSON, HTML, XML)

###### jsonize(o)

Quotes a value as a JSON literal value.

###### parseJson(s)

Parses a string as JSON. get() can then be used with parseJson(): for example, `parseJson(" { 'a' : 1 } ").get("a")` returns 1.

For example from the following JSON array, let's get all instancescalled "keywords" having the same object string name of "text", combine with the forEach() function to iterate over the array.

    {
       "status":"OK",
       "url":"",
       "language":"english",
       "keywords":[
          {
             "text":"York en route",
             "relevance":"0.974363"
          },
          {
             "text":"Anthony Eden",
             "relevance":"0.814394"
          },
          {
             "text":"President Eisenhower",
             "relevance":"0.700189"
          }
       ]
    }

The GREL expression `forEach(value.parseJson().keywords,v,v.text).join(":::")` will output "York en route:::Anthony Eden:::President Eisenhower".

##### Jsoup XML and HTML parsing functions

###### parseHtml(s)
Given a cell full of HTML-formatted text, simplifies HTML tags (such as by removing “ /” at the end of self-closing tags), closes any unclosed tags, and inserts linebreaks and indents for cleaner code. You cannot pass parseHtml() a URL, but you can pre-fetch HTML with the “Add column by fetching URLs” menu option. A cell cannot store the output of parseHtml() unless you convert it with toString(). 

When parseHtml() simplifies HTML, it can sometimes introduce errors. When closing tags, it makes its best guesses based on line breaks, indentation, and the presence of other tags. You may need to manually check the results. 

You can then extract or select() which portions of the HTML document you need for further splitting, partitioning, etc. An example of extracting all table rows from a div using parseHtml().select() together is described more in depth at [StrippingHTML](https://github.com/OpenRefine/OpenRefine/wiki/StrippingHTML).

###### parseXml(s)
Given a cell full of XML-formatted text, returns a full XML document and adds any missing closing tags. You can then extract or select() which portions of the XML document you need for further splitting, partitioning, etc. Functions the same way as parseHtml() is described above. 

###### select(s, element)
Returns an array of all the desired elements from an HTML or XML document, if the element exists. Elements are identified using the [Jsoup selector syntax](https://jsoup.org/apidocs/org/jsoup/select/Selector.html). For example, `value.parseHtml().select(“img.portrait”)[0]` would return the entirety of the first "img" tag with the “portrait” class found in the parsed HTML inside `value`. Returns an empty array if no matching element is found. Use with toString() to capture the results in a cell. A tutorial of select() is shown in [StrippingHTML](https://github.com/OpenRefine/OpenRefine/wiki/StrippingHTML).

You can use select() more than once:

```
value.parseHtml().select("div#content")[0].select("tr").toString()
```

###### htmlAttr(s, element)
Returns a string from an attribute on an HTML element. Use it in conjunction with parseHtml() as in the following example: `value.parseHtml().select("a.email")[0].htmlAttr("href")`.

###### xmlAttr(s, element)
Returns a string from an attribute on an XML element. Function the same way htmlAttr() is described above. Use it in conjunction with parseXml().

###### htmlText(element)
Returns a string of the text from within an HTML element (including all child elements), removing HTML tags and line breaks inside the string. Use it in conjunction with parseHtml() and select() to provide an element, as in the following example: `value.parseHtml().select("div.footer")[0].htmlText()`. 

###### xmlText(element)
Returns a string of the text from within an XML element (including all child elements). Functions the same way as htmlText() is described above. Use it in conjunction with parseXml() and select() to provide an element.

###### wholeText(element)
_Works from OpenRefine 3.4.1 beta 644 onwards only_
Selects the (unencoded) text of an element and its children, including any newlines and spaces, and returns a string of unencoded, un-normalized text. Use it in conjunction with parseHtml() and select() to provide an element as in the following example: `value.parseHtml().select("div.footer")[0].wholeText()`.

###### innerHtml(element)
Returns the [inner HTML](https://developer.mozilla.org/en-US/docs/Web/API/Element/innerHTML) of an HTML element. This will include text and children elements within the element selected. Use it in conjunction with parseHtml() and select() to provide an element.

###### innerXml(element)
Returns all the inner XML elements inside your chosen XML element. Does not return the text directly inside your chosen XML element - only the contents of its children. To select the direct text, use ownText(). To select both, use xmlText(). Use it in conjunction with parseXml() and select() to provide an element.

###### outerHtml(element)
Returns the [outer HTML](https://developer.mozilla.org/en-US/docs/Web/API/Element/outerHTML) of an HTML element. outerHtml includes the start and end tags of the current element. Use it in conjunction with parseHtml() and select() to provide an element.

###### ownText(element)
Returns the text directly inside the selected XML or HTML element only, ignoring text inside children elements. Use it in conjunction with a parser and select() to provide an element.

#### Array functions

###### length(a)
Returns the length of an array, meaning the number of objects inside it. Arrays can be empty, in which case length() will return 0. 

###### slice(a, n from, n to (optional))
Returns a sub-array of a given array, from the first index provided and up to and excluding the optional last index provided. Remember that array objects are indexed starting at 0. If to is omitted, it is understood to be the end of the array. For example, `[0, 1, 2, 3, 4].slice(1, 3)` returns [ 1, 2 ], and `[ 0, 1, 2, 3, 4].slice(1)` returns [ 1, 2, 3, 4 ]. Also works with strings; see [String functions](#slices-n-from-n-to-optional).

###### get(o, n from, n to (optional))
Returns a sub-array of a given array, from the first index provided and up to and excluding the optional last index provided. Remember that array objects are indexed starting at 0. If to is omitted, only one array item is returned, as a string, instead of a sub-array. To return a sub-array from one index to the end, you can set the to argument to a very high number, such as get(2,9999). For example: `["A","B","C","D","E"].get(1,4)` returns the value [ "B", "C", "D" ]. `[1,2,3,4].get(2)` returns the value 3.

Also works with strings; see [String functions](#gets-n-or-s-from-n-to-optional).

###### inArray(a, s)
Returns true if the array contains the desired string, and false otherwise.

###### reverse(a)
Reverses the array. For example, `[ 0, 1, 2, 3].reverse()` returns the array [ 3, 2, 1, 0 ].

###### sort(a)
Sorts the array in ascending order. Sorting is case-sensitive, uppercase first and lowercase second. For example, `[ "al", "Joe", "Bob", "jim" ].sort()` returns the array [ "Bob", "Joe", "al", "jim" ]. 

###### sum(a)
Return the sum of the numbers in the array a. For example, `[ 2, 1, 0, 3].sum()` returns 6.

###### join(a, sep)
Joins the items in the array with the separator string, and returns it all as a string. For example, `[ "and", "or", "not" ].join("/")` returns the string “and/or/not”.

###### uniques(a)
Returns the array with duplicates removed. Case-sensitive. For example, `[ "al", "Joe", "Bob", "Joe", "Al", "Bob" ].uniques()` returns the array [ "Joe", "al", "Al", "Bob" ]. 

As of OpenRefine 3.4.1, uniques() reorders the array items it returns; in 3.4 beta 644 and onwards, it preserves the original order (in this case, [ "al", "Joe", "Bob", "Al" ]). 

#### Date functions
If you have a column full of dates in a more common format (such as yyyy/mm/dd or YYYYMMDD) you can convert these using “Transform” > “Common transforms …” > “to date,” or the GREL function toDate(). You may wish to create a new column with the transformation, in order to also preserve the more human-readable version.

OpenRefine uses [Date.parse()](https://www.w3schools.com/jsref/jsref_parse.asp) to recognize a variety of formats and convert them, including converting from other time zones to UTC:

![A screenshot of different date formats being converted, and one error.](/img/dates.png)

You may need to do some reformatting if your dates are not being recognized by the toDate() function. For example, in the image above, the date that includes "7AM" is giving an error message, but the ones with "2:42 PM" and "3:22PM" are being converted.

###### now()

Returns the current time in the [ISO 8601 extended format](exploring#data-types). For example, 12:58pm on October 28th 2020 returns [date 2020-10-28T16:58:32Z].

###### toDate(o, boolean monthFirst / format1, format2, ... )

Returns the inputted object converted to a date object. Without arguments, it returns the ISO 8601 extended format. With arguments, you can control the output format:
*   monthFirst: set false if the date is formatted with the day before the month.
*   formatN: attempt to parse the date using an ordered list of possible formats. Supply formats based on the [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) syntax (and see the table below for a handy reference). 

For example, you can parse a column containing dates in different formats, such as cells with "Nov-09" and "11/09", using `value.toDate('MM/yy','MMM-yy').toString('yyyy-MM')` and both will output “2009-11”. For another example, "1/4/2012 13:30:00" can be parsed into a date using `value.toDate('d/M/y H&#58;m&#58;s')`.

| Letter | Date or Time Component | Presentation | Examples |
|-|-|-|-|
| G | Era designator | Text | AD |
| y | Year | Year | 1996; 96 |
| Y | [Week year](https://en.wikipedia.org/wiki/ISO_week_date#First_week) | Year | 2009; 09 |
| M | Month in year | Month | July; Jul; 07 |
| w | Week in year | Number | 27 |
| W | Week in month | Number | 2 |
| D | Day in year | Number | 189 |
| d | Day in month | Number | 10 |
| F | Day of week in month | Number | 2 |
| E | Day name in week | Text | Tuesday; Tue |
| u | Day number of week (1 = Monday, ..., 7 = Sunday) | Number | 1 |
| a | AM/PM marker | Text | PM |
| H | Hour in day (0-23) | Number | 0 |
| k | Hour in day (1-24) | Number | 24 |
| K | Hour in AM/PM (0-11) | Number | 0 |
| h | Hour in AM/PM (1-12) | Number | 12 |
| m | Minute in hour | Number | 30 |
| s | Second in minute | Number | 55 |
| S | Millisecond | Number | 978 |
| n | Nanosecond | Number | 789000 |
| z | Time zone | General time zone | Pacific Standard Time; PST; GMT-08:00 |
| Z | Time zone | RFC 822 time zone | \-0800 |
| X | Time zone | ISO 8601 time zone | \-08; -0800; -08:00 |

###### diff(d1, d2, s timeUnit)

Given two dates, returns a number indicating the difference in a given time unit (see the table below). For example, `diff(("Nov-11".toDate('MMM-yy')), ("Nov-09".toDate('MMM-yy')), "weeks")` will return 104, for 104 weeks, or two years. The later date should go first. If the output is negative, invert d1 and d2.

###### inc(d, n, s timeUnit)

Returns a date changed by the given amount in the given unit of time (see the table below). The default unit is “hour.” For example, if you want to move a date backwards by two months, use `value.inc(-2,'month')`.

###### datePart(d, s timeUnit)

Returns part of a date. Data type returned depends on the unit (see the table below). 

OpenRefine supports the following values for timeUnit:

| Unit | Date part returned | Returned data type | Example using [date 2014-03-14T05:30:04.000789000Z] as value |
|-|-|-|-|
| years | Year | Number | value.datePart("years") -> 2014 |
| year | Year | Number | value.datePart("year") -> 2014 |
| months | Month | Number | value.datePart("months") -> 2 |
| month | Month | Number | value.datePart("month") -> 2 |
| weeks | Week of the month | Number | value.datePart("weeks") -> 3 |
| week | Week of the month | Number | value.datePart("week") -> 3 |
| w | Week of the month | Number | value.datePart("w") -> 3 |
| weekday | Day of the week | String | value.datePart("weekday") -> Friday |
| hours | Hour | Number | value.datePart("hours") -> 5 |
| hour | Hour | Number | value.datePart("hour") -> 5 |
| h | Hour | Number | value.datePart("h") -> 5 |
| minutes | Minute | Number | value.datePart("minutes") -> 30 |
| minute | Minute | Number | value.datePart("minute") -> 30 |
| min | Minute | Number | value.datePart("min") -> 30 |
| seconds | Seconds | Number | value.datePart("seconds") -> 04 |
| sec | Seconds | Number | value.datePart("sec") -> 04 |
| s | Seconds | Number | value.datePart("s") -> 04 |
| milliseconds | Millseconds | Number | value.datePart("milliseconds") -> 789 |
| ms | Millseconds | Number | value.datePart("ms") -> 789 |
| S | Millseconds | Number | value.datePart("S") -> 789 |
| n | Nanoseconds | Number | value.datePart("n") -> 789000 |
| nano | Nanoseconds | Number | value.datePart("n") -> 789000 |
| nanos | Nanoseconds | Number | value.datePart("n") -> 789000 |
| time | Date expressed as milliseconds since the Unix Epoch | Number | value.datePart("time") -> 1394775004000 |

#### Math functions

For integer division and precision, you can use simple evaluations such as `1 / 2`, which is equivalent to `floor(1/2)` - that is, it returns only whole number results. If either operand is a floating point number, they both get promoted to floating point and a floating point result is returned. You can use `1 / 2.0` or `1.0 / 2` or `1.0 * x / y` (if you're working with variables of unknown contents).

###### floor(n)
Returns the floor of a number. For example, `3.7.floor()` returns 3 and `-3.7.floor()` returns -4.

###### ceil(n)
Returns the ceiling of a number. For example, `3.7.ceil()` returns 4 and `-3.7.ceil()` returns -3.

###### round(n)
Rounds a number to the nearest integer. For example, `3.7.round()` returns 4 and `-3.7.round()` returns -4.

###### min(n1, n2)
Returns the smaller of two numbers.

###### max(n1, n2)
Returns the larger of two numbers.

###### mod(n1, n2)
Returns n1 modulus n2. For example, mod(74, 9) returns 2. Note: `value.mod(9)` will work, whereas `74.mod(value)` will not work.

###### ln(n)
Returns the [natural logarithm](https://en.wikipedia.org/wiki/Natural_logarithm) of n.

###### log(n)
Returns the base 10 logarithm of n.

###### exp(n)
Returns [e](https://en.wikipedia.org/wiki/E_(mathematical_constant)) raised to the power of n.

###### pow(n1, n2)
Returns n1 raised to the power of n2. For example, `pow(2, 3)` returns 8 (2 cubed) and `pow(3, 2)` returns 9 (3 squared). The square root of any numeric value can be called with `value.pow(0.5)`. Note: `value.mod(0.5)` will work, whereas `74.mod(value)` will not work.

###### randomNumber(n lowerBound, n upperBound)
Returns a random integer in the interval between the lower and upper bounds (inclusively). Will output a different random number in each cell in a column.

#### Other functions

###### type(o)
Returns a string with the data type of o, such as undefined, string, number, boolean, etc. For example, a Transform operation using `value.type()` will convert all cells in a column to strings of their data types.

###### facetCount(choiceValue, s facetExpression, s columnName)
Returns the facet count corresponding to the given choice value, by looking for the facetExpression in the choiceValue in columnName. For example, to create facet counts for the following table, we could generate a new column based on “Gift” and enter in `value.facetCount("value", "Gift")`. This would add the column we've named “Count”:

| Gift | Recipient | Price | Count |
|-|-|-|-|
| lamp | Mary | 20 | 1 |
| clock | John | 57 | 2 |
| watch | Amit | 80 | 1 |
| clock | Claire | 62 | 2 |

The facet expression, wrapped in quotes, can be useful to manipulate the inputted values before counting. For example, you could create a numeric facet that rounds the price to the nearest $10, then counts: `(round(value / 10.0)*10).facetCount("round(value / 10.0)*10","Price")`. This would count 1 value at 20, 2 at 60, 1 at 80, and 2 at 60.

###### hasField(o, s name)
Returns a boolean indicating whether o has a member field called name. For example, `cell.recon.hasField(“match”)` will return false if a reconciliation match hasn’t been selected yet, or true if it does. You cannot chain your desired fields: for example, `cell.hasField(“recon.match”)` will return false even if the above expression returns true).

###### coalesce(o1, o2, o3, ...)
Returns the first non-null from a series of values of any kind. For example, `coalesce(value, "")` would return an empty string "" if the value was null, but otherwise return the value.

###### cross(cell c, s projectName, s columnName)
Returns an array of zero or more rows in the project projectName for which the cells in their column columnName have the same content as cell c.

### Controls

GREL offers controls to support branching and looping (that is, “if” and “for” functions), but unlike functions, their arguments don't all get evaluated before they get run. A control can decide which part of the code to execute and can affect the environment bindings. Functions, on the other hand, can't do either. Each control decides which of their arguments to evaluate to value, and how.

Please note that the GREL control names are case-sensitive: for example, the isError() control can't be called with iserror().

#### if(e, expression eTrue, expression eFalse)

Expression o is evaluated to a value. If that value is true, then expression eTrue is evaluated and the result is the value of the whole `if` expression. Otherwise, expression eFalse is evaluated and that result is the value.

Examples:

| Example expression                                                           	| Result   	|
| ------------------------------------------------------------------------ | ------------ |
| `if("internationalization".length() > 10, "big string", "small string")` | big string |
| `if(mod(37, 2) == 0, "even", "odd")`                                 	| odd    	|

Nested if (switch case) example:

	if(value == 'Place', 'http://www.example.com/Location',

	 	if(value == 'Person', 'http://www.example.com/Agent',

	  	if(value == 'Book', 'http://www.example.com/Publication',

	null)))

#### with(e1, variable v, e2)

Evaluates expression e1 and binds its value to variable v. Then evaluates expression e2 and returns that result.

| Example expression                                                                       	| Result 	|
| ------------------------------------------------------------------------------------ | ---------- |
| `with("european union".split(" "), a, a.length())`                               	| 2   	|
| `with("european union".split(" "), a, forEach(a, v, v.length()))`                	| [ 8, 5 ] |
| `with("european union".split(" "), a, forEach(a, v, v.length()).sum() / a.length())` | 6.5  	|

#### filter(e1, variable v, e test)

Evaluates expression e1 to an array. Then for each array element, binds its value to variable v, evaluates expression test - which should return a boolean. If the boolean is true, pushes v onto the result array.

| Expression                                 	| Result    	|
| ---------------------------------------------- | ------------- |
| `filter([ 3, 4, 8, 7, 9 ], v, mod(v, 2) == 1)` | [ 3, 7, 9 ] |

#### forEach(e1, variable v, e2)

Evaluates expression e1 to an array. Then for each array element, binds its value to variable v, evaluates expression e2, and pushes the result onto the result array.

| Expression                             	| Result          	|
| ------------------------------------------ | ------------------- |
| `forEach([ 3, 4, 8, 7, 9 ], v, mod(v, 2))` | [ 1, 0, 0, 1, 1 ] |

#### forEachIndex(e1, variable i, variable v, e2)

Evaluates expression e1 to an array. Then for each array element, binds its index to variable i and its value to variable v, evaluates expression e2, and pushes the result onto the result array.

| Expression                                                                  	| Result                  	|
| ------------------------------------------------------------------------------- | --------------------------- |
| `forEachIndex([ "anne", "ben", "cindy" ], i, v, (i + 1) + ". " + v).join(", ")` | 1. anne, 2. ben, 3. cindy |

#### forRange(n from, n to, n step, variable v, e)

Iterates over the variable v starting at from, incrementing by step each time while less than to. At each iteration, evaluates expression e, and pushes the result onto the result array.

#### forNonBlank(e, variable v, expression eNonBlank, expression eBlank)

Evaluates expression e. If it is non-blank, forNonBlank() binds its value to variable v, evaluates expression eNonBlank and returns the result. Otherwise (if o evaluates to blank), forNonBlank() evaluates expression eBlank and returns that result instead.

Unlike other GREL functions beginning with "for", forNonBlank() is not iterative. forNonBlank() essentially offers a shorter syntax to achieving the same outcome by using the isNonBlank() function within an "if" statement.

#### isBlank(e), isNonBlank(e), isNull(e), isNotNull(e), isNumeric(e), isError(e)

Evaluates the expression e, and returns a boolean based on the named evaluation.

Examples:

| Expression      	| Result  |
| ------------------- | ------- |
| `isBlank("abc")`	| false |
| `isNonBlank("abc")` | true |
| `isNull("abc")` 	| false |
| `isNotNull("abc")`	| true |
| `isNumeric(2)`  	| true  |
| `isError(1)`    	| false |
| `isError("abc")`	| false |
| `isError(1 / 0)`	| true  |

Remember that these are controls and not functions. So you can’t use dot notation (the `e.isX()` syntax).

### Constants
|Name |Meaning |
|-|-|
| true | The boolean constant true |
| false | The boolean constant false |
| PI | From [Java's Math.PI](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#PI), the value of pi (that is, 3.1415...) |

## Jython

Jython 2.7.2 comes bundled with the default installation of OpenRefine 3.4.1. [You can add libraries and code by following this tutorial](https://github.com/OpenRefine/OpenRefine/wiki/Extending-Jython-with-pypi-modules). You will find that a large number of Python files (.py or .pyc) are compatible. Python code that depends on C bindings will not work in OpenRefine, which uses Java / Jython only. Since Jython is essentially Java, you can also import Java libraries and utilize those. Remember to restart OpenRefine, so that new Jython/Python libraries are initialized during Butterfly's startup.

OpenRefine now has [most of the Jsoup.org library built into GREL functions](#jsoup-xml-and-html-parsing-functions), for parsing and working with HTML elements and extraction.

### Syntax

Expressions in Jython must have a `return` statement:

```
  return value[1:-1]
```

```
  return rowIndex%2
```

Fields have to be accessed using the bracket operator rather than the dot operator:

```
  return cells["col1"]["value"]
```

To access the Levenshtein distance between the reconciled value and the cell value (?) use the [recon variables](#reconciliation):

```
  return cell["recon"]["features"]["nameLevenshtein"]
```

To return the lower case of value (if the value is not null):

```
  if value is not None:
    return value.lower()
  else:
    return None
```

### Tutorials
- [Extending Jython with pypi modules](https://github.com/OpenRefine/OpenRefine/wiki/Extending-Jython-with-pypi-modules)
- [Working with Phone numbers using Java libraries inside Python](https://github.com/OpenRefine/OpenRefine/wiki/Jython#tutorial---working-with-phone-numbers-using-java-libraries-inside-python)

Full documentation on the Jython language can be found on its official site [http://www.jython.org](http://www.jython.org).

## Clojure

Clojure 1.10.1 comes bundled with the default installation of OpenRefine 3.4.1. 

For a guide to Clojure syntax, see the [Clojure website's guide to syntax](https://clojure.org/guides/learn/syntax).

Full documentation on the Clojure language can be found on its official site [https://clojure.org/](https://clojure.org/).
