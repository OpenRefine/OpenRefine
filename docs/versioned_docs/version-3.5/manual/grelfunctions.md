---
id: grelfunctions
title: GREL functions
sidebar_label: GREL functions
---

## Reading this reference {#reading-this-reference}

For the reference below, the function is given in full-length notation and the in-text examples are written in dot notation. Shorthands are used to indicate the kind of [data type](exploring#data-types) used in each function: s for string, b for boolean, n for number, d for date, a for array, p for a regex pattern, and o for object (meaning any data type), as well as “null” and “error” data types. 

If a function can take more than one kind of data as input or can output more than one kind of data, that is indicated with more than one letter (as with “s or a”) or with o for object, meaning it can take any type of data (string, boolean, date, number, etc.). 

We also use shorthands for substring (“sub”) and separator string (“sep”). 
Optional arguments will say “(optional)”.

In places where OpenRefine will accept a string (s) or a regex pattern (p), you can supply a string by putting it in quotes. If you wish to use any [regex](expressions#regular-expressions) notation, wrap the pattern in forward slashes.

## Boolean functions {#boolean-functions}

###### and(b1, b2, ...) {#andb1-b2-}

Uses the logical operator AND on two or more booleans to output a boolean. Evaluates multiple statements into booleans, then returns true if all of the statements are true. For example, `(1 < 3).and(1 < 0)` returns false because one condition is true and one is false.

###### or(b1, b2, ...) {#orb1-b2-}

Uses the logical operator OR on two or more booleans to output a boolean. For example, `(1 < 3).or(1 > 7)` returns true because at least one of the conditions (the first one) is true.

###### not(b) {#notb}

Uses the logical operator NOT on a boolean to output a boolean. For example, `not(1 > 7)` returns true because 1 > 7 itself is false.

###### xor(b1, b2, ...) {#xorb1-b2-}

Uses the logical operator XOR (exclusive-or) on two or more booleans to output a boolean. Evaluates multiple statements, then returns true if only one of them is true. For example, `(1 < 3).xor(1 < 7)` returns false because more than one of the conditions is true.

## String functions {#string-functions}

###### length(s) {#lengths}

Returns the length of string s as a number.

###### toString(o, string format (optional)) {#tostringo-string-format-optional}

Takes any value type (string, number, date, boolean, error, null) and gives a string version of that value. 

You can use toString() to convert numbers to strings with rounding, using an [optional string format](https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html). For example, if you applied the expression `value.toString("%.0f")` to a column:

|Input|Output|
|-|-|
|3.2|3|
|0.8|1|
|0.15|0|
|100.0|100|

You can also convert dates to strings, using date parsing syntax built into OpenRefine (see [the toDate() function for details](#todateo-b-monthfirst-s-format1-s-format2-)). For example, `value.toString("MMM-dd-yyyy")` would convert the date value [2024-10-15T00:00:00Z] to “Oct-15-2024”.

Note: In OpenRefine, using toString() on a null cell outputs the string “null”.

### Testing string characteristics {#testing-string-characteristics}

###### startsWith(s, sub) {#startswiths-sub}

Returns a boolean indicating whether s starts with sub. For example, `"food".startsWith("foo")` returns true, whereas `"food".startsWith("bar")` returns false. 

###### endsWith(s, sub) {#endswiths-sub}

Returns a boolean indicating whether s ends with sub. For example, `"food".endsWith("ood")` returns true, whereas `"food".endsWith("odd")` returns false. 

###### contains(s, sub or p) {#containss-sub-or-p}

Returns a boolean indicating whether s contains sub, which is either a substring or a regex pattern. For example, `"food".contains("oo")` returns true whereas `"food".contains("ee")` returns false. 

You can search for a regular expression by wrapping it in forward slashes rather than quotes: `"rose is a rose".contains(/\s+/)` returns true. startsWith() and endsWith() can only take strings, while contains() can take a regex pattern, so you can use contains() to look for beginning and ending string patterns.  

### Basic string modification {#basic-string-modification}

#### Case conversion {#case-conversion}

###### toLowercase(s) {#tolowercases}

Returns string s converted to all lowercase characters.

###### toUppercase(s) {#touppercases}

Returns string s converted to all uppercase characters.

###### toTitlecase(s) {#totitlecases}

Returns string s converted into titlecase: a capital letter starting each word, and the rest of the letters lowercase. For example, `"Once upon a midnight DREARY".toTitlecase()` returns the string “Once Upon A Midnight Dreary”.

#### Trimming {#trimming}

###### trim(s) {#trims}

Returns a copy of the string s with leading and trailing whitespace removed. For example, `" island ".trim()` returns the string “island”. Identical to strip().

###### strip(s) {#strips}

Returns a copy of the string s with leading and trailing whitespace removed. For example, `" island ".strip()` returns the string “island”. Identical to trim().

###### chomp(s, sep) {#chomps-sep}

Returns a copy of string s with the string sep removed from the end if s ends with sep; otherwise, just returns s. For example, `"barely".chomp("ly")` and `"bare".chomp("ly")` both return the string “bare”.

#### Substring {#substring}

###### substring(s, n from, n to (optional)) {#substrings-n-from-n-to-optional}

Returns the substring of s starting from character index from, and up to (excluding) character index to. If the to argument is omitted, substring will output to the end of s. For example, `"profound".substring(3)` returns the string “found”, and `"profound".substring(2, 4)` returns the string “of”.

Remember that character indices start from zero. A negative character index counts from the end of the string. For example, `"profound".substring(0, -1)` returns the string “profoun”.

###### slice(s, n from, n to (optional)) {#slices-n-from-n-to-optional}

Identical to substring() in relation to strings. Also works with arrays; see [Array functions section](#slicea-n-from-n-to-optional).

###### get(s, n from, n to (optional)) {#gets-n-from-n-to-optional}

Identical to substring() in relation to strings. Also works with named fields. Also works with arrays; see [Array functions section](#geta-n-from-n-to-optional).

#### Find and replace {#find-and-replace}

###### indexOf(s, sub) {#indexofs-sub}

Returns the first character index of sub as it first occurs in s; or, returns -1 if s does not contain sub. For example, `"internationalization".indexOf("nation")` returns 5, whereas `"internationalization".indexOf("world")` returns -1.

###### lastIndexOf(s, sub) {#lastindexofs-sub}

Returns the first character index of sub as it last occurs in s; or, returns -1 if s does not contain sub. For example, `"parallel".lastIndexOf("a")` returns 3 (pointing at the second “a”).

###### replace(s, s or p find, s replace) {#replaces-s-or-p-find-s-replace}

Returns the string obtained by replacing the find string with the replace string in the inputted string. For example, `"The cow jumps over the moon and moos".replace("oo", "ee")` returns the string “The cow jumps over the meen and mees”. Find can be a regex pattern. For example, `"The cow jumps over the moon and moos".replace(/\s+/, "_")` will return “The_cow_jumps_over_the_moon_and_moos”. 

You cannot find or replace nulls with this, as null is not a string. You can instead:

1. Facet by null and then bulk-edit them to a string, or
2. Transform the column with an expression such as `if(value==null,"new",value)`.

###### replaceChars(s, s find, s replace) {#replacecharss-s-find-s-replace}

Returns the string obtained by replacing a character in s, identified by find, with the corresponding character identified in replace. For example, `"Téxt thát was optícálly recógnízéd".replaceChars("áéíóú", "aeiou")` returns the string “Text that was optically recognized”. You cannot use this to replace a single character with more than one character.

###### find(s, sub or p) {#finds-sub-or-p}

Outputs an array of all consecutive substrings inside string s that match the substring or [regex](expressions#grel-supported-regex) pattern p. For example, `"abeadsabmoloei".find(/[aeio]+/)` would result in the array [ "a", "ea", "a", "o", "oei" ].

You can supply a substring instead of p, by putting it in quotes, and OpenRefine will compile it into a regex pattern. Anytime you supply quotes, OpenRefine interprets the contents as a string, not regex. If you wish to use any regex notation, wrap the pattern in forward slashes. 

###### match(s, p) {#matchs-p}

Attempts to match the string s in its entirety against the [regex](expressions#grel-supported-regex) pattern p and, if the pattern is found, outputs an array of all [capturing groups](https://www.regular-expressions.info/brackets.html) (found in order). For example, `"230.22398, 12.3480".match(/.*(\d\d\d\d)/)` returns an array of 1 substring: [ "3480" ]. It does not find 2239 as the first sequence with four digits, because the regex indicates the four digits must come at the end of the string.

You will need to convert the array to a string to store it in a cell, with a function such as toString(). An empty array [] is returned when there is no match to the desired substrings. A null is output when the entire regex does not match.

Remember to enclose your regex in forward slashes, and to escape characters and use parentheses as needed. Parentheses denote a desired substring (capturing group); for example, “.&#42;(\d\d\d\d)” would return an array containing a single value, while “(.&#42;)(\d\d\d\d)” would return two. So, if you are looking for a desired substring anywhere within a string, use the syntax `value.match(/.*(desired-substring-regex).*/)`.

For example, if `value` is “hello 123456 goodbye”, the following would occur:

|Expression|Result|
|-|-|
|`value.match(/\d{6}/)` |null (does not match the full string)|
|`value.match(/.*\d{6}.*/)` |[ ] (no indicated substring)|
|`value.match(/.*(\d{6}).*/)` |[ "123456" ] (array with one value)|
|`value.match(/(.*)(\d{6})(.*)/)` |[ "hello ", "123456", " goodbye" ] (array with three values)|

### String parsing and splitting {#string-parsing-and-splitting}

###### toNumber(s) {#tonumbers}

Returns a string converted to a number. Will attempt to convert other formats into a string, then into a number. If the value is already a number, it will return the number.

###### split(s, s or p sep, b preserveTokens (optional)) {#splits-s-or-p-sep-b-preservetokens-optional}

Returns the array of strings obtained by splitting s by sep. The separator can be either a string or a regex pattern. For example, `"fire, water, earth, air".split(",")` returns an array of 4 strings: [ "fire", " water", " earth", " air" ]. Note that the space characters are retained but the separator is removed. If you include “true” for the preserveTokens boolean, empty segments are preserved.

###### splitByLengths(s, n1, n2, ...){#splitbylengthss-n1-n2-}

Returns the array of strings obtained by splitting s into substrings with the given lengths. For example, `"internationalization".splitByLengths(5, 6, 3)` returns an array of 3 strings: [ "inter", "nation", "ali" ]. Excess characters are discarded from the output array.

Like other functions that return an array, it also allows array slicing on the returned array. In that case, it returns the array consisting of a subset of elements between i1 and (i2 – 1).
For example,

|Expression|Result|
|-|-|
|`"internationalization".splitByLengths(5, 6, 3)[0,3]` |Returns an array of 3 strings: [ "inter", "nation", “ali” .|
|`"internationalization".splitByLengths(5, 6, 3)[0,2]` |Returns an array of 2 strings: [ "inter", "nation" ]|
|`"internationalization".splitByLengths(5, 6, 3)[1,3]` |Returns an array of 2 string: [ "nation", “ali” ]|
|`"internationalization".splitByLengths(5, 6, 3)[1]` |Returns string at position 1: "nation" |

###### smartSplit(s, s or p sep (optional)) {#smartsplits-s-or-p-sep-optional}

Returns the array of strings obtained by splitting s by sep, or by guessing either tab or comma separation if there is no sep given. Handles quotes properly and understands cancelled characters. The separator can be either a string or a regex pattern. For example, `value.smartSplit("\n")` will split at a carriage return or a new-line character.

Note: [`value.escape('javascript')`](#escapes-s-mode) is useful for previewing unprintable characters prior to using smartSplit().

###### splitByCharType(s) {#splitbychartypes}

Returns an array of strings obtained by splitting s into groups of consecutive characters each time the characters change [Unicode categories](https://en.wikipedia.org/wiki/Unicode_character_property#General_Category). For example, `"HenryCTaylor".splitByCharType()` will result in an array of [ "H", "enry", "CT", "aylor" ]. It is useful for separating letters and numbers: `"BE1A3E".splitByCharType()` will result in [ "BE", "1", "A", "3", "E" ].

###### partition(s, s or p fragment, b omitFragment (optional)) {#partitions-s-or-p-fragment-b-omitfragment-optional}

Returns an array of strings [ a, fragment, z ] where a is the substring within s before the first occurrence of fragment, and z is the substring after fragment. Fragment can be a string or a regex. For example, `"internationalization".partition("nation")` returns 3 strings: [ "inter", "nation", "alization" ]. If s does not contain fragment, it returns an array of [ s, "", "" ] (the original unpartitioned string, and two empty strings). 

If the omitFragment boolean is true, for example with `"internationalization".partition("nation", true)`, the fragment is not returned. The output is [ "inter", "alization" ].

You can use regex for your fragment. The expression `"abcdefgh".partition(/c.e/)` will output  ["abc", "cde", "fgh" ]. 

###### rpartition(s, s or p fragment, b omitFragment (optional)) {#rpartitions-s-or-p-fragment-b-omitfragment-optional}

Returns an array of strings [ a, fragment, z ] where a is the substring within s before the last occurrence of fragment, and z is the substring after the last instance of fragment. (Rpartition means “reverse partition.”) For example, `"parallel".rpartition("a")` returns 3 strings: [ "par", "a", "llel" ]. Otherwise works identically to partition() above.

### Encoding and hashing {#encoding-and-hashing}

###### diff(s1, s2, s timeUnit (optional)) {#diffs1-s2-s-timeunit-optional}

Takes two strings and compares them, returning a string. Returns the remainder of s2 starting with the first character where they differ. For example, `"cacti".diff("cactus")` returns "us". Also works with dates; see [Date functions](#diffd1-d2-s-timeunit).

###### escape(s, s mode) {#escapes-s-mode}

Escapes s in the given escaping mode. The mode can be one of: "html", "xml", "csv", "url", "javascript". Note that quotes are required around your mode. See the [recipes](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#question-marks--showing-in-your-data) for examples of escaping and unescaping.

###### unescape(s, s mode) {#unescapes-s-mode}

Unescapes s in the given escaping mode. The mode can be one of: "html", "xml", "csv", "url", "javascript". Note that quotes are required around your mode. See the [recipes](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#atampampt----att) for examples of escaping and unescaping. 

###### md5(o) {#md5o}

Returns the [MD5 hash](https://en.wikipedia.org/wiki/MD5) of an object. If fed something other than a string (array, number, date, etc.), md5() will convert it to a string and deliver the hash of the string. For example, `"internationalization".md5()` will return 2c55a1626e31b4e373ceedaa9adc12a3.

###### sha1(o) {#sha1o}

Returns the [SHA-1 hash](https://en.wikipedia.org/wiki/SHA-1) of an object. If fed something other than a string (array, number, date, etc.), sha1() will convert it to a string and deliver the hash of the string. For example, `"internationalization".sha1()` will return cd05286ee0ff8a830dbdc0c24f1cb68b83b0ef36.

###### phonetic(s, s encoding) {#phonetics-s-encoding}

Returns a phonetic encoding of a string, based on an available phonetic algorithm. See the [section on phonetic clustering](cellediting#clustering-methods) for more information. Can be one of the following supported phonetic methods: [metaphone, doublemetaphone, metaphone3](https://www.wikipedia.org/wiki/Metaphone), [soundex](https://en.wikipedia.org/wiki/Soundex), [cologne](https://en.wikipedia.org/wiki/Cologne_phonetics). Quotes are required around your encoding method. For example, `"Ruth Prawer Jhabvala".phonetic("metaphone")` outputs the string “R0PRWRJHBFL”.  

###### reinterpret(s, s encoderTarget, s encoderSource) {#reinterprets-s-encodertarget-s-encodersource}

Returns s reinterpreted through the given character encoders. You must supply one of the [supported encodings](http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html) for each of the original source and the target output. Note that quotes are required around your character encoder.

When an OpenRefine project is started, data is imported and interpreted. A specific character encoding is identified or manually selected at that time (such as UTF-8). You can reinterpret a column into another specificed encoding using this function. This function may not fix your data; it may be better to use this in conjunction with new projects to test the interpretation, and pre-format your data as needed. 

###### fingerprint(s) {#fingerprints}

Returns the fingerprint of s, a string that is the first step in [fingerprint clustering methods](cellediting#clustering-methods): it will trim whitespaces, convert all characters to lowercase, remove punctuation, sort words alphabetically, etc. For example, `"Ruth  Prawer    Jhabvala".fingerprint()` outputs the string “jhabvala prawer ruth”.  This function can be used for simple transliteration to smartly replace Unicode chars into normalized ASCII (e.g. `"L⌜e⌝gạàX̅V̅".fingerprint()` would return ["l⌜e⌝gaaxv"]).  The function is based on Java Normalizer and uses NFKD mode with some custom Unicode mapping additionally.

###### ngram(s, n) {#ngrams-n}

Returns an array of the word n-grams of s. That is, it lists all the possible consecutive combinations of n words in the string. For example, `"Ruth Prawer Jhabvala".ngram(2)` would output the array [ "Ruth Prawer", "Prawer Jhabvala" ]. A word n-gram of 1 simply lists all the words in original order; an n-gram larger than the number of words in the string will only return the original string inside an array (e.g. `"Ruth Prawer Jhabvala".ngram(4)` would simply return ["Ruth Prawer Jhabvala"]).

###### ngramFingerprint(s, n) {#ngramfingerprints-n}

Returns the [n-gram fingerprint](cellediting#clustering-methods) of s. For example, `"banana".ngram(2)` would output “anbana”, after first generating the 2-grams “ba an na an na”, removing duplicates, and sorting them alphabetically.

###### unicode(s) {#unicodes}

Returns an array of strings describing each character of s in their full unicode notation. For example, `"Bernice Rubens".unicode()` outputs [ 66, 101, 114, 110, 105, 99, 101, 32, 82, 117, 98, 101, 110, 115 ].

###### unicodeType(s) {#unicodetypes}

Returns an array of strings describing each character of s by their unicode type. For example, `"Bernice Rubens".unicodeType()` outputs [ "uppercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "space separator", "uppercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter", "lowercase letter" ].

## Format-based functions (JSON, HTML, XML) {#format-based-functions-json-html-xml}

###### jsonize(o) {#jsonizeo}

Quotes a value as a JSON literal value.

###### parseJson(s) {#parsejsons}

Parses a string as JSON. get() can then be used with parseJson(): for example, `parseJson(" { 'a' : 1 } ").get("a")` returns 1.

For example, from the following JSON array in `value`, we want to get all instances of “keywords” having the same object string name of “text”, and combine them, using the forEach() function to iterate over the array.

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

The GREL expression `forEach(value.parseJson().keywords,v,v.text).join(":::")` will output “York en route:::Anthony Eden:::President Eisenhower”.

### Jsoup XML and HTML parsing {#jsoup-xml-and-html-parsing}

###### parseHtml(s) {#parsehtmls}
Given a cell full of HTML-formatted text, parseHtml() simplifies HTML tags (such as by removing “ /” at the end of self-closing tags), closes any unclosed tags, and inserts linebreaks and indents for cleaner code. You cannot pass parseHtml() a URL, but you can pre-fetch HTML with the <span class="menuItems">[Add column by fetching URLs](columnediting#add-column-by-fetching-urls)</span> menu option. 

A cell cannot store the output of parseHtml() unless you convert it with toString(): for example, `value.parseHtml().toString()`. 

When parseHtml() simplifies HTML, it can sometimes introduce errors. When closing tags, it makes its best guesses based on line breaks, indentation, and the presence of other tags. You may need to manually check the results. 

You can then extract or [select()](#selects-element) which portions of the HTML document you need for further splitting, partitioning, etc. An example of extracting all table rows from a div using parseHtml().select() together is described more in depth at [StrippingHTML](https://github.com/OpenRefine/OpenRefine/wiki/StrippingHTML).

###### parseXml(s) {#parsexmls}
Given a cell full of XML-formatted text, parseXml() returns a full XML document and adds any missing closing tags. You can then extract or [select()](#selects-element) which portions of the XML document you need for further splitting, partitioning, etc. Functions the same way as parseHtml() is described above. 

###### select(s, element) {#selects-element}
Returns an array of all the desired elements from an HTML or XML document, if the element exists. Elements are identified using the [Jsoup selector syntax](https://jsoup.org/apidocs/org/jsoup/select/Selector.html). For example, `value.parseHtml().select("img.portrait")[0]` would return the entirety of the first “img” tag with the “portrait” class found in the parsed HTML inside `value`. Returns an empty array if no matching element is found. Use with toString() to capture the results in a cell. A tutorial of select() is shown in [StrippingHTML](https://github.com/OpenRefine/OpenRefine/wiki/StrippingHTML).

You can use select() more than once:

```
value.parseHtml().select("div#content")[0].select("tr").toString()
```

###### htmlAttr(s, element) {#htmlattrs-element}
Returns a string from an attribute on an HTML element. Use it in conjunction with parseHtml() as in the following example: `value.parseHtml().select("a.email")[0].htmlAttr("href")` would retrieve the email address attached to a link with the “email” class.

###### xmlAttr(s, element) {#xmlattrs-element}
Returns a string from an attribute on an XML element. Functions the same way htmlAttr() is described above. Use it in conjunction with parseXml().

###### htmlText(element) {#htmltextelement}
Returns a string of the text from within an HTML element (including all child elements), removing HTML tags and line breaks inside the string. Use it in conjunction with parseHtml() and select() to provide an element, as in the following example: `value.parseHtml().select("div.footer")[0].htmlText()`. 

###### xmlText(element) {#xmltextelement}
Returns a string of the text from within an XML element (including all child elements). Functions the same way htmlText() is described above. Use it in conjunction with parseXml() and select() to provide an element.

###### wholeText(element) {#wholetextelement}

Selects the (unencoded) text of an element and its children, including any new lines and spaces, and returns a string of unencoded, un-normalized text. Use it in conjunction with parseHtml() and select() to provide an element as in the following example: `value.parseHtml().select("div.footer")[0].wholeText()`.

###### innerHtml(element) {#innerhtmlelement}
Returns the [inner HTML](https://developer.mozilla.org/en-US/docs/Web/API/Element/innerHTML) of an HTML element. This will include text and children elements within the element selected. Use it in conjunction with parseHtml() and select() to provide an element.

###### innerXml(element) {#innerxmlelement}
Returns the inner XML elements of an XML element. Does not return the text directly inside your chosen XML element - only the contents of its children. To select the direct text, use ownText(). To select both, use xmlText(). Use it in conjunction with parseXml() and select() to provide an element.

###### ownText(element) {#owntextelement}
Returns the text directly inside the selected XML or HTML element only, ignoring text inside children elements (for this, use innerXml()). Use it in conjunction with a parser and select() to provide an element.

## Array functions {#array-functions}

###### length(a) {#lengtha}
Returns the size of an array, meaning the number of objects inside it. Arrays can be empty, in which case length() will return 0. 

###### slice(a, n from, n to (optional)) {#slicea-n-from-n-to-optional}
Returns a sub-array of a given array, from the first index provided and up to and excluding the optional last index provided. Remember that array objects are indexed starting at 0. If the to value is omitted, it is understood to be the end of the array. For example, `[0, 1, 2, 3, 4].slice(1, 3)` returns [ 1, 2 ], and `[ 0, 1, 2, 3, 4].slice(2)` returns [ 2, 3, 4 ]. Also works with strings; see [String functions](#slices-n-from-n-to-optional).

###### get(a, n from, n to (optional)) {#geta-n-from-n-to-optional}
Returns a sub-array of a given array, from the first index provided and up to and excluding the optional last index provided. Remember that array objects are indexed starting at 0. 

If the to value is omitted, only one array item is returned, as a string, instead of a sub-array. To return a sub-array from one index to the end, you can set the to argument to a very high number such as `value.get(2,999)` or you can use something like `with(value,a,a.get(1,a.length()))` to count the length of each array.

Also works with strings; see [String functions](#gets-n-from-n-to-optional).

###### inArray(a, s) {#inarraya-s}
Returns true if the array contains the desired string, and false otherwise. Will not convert data types; for example, `[ 1, 2, 3, 4 ].inArray("3")` will return false.

###### reverse(a) {#reversea}
Reverses the array. For example, `[ 0, 1, 2, 3].reverse()` returns the array [ 3, 2, 1, 0 ].

###### sort(a) {#sorta}
Sorts the array in ascending order. Sorting is case-sensitive, uppercase first and lowercase second. For example, `[ "al", "Joe", "Bob", "jim" ].sort()` returns the array [ "Bob", "Joe", "al", "jim" ]. 

###### sum(a) {#suma}
Return the sum of the numbers in the array. For example, `[ 2, 1, 0, 3 ].sum()` returns 6.

###### join(a, sep) {#joina-sep}
Joins the items in the array with sep, and returns it all as a string. For example, `[ "and", "or", "not" ].join("/")` returns the string “and/or/not”.

###### uniques(a) {#uniquesa}
Returns the array with duplicates removed. Case-sensitive. For example, `[ "al", "Joe", "Bob", "Joe", "Al", "Bob" ].uniques()` returns the array [ "Joe", "al", "Al", "Bob" ]. 

As of OpenRefine 3.4.1, uniques() reorders the array items it returns; in 3.4 beta 644 and onwards, it preserves the original order (in this case, [ "al", "Joe", "Bob", "Al" ]). 

## Date functions {#date-functions}

###### now() {#now}

Returns the current time according to your system clock, in the [ISO 8601 extended format](exploring#data-types) (converted to UTC). For example, 10:53am (and 00 seconds) on November 26th 2020 in EST returns [date 2020-11-26T15:53:00Z].

###### toDate(o, b monthFirst, s format1, s format2, ...) {#todateo-b-monthfirst-s-format1-s-format2-}

Returns the inputted object converted to a date object. Without arguments, it returns the ISO 8601 extended format. With arguments, you can control the output format:
*   monthFirst: set false if the date is formatted with the day before the month.
*   formatN: attempt to parse the date using an ordered list of possible formats. Supply formats based on the [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) syntax (and see the table below for a handy reference). 

For example, you can parse a column containing dates in different formats, such as cells with “Nov-09” and “11/09”, using `value.toDate('MM/yy','MMM-yy').toString('yyyy-MM')` and both will output “2009-11”. For another example, “1/4/2012 13:30:00” can be parsed into a date using `value.toDate('d/M/y H&#58;m&#58;s')`.

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

###### diff(d1, d2, s timeUnit) {#diffd1-d2-s-timeunit}

Given two dates, returns a number indicating the difference in a given time unit (see the table below). For example, `diff(("Nov-11".toDate('MMM-yy')), ("Nov-09".toDate('MMM-yy')), "weeks")` will return 104, for 104 weeks, or two years. The later date should go first. If the output is negative, invert d1 and d2.

Also works with strings; see [diff() in string functions](#diffsd1-sd2-s-timeunit-optional).

###### inc(d, n, s timeUnit) {#incd-n-s-timeunit}

Returns a date changed by the given amount in the given unit of time (see the table below). The default unit is “hour”. A positive value increases the date, and a negative value moves it back in time. For example, if you want to move a date backwards by two months, use `value.inc(-2,"month")`.

###### datePart(d, s timeUnit) {#datepartd-s-timeunit}

Returns part of a date. The data type returned depends on the unit (see the table below). 

OpenRefine supports the following values for timeUnit:

| Unit | Date part returned | Returned data type | Example using [date 2014-03-14T05:30:04.000789000Z] as value |
|-|-|-|-|
| years | Year | Number | value.datePart("years") → 2014 |
| year | Year | Number | value.datePart("year") → 2014 |
| months | Month | Number | value.datePart("months") → 2 |
| month | Month | Number | value.datePart("month") → 2 |
| weeks | Week of the month | Number | value.datePart("weeks") → 3 |
| week | Week of the month | Number | value.datePart("week") → 3 |
| w | Week of the month | Number | value.datePart("w") → 3 |
| weekday | Day of the week | String | value.datePart("weekday") → Friday |
| hours | Hour | Number | value.datePart("hours") → 5 |
| hour | Hour | Number | value.datePart("hour") → 5 |
| h | Hour | Number | value.datePart("h") → 5 |
| minutes | Minute | Number | value.datePart("minutes") → 30 |
| minute | Minute | Number | value.datePart("minute") → 30 |
| min | Minute | Number | value.datePart("min") → 30 |
| seconds | Seconds | Number | value.datePart("seconds") → 04 |
| sec | Seconds | Number | value.datePart("sec") → 04 |
| s | Seconds | Number | value.datePart("s") → 04 |
| milliseconds | Millseconds | Number | value.datePart("milliseconds") → 789 |
| ms | Millseconds | Number | value.datePart("ms") → 789 |
| S | Millseconds | Number | value.datePart("S") → 789 |
| n | Nanoseconds | Number | value.datePart("n") → 789000 |
| nano | Nanoseconds | Number | value.datePart("n") → 789000 |
| nanos | Nanoseconds | Number | value.datePart("n") → 789000 |
| time | Milliseconds between input and the [Unix Epoch](https://en.wikipedia.org/wiki/Unix_time) | Number | value.datePart("time") → 1394775004000 |

## Math functions {#math-functions}

For integer division and precision, you can use simple evaluations such as `1 / 2`, which is equivalent to `floor(1/2)` - that is, it returns only whole number results. If either operand is a floating point number, they both get promoted to floating point and a floating point result is returned. You can use `1 / 2.0` or `1.0 / 2` or `1.0 * x / y` (if you're working with variables of unknown contents).

:::caution
Some of these math functions don't recognize integers when supplied as the first argument in dot notation (e.g., `5.cos()` simply returns 5 instead of the expected result). To ensure operations are successful, always wrap the first argument in brackets, such as `(value).cos()`.
:::

|Function|Use|Example|
|-|-|-|
|`abs(n)`|Returns the absolute value of a number.|`abs(-6)` returns 6.|
|`acos(n)`|Returns the arc cosine of an angle, in the range 0 through [PI](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#PI).|`acos(0.345)` returns 1.218557541697832.|
|`asin(n)`|Returns the arc sine of an angle in the range of -PI/2 through PI/2.|`asin(0.345)` returns 0.35223878509706474.|
|`atan(n)`|Returns the arc tangent of an angle in the range of -PI/2 through PI/2.|`atan(0.345)` returns 0.3322135507465967.|
|`atan2(n1, n2)`|Converts rectangular coordinates (n1, n2) to polar (r, theta). Returns number theta.|`atan2(0.345,0.6)` returns 0.5218342798144103.|
|`ceil(n)`|Returns the ceiling of a number.|`3.7.ceil()` returns 4 and `-3.7.ceil()` returns -3.|
|`combin(n1, n2)`|Returns the number of combinations for n2 elements as divided into n1.|`combin(20,2)` returns 190.|
|`cos(n)`|Returns the trigonometric cosine of a value.|`cos(5)` returns 0.28366218546322625.|
|`cosh(n)`|Returns the hyperbolic cosine of a value.|`cosh(5)` returns 74.20994852478785.|
|`degrees(n)`|Converts an angle from radians to degrees.|`degrees(5)` returns 286.4788975654116.|
|`even(n)`|Rounds the number up to the nearest even integer.|`even(5)` returns 6.|
|`exp(n)`|Returns [e](https://en.wikipedia.org/wiki/E_(mathematical_constant)) raised to the power of n.|`exp(5)` returns 148.4131591025766.|
|`fact(n)`|Returns the factorial of a number, starting from 1.|`fact(5)` returns 120.|
|`factn(n1, n2)`|Returns the factorial of n1, starting from n2.|`factn(10,3)` returns 280.|
|`floor(n)`|Returns the floor of a number.|`3.7.floor()` returns 3 and `-3.7.floor()` returns -4.|
|`gcd(n1, n2)`|Returns the greatest common denominator of two numbers.|`gcd(95,135)` returns 5.|
|`lcm(n1, n2)`|Returns the least common multiple of two numbers.|`lcm(95,135)` returns 2565.|
|`ln(n)`|Returns the natural logarithm of n.|`ln(5)` returns 1.6094379124341003.|
|`log(n)`|Returns the base 10 logarithm of n.|`log(5)` returns 0.6989700043360189.|
|`max(n1, n2)`|Returns the larger of two numbers.|`max(3,10)` returns 10.|
|`min(n1, n2)`|Returns the smaller of two numbers.|`min(3,10)` returns 3.|
|`mod(n1, n2)`|Returns n1 modulus n2. Note: `value.mod(9)` will work, whereas `74.mod(9)` will not work.|`mod(74, 9)` returns 2. |
|`multinomial(n1, n2 …(optional))`|Calculates the multinomial of one number or a series of numbers.|`multinomial(2,3)` returns 10.|
|`odd(n)`|Rounds the number up to the nearest odd integer.|`odd(10)` returns 11.|
|`pow(n1, n2)`|Returns n1 raised to the power of n2. Note: value.pow(3)` will work, whereas `2.pow(3)` will not work.|`pow(2, 3)` returns 8 (2 cubed) and `pow(3, 2)` returns 9 (3 squared). The square root of any numeric value can be called with `value.pow(0.5)`.|
|`quotient(n1, n2)`|Returns the integer portion of a division (truncated, not rounded), when supplied with a numerator and denominator.|`quotient(9,2)` returns 4.|
|`radians(n)`|Converts an angle in degrees to radians.|`radians(10)` returns 0.17453292519943295.|
|`randomNumber(n lowerBound, n upperBound)`|Returns a random integer in the interval between the lower and upper bounds (inclusively). Will output a different random number in each cell in a column.|
|`round(n)`|Rounds a number to the nearest integer.|`3.7.round()` returns 4 and `-3.7.round()` returns -4.|
|`sin(n)`|Returns the trigonometric sine of an angle.|`sin(10)` returns -0.5440211108893698.|
|`sinh(n)`|Returns the hyperbolic sine of an angle.|`sinh(10)` returns 11013.232874703393.|
|`sum(a)`|Sums the numbers in an array. Ignores non-number items. Returns 0 if the array does not contain numbers.|`sum([ 10, 2, three ])` returns 12.|
|`tan(n)`|Returns the trigonometric tangent of an angle.|`tan(10)` returns 0.6483608274590866.|
|`tanh(n)`|Returns the hyperbolic tangent of a value.|`tanh(10)` returns 0.9999999958776927.|

## Other functions {#other-functions}

###### type(o) {#typeo}
Returns a string with the data type of o, such as undefined, string, number, boolean, etc. For example, a [Transform](cellediting#transform) operation using `value.type()` will convert all cells in a column to strings of their data types.

###### facetCount(choiceValue, s facetExpression, s columnName) {#facetcountchoicevalue-s-facetexpression-s-columnname}
Returns the facet count corresponding to the given choice value, by looking for the facetExpression in the choiceValue in columnName. For example, to create facet counts for the following table, we could generate a new column based on “Gift” and enter in `value.facetCount("value", "Gift")`. This would add the column we've named “Count”:

| Gift | Recipient | Price | Count |
|-|-|-|-|
| lamp | Mary | 20 | 1 |
| clock | John | 57 | 2 |
| watch | Amit | 80 | 1 |
| clock | Claire | 62 | 2 |

The facet expression, wrapped in quotes, can be useful to manipulate the inputted values before counting. For example, you could do a textual cleanup using fingerprint(): `(value.fingerprint()).facetCount(value.fingerprint(),"Gift")`.

###### hasField(o, s name) {#hasfieldo-s-name}
Returns a boolean indicating whether o has a member field called [name](expressions#variables). For example, `cell.recon.hasField("match")` will return false if a reconciliation match hasn’t been selected yet, or true if it has. You cannot chain your desired fields: for example, `cell.hasField("recon.match")` will return false even if the above expression returns true).

###### coalesce(o1, o2, o3, ...) {#coalesceo1-o2-o3-}
Returns the first non-null from a series of objects. For example, `coalesce(value, "")` would return an empty string “” if `value` was null, but otherwise return `value`.

###### cross(cell, s projectName (optional), s columnName (optional)) {#crosscell-s-projectname-optional-s-columnname-optional}
Returns an array of zero or more rows in the project projectName for which the cells in their column columnName have the same content as the cell in your chosen column. For example, if two projects contained matching names, and you wanted to pull addresses for people by their names from a project called “People” you would apply the following expression to your column of names: 
```
cell.cross("People","Name").cells["Address"].value[0]
```

This would match your current column to the “Name” column in “People” and, using those matches, pull the respective “Address” value into your current project. 

You may need to do some data preparation with cross(), such as using trim() on your key columns or deduplicating values.

The first argument will be interpreted as `cell.value` if set to `cell`. If you omit projectName and columnName, they will default to the current project and index column (number 0). 

Recipes and more examples for using cross() can be found [on our wiki](https://github.com/OpenRefine/OpenRefine/wiki/Recipes#combining-datasets).
