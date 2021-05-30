---
id: grel
title: General Refine Expression Language
sidebar_label: General Refine Expression Language
---

## Basics {#basics}

GREL (General Refine Expression Language) is designed to resemble Javascript. Formulas use variables and depend on data types to do things like string manipulation or mathematical calculations:

|Example|Output|
|---|---|
| `value + " (approved)"` | Concatenate two strings; whatever is in the cell gets converted to a string first |
| `value + 2.239`    | Add 2.239 to the existing value (if a number); append text "2.239" to the end of the string otherwise |
| `value.trim().length()` &nbsp; &nbsp; | Trim leading and trailing whitespace of the cell value and then output the length of the result |
| `value.substring(7, 10)` | Output the substring of the value from character index 7, 8, and 9 (excluding character index 10) |
| `value.substring(13)` | Output the substring from index 13 to the end of the string |

Note that the operator for string concatenation is `+` (not “&” as is used in Excel). 

Evaluating conditions uses symbols such as <, >, *, /, etc. To check whether two objects are equal, use two equal signs (`value=="true"`).

See the [GREL functions page for a thorough reference](grelfunctions) on each function and its inputs and outputs. Read on below for more about the general nature of GREL expressions.

## Syntax {#syntax}

In GREL, functions can use either of these two forms:
*   functionName(arg0, arg1, ...)
*   arg0.functionName(arg1, ...)

The second form is a shorthand to make expressions easier to read. It simply pulls the first argument out and appends it to the front of the function, with a dot:

|Dot notation |Full notation |
|-|-|
| `value.trim().length()` | `length(trim(value))` |
| `value.substring(7, 10)` | `substring(value, 7, 10)` |

So, in the dot shorthand, the functions occur from left to right in the order of calling, rather than in the reverse order with parentheses. This allows you to string together multiple functions in a readable order.

The dot notation can also be used to access the member fields of [variables](expressions#variables). For referring to column names that contain spaces (anything not a continuous string), use square brackets instead of dot notation:

|Example |Description |
|-|-|
| `FirstName.cells` | Access the cell in the column named “FirstName” of the current row |
| `cells["First Name"]` | Access the cell in the column called “First Name” of the current row |

Square brackets can also be used to get substrings and sub-arrays, and single items from arrays:

|Example |Description |
|-|-|
| `value[1,3]` | A substring of value, starting from character 1 up to but excluding character 3 |
| `"internationalization"[1,-2]` | Will return “nternationalizati” (negative indexes are counted from the end) |
| `row.columnNames[5]` | Will return the name of the fifth column |

Any function that outputs an array can use square brackets to select only one part of the array to output as a string (remember that the index of the items in an array starts with 0). 

For example, [partition()](grelfunctions#partitions-s-or-p-fragment-b-omitfragment-optional) would normally output an array of three items: the part before your chosen fragment, the fragment you've identified, and the part after. Selecting only the third part with `"internationalization".partition("nation")[2]` will output “alization” (and so will [-1], indicating the final item in the array).

## Controls {#controls}

GREL offers controls to support branching and looping (that is, “if” and “for” functions), but unlike functions, their arguments don't all get evaluated before they get run. A control can decide which part of the code to execute and can affect the environment bindings. Functions, on the other hand, can't do either. Each control decides which of their arguments to evaluate to `value`, and how.

Please note that the GREL control names are case-sensitive: for example, the isError() control can't be called with iserror().

#### if(e, eTrue, eFalse) {#ife-etrue-efalse}

Expression e is evaluated to a value. If that value is true, then expression eTrue is evaluated and the result is the value of the whole if() expression. Otherwise, expression eFalse is evaluated and that result is the value.

Examples:

| Example expression                                                           	| Result   	|
| ------------------------------------------------------------------------ | ------------ |
| `if("internationalization".length() > 10, "big string", "small string")` | “big string” |
| `if(mod(37, 2) == 0, "even", "odd")`                                 	| “odd”    	|

Nested if (switch case) example:

	if(value == 'Place', 'http://www.example.com/Location',

	 	if(value == 'Person', 'http://www.example.com/Agent',

	  	if(value == 'Book', 'http://www.example.com/Publication',

	null)))

#### with(e1, variable v, e2) {#withe1-variable-v-e2}

Evaluates expression e1 and binds its value to variable v. Then evaluates expression e2 and returns that result.

| Example expression                                                                       	| Result 	|
| ------------------------------------------------------------------------------------ | ---------- |
| `with("european union".split(" "), a, a.length())`                               	| 2   	|
| `with("european union".split(" "), a, forEach(a, v, v.length()))`                	| [ 8, 5 ] |
| `with("european union".split(" "), a, forEach(a, v, v.length()).sum() / a.length())` | 6.5  	|

#### filter(e1, v, e test) {#filtere1-v-e-test}

Evaluates expression e1 to an array. Then for each array element, binds its value to variable v, evaluates expression test - which should return a boolean. If the boolean is true, pushes v onto the result array.

| Expression                                 	| Result    	|
| ---------------------------------------------- | ------------- |
| `filter([ 3, 4, 8, 7, 9 ], v, mod(v, 2) == 1)` | [ 3, 7, 9 ] |

#### forEach(e1, v, e2) {#foreache1-v-e2}

Evaluates expression e1 to an array. Then for each array element, binds its value to variable v, evaluates expression e2, and pushes the result onto the result array.

| Expression                             	| Result          	|
| ------------------------------------------ | ------------------- |
| `forEach([ 3, 4, 8, 7, 9 ], v, mod(v, 2))` | [ 1, 0, 0, 1, 1 ] |

#### forEachIndex(e1, i, v, e2) {#foreachindexe1-i-v-e2}

Evaluates expression e1 to an array. Then for each array element, binds its index to variable i and its value to variable v, evaluates expression e2, and pushes the result onto the result array.

| Expression                                                                  	| Result                  	|
| ------------------------------------------------------------------------------- | --------------------------- |
| `forEachIndex([ "anne", "ben", "cindy" ], i, v, (i + 1) + ". " + v).join(", ")` | 1. anne, 2. ben, 3. cindy |

#### forRange(n from, n to, n step, v, e) {#forrangen-from-n-to-n-step-v-e}

Iterates over the variable v starting at from, incrementing by the value of step each time while less than to. At each iteration, evaluates expression e, and pushes the result onto the result array.

#### forNonBlank(e, v, eNonBlank, eBlank) {#fornonblanke-v-enonblank-eblank}

Evaluates expression e. If it is non-blank, forNonBlank() binds its value to variable v, evaluates expression eNonBlank and returns the result. Otherwise (if e evaluates to blank), forNonBlank() evaluates expression eBlank and returns that result instead.

Unlike other GREL functions beginning with “for,” forNonBlank() is not iterative. forNonBlank() essentially offers a shorter syntax to achieving the same outcome by using the isNonBlank() function within an “if” statement.

#### isBlank(e), isNonBlank(e), isNull(e), isNotNull(e), isNumeric(e), isError(e) {#isblanke-isnonblanke-isnulle-isnotnulle-isnumerice-iserrore}

Evaluates the expression e, and returns a boolean based on the named evaluation.

Examples:

| Expression      	| Result  |
| ------------------- | ------- |
| `isBlank("abc")`	| false |
| `isNonBlank("abc")`   | true |
| `isNull("abc")` 	| false |
| `isNotNull("abc")`	| true |
| `isNumeric(2)`  	| true  |
| `isError(1)`    	| false |
| `isError("abc")`	| false |
| `isError(1 / 0)`	| true  |

Remember that these are controls and not functions: you can’t use dot notation (for example, the format `e.isX()` will not work).

## Constants {#constants}
|Name |Meaning |
|-|-|
| true | The boolean constant true |
| false | The boolean constant false |
| PI | From [Java's Math.PI](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#PI), the value of pi (that is, 3.1415...) |