// CHECKSTYLE:OFF

package com.google.refine.grel;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;


/**
 * Generated localization support class.
 * 
 */
@SuppressWarnings({
    "",
    "PMD",
    "all"
})
public class FunctionDescription {

    /**
     * The resource bundle reference
     * 
     */
    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(FunctionDescription.class);

    /**
     * Key {@code str_trim}: {@code Returns a copy of string s with leading
     * and trailing whitespace removed.}.
     * 
     * @return
     *     {@code Returns a copy of string s with leading and trailing whitespace
     *     removed.}
     */
    public static String str_trim() {
        return holder.format("str_trim");
    }

    /**
     * Key {@code str_trim}: {@code Returns a copy of string s with leading
     * and trailing whitespace removed.}.
     * 
     * @return
     *     {@code Returns a copy of string s with leading and trailing whitespace
     *     removed.}
     */
    public static Localizable _str_trim() {
        return new Localizable(holder, "str_trim");
    }

    /**
     * Key {@code math_asin}: {@code Returns the arc sine of an angle in the
     * range of -PI/2 through PI/2.}.
     * 
     * @return
     *     {@code Returns the arc sine of an angle in the range of -PI/2 through
     *     PI/2.}
     */
    public static String math_asin() {
        return holder.format("math_asin");
    }

    /**
     * Key {@code math_asin}: {@code Returns the arc sine of an angle in the
     * range of -PI/2 through PI/2.}.
     * 
     * @return
     *     {@code Returns the arc sine of an angle in the range of -PI/2 through
     *     PI/2.}
     */
    public static Localizable _math_asin() {
        return new Localizable(holder, "math_asin");
    }

    /**
     * Key {@code html_parse_html}: {@code Given a cell full of
     * HTML-formatted text, parseHtml() simplifies HTML tags (such as by
     * removing '' /'' at the end of self-closing tags), closes any unclosed
     * tags, and inserts linebreaks and indents for cleaner code. A cell
     * cannot store the output of parseHtml() unless you convert it with
     * toString(): for example, value.parseHtml().toString().}.
     * 
     * @return
     *     {@code Given a cell full of HTML-formatted text, parseHtml()
     *     simplifies HTML tags (such as by removing '' /'' at the end of
     *     self-closing tags), closes any unclosed tags, and inserts linebreaks
     *     and indents for cleaner code. A cell cannot store the output of
     *     parseHtml() unless you convert it with toString(): for example,
     *     value.parseHtml().toString().}
     */
    public static String html_parse_html() {
        return holder.format("html_parse_html");
    }

    /**
     * Key {@code html_parse_html}: {@code Given a cell full of
     * HTML-formatted text, parseHtml() simplifies HTML tags (such as by
     * removing '' /'' at the end of self-closing tags), closes any unclosed
     * tags, and inserts linebreaks and indents for cleaner code. A cell
     * cannot store the output of parseHtml() unless you convert it with
     * toString(): for example, value.parseHtml().toString().}.
     * 
     * @return
     *     {@code Given a cell full of HTML-formatted text, parseHtml()
     *     simplifies HTML tags (such as by removing '' /'' at the end of
     *     self-closing tags), closes any unclosed tags, and inserts linebreaks
     *     and indents for cleaner code. A cell cannot store the output of
     *     parseHtml() unless you convert it with toString(): for example,
     *     value.parseHtml().toString().}
     */
    public static Localizable _html_parse_html() {
        return new Localizable(holder, "html_parse_html");
    }

    /**
     * Key {@code bool_xor}: {@code Uses the logical operator XOR
     * (exclusive-or) on two or more booleans to output a boolean. Evaluates
     * multiple statements, then returns true if only one of them is true.
     * For example, (1 < 3).xor(1 < 7) returns false because more than one of
     * the conditions is true.}.
     * 
     * @return
     *     {@code Uses the logical operator XOR (exclusive-or) on two or more
     *     booleans to output a boolean. Evaluates multiple statements, then
     *     returns true if only one of them is true. For example, (1 < 3).xor(1 <
     *      7) returns false because more than one of the conditions is true.}
     */
    public static String bool_xor() {
        return holder.format("bool_xor");
    }

    /**
     * Key {@code bool_xor}: {@code Uses the logical operator XOR
     * (exclusive-or) on two or more booleans to output a boolean. Evaluates
     * multiple statements, then returns true if only one of them is true.
     * For example, (1 < 3).xor(1 < 7) returns false because more than one of
     * the conditions is true.}.
     * 
     * @return
     *     {@code Uses the logical operator XOR (exclusive-or) on two or more
     *     booleans to output a boolean. Evaluates multiple statements, then
     *     returns true if only one of them is true. For example, (1 < 3).xor(1 <
     *      7) returns false because more than one of the conditions is true.}
     */
    public static Localizable _bool_xor() {
        return new Localizable(holder, "bool_xor");
    }

    /**
     * Key {@code fun_get}: {@code If o has named fields, returns the field
     * named ''from'' of o. If o is an array, returns a sub-array o[from,
     * to]. if o is a string, returns o.substring(from, to).}.
     * 
     * @return
     *     {@code If o has named fields, returns the field named ''from'' of o.
     *     If o is an array, returns a sub-array o[from, to]. if o is a string,
     *     returns o.substring(from, to).}
     */
    public static String fun_get() {
        return holder.format("fun_get");
    }

    /**
     * Key {@code fun_get}: {@code If o has named fields, returns the field
     * named ''from'' of o. If o is an array, returns a sub-array o[from,
     * to]. if o is a string, returns o.substring(from, to).}.
     * 
     * @return
     *     {@code If o has named fields, returns the field named ''from'' of o.
     *     If o is an array, returns a sub-array o[from, to]. if o is a string,
     *     returns o.substring(from, to).}
     */
    public static Localizable _fun_get() {
        return new Localizable(holder, "fun_get");
    }

    /**
     * Key {@code str_reinterpret}: {@code Returns s reinterpreted through
     * the given character encoders. You must supply one of the supported
     * encodings for each of the original source and the target output:
     * https://docs.oracle.com/javase/1.5.0/docs/guide/intl/encoding.doc.html.
     * Note that quotes are required around character encoders.}.
     * 
     * @return
     *     {@code Returns s reinterpreted through the given character encoders.
     *     You must supply one of the supported encodings for each of the
     *     original source and the target output:
     *     https://docs.oracle.com/javase/1.5.0/docs/guide/intl/encoding.doc.html.
     *     Note that quotes are required around character encoders.}
     */
    public static String str_reinterpret() {
        return holder.format("str_reinterpret");
    }

    /**
     * Key {@code str_reinterpret}: {@code Returns s reinterpreted through
     * the given character encoders. You must supply one of the supported
     * encodings for each of the original source and the target output:
     * https://docs.oracle.com/javase/1.5.0/docs/guide/intl/encoding.doc.html.
     * Note that quotes are required around character encoders.}.
     * 
     * @return
     *     {@code Returns s reinterpreted through the given character encoders.
     *     You must supply one of the supported encodings for each of the
     *     original source and the target output:
     *     https://docs.oracle.com/javase/1.5.0/docs/guide/intl/encoding.doc.html.
     *     Note that quotes are required around character encoders.}
     */
    public static Localizable _str_reinterpret() {
        return new Localizable(holder, "str_reinterpret");
    }

    /**
     * Key {@code math_tanh}: {@code Returns the hyperbolic tangent of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the hyperbolic tangent of an angle.}
     */
    public static String math_tanh() {
        return holder.format("math_tanh");
    }

    /**
     * Key {@code math_tanh}: {@code Returns the hyperbolic tangent of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the hyperbolic tangent of an angle.}
     */
    public static Localizable _math_tanh() {
        return new Localizable(holder, "math_tanh");
    }

    /**
     * Key {@code str_unicode_type}: {@code Returns an array of strings
     * describing each character of s by their unicode type.}.
     * 
     * @return
     *     {@code Returns an array of strings describing each character of s by
     *     their unicode type.}
     */
    public static String str_unicode_type() {
        return holder.format("str_unicode_type");
    }

    /**
     * Key {@code str_unicode_type}: {@code Returns an array of strings
     * describing each character of s by their unicode type.}.
     * 
     * @return
     *     {@code Returns an array of strings describing each character of s by
     *     their unicode type.}
     */
    public static Localizable _str_unicode_type() {
        return new Localizable(holder, "str_unicode_type");
    }

    /**
     * Key {@code math_tan}: {@code Returns the trigonometric tangent of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric tangent of an angle.}
     */
    public static String math_tan() {
        return holder.format("math_tan");
    }

    /**
     * Key {@code math_tan}: {@code Returns the trigonometric tangent of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric tangent of an angle.}
     */
    public static Localizable _math_tan() {
        return new Localizable(holder, "math_tan");
    }

    /**
     * Key {@code str_sha1}: {@code Returns the SHA-1 hash of an object. If
     * fed something other than a string (array, number, date, etc.), sha1()
     * will convert it to a string and deliver the hash of the string.}.
     * 
     * @return
     *     {@code Returns the SHA-1 hash of an object. If fed something other
     *     than a string (array, number, date, etc.), sha1() will convert it to a
     *     string and deliver the hash of the string.}
     */
    public static String str_sha1() {
        return holder.format("str_sha1");
    }

    /**
     * Key {@code str_sha1}: {@code Returns the SHA-1 hash of an object. If
     * fed something other than a string (array, number, date, etc.), sha1()
     * will convert it to a string and deliver the hash of the string.}.
     * 
     * @return
     *     {@code Returns the SHA-1 hash of an object. If fed something other
     *     than a string (array, number, date, etc.), sha1() will convert it to a
     *     string and deliver the hash of the string.}
     */
    public static Localizable _str_sha1() {
        return new Localizable(holder, "str_sha1");
    }

    /**
     * Key {@code math_factn}: {@code Returns the factorial of n1, starting
     * from n2.}.
     * 
     * @return
     *     {@code Returns the factorial of n1, starting from n2.}
     */
    public static String math_factn() {
        return holder.format("math_factn");
    }

    /**
     * Key {@code math_factn}: {@code Returns the factorial of n1, starting
     * from n2.}.
     * 
     * @return
     *     {@code Returns the factorial of n1, starting from n2.}
     */
    public static Localizable _math_factn() {
        return new Localizable(holder, "math_factn");
    }

    /**
     * Key {@code math_cos}: {@code Returns the trigonometric cosine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric cosine of an angle.}
     */
    public static String math_cos() {
        return holder.format("math_cos");
    }

    /**
     * Key {@code math_cos}: {@code Returns the trigonometric cosine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric cosine of an angle.}
     */
    public static Localizable _math_cos() {
        return new Localizable(holder, "math_cos");
    }

    /**
     * Key {@code fun_jsonize}: {@code Quotes a value as a JSON literal
     * value.}.
     * 
     * @return
     *     {@code Quotes a value as a JSON literal value.}
     */
    public static String fun_jsonize() {
        return holder.format("fun_jsonize");
    }

    /**
     * Key {@code fun_jsonize}: {@code Quotes a value as a JSON literal
     * value.}.
     * 
     * @return
     *     {@code Quotes a value as a JSON literal value.}
     */
    public static Localizable _fun_jsonize() {
        return new Localizable(holder, "fun_jsonize");
    }

    /**
     * Key {@code date_now}: {@code Returns the current time according to
     * your system clock, in the ISO 8601 extended format (converted to UTC).
     * For example, 10:53am (and 00 seconds) on November 26th 2020 in EST
     * returns [date 2020-11-26T15:53:00Z].}.
     * 
     * @return
     *     {@code Returns the current time according to your system clock, in the
     *     ISO 8601 extended format (converted to UTC). For example, 10:53am (and
     *      00 seconds) on November 26th 2020 in EST returns [date
     *      2020-11-26T15:53:00Z].}
     */
    public static String date_now() {
        return holder.format("date_now");
    }

    /**
     * Key {@code date_now}: {@code Returns the current time according to
     * your system clock, in the ISO 8601 extended format (converted to UTC).
     * For example, 10:53am (and 00 seconds) on November 26th 2020 in EST
     * returns [date 2020-11-26T15:53:00Z].}.
     * 
     * @return
     *     {@code Returns the current time according to your system clock, in the
     *     ISO 8601 extended format (converted to UTC). For example, 10:53am (and
     *      00 seconds) on November 26th 2020 in EST returns [date
     *      2020-11-26T15:53:00Z].}
     */
    public static Localizable _date_now() {
        return new Localizable(holder, "date_now");
    }

    /**
     * Key {@code date_inc}: {@code Returns a date changed by the given
     * amount in the given unit of time, in quotes. See
     * https://docs.openrefine.org/manual/grelfunctions/#incd-n-s-timeunit
     * for a table. The default unit is ''hour''. A positive value increases
     * the date, and a negative value moves it back in time.}.
     * 
     * @return
     *     {@code Returns a date changed by the given amount in the given unit of
     *     time, in quotes. See
     *     https://docs.openrefine.org/manual/grelfunctions/#incd-n-s-timeunit
     *     for a table. The default unit is ''hour''. A positive value increases
     *     the date, and a negative value moves it back in time.}
     */
    public static String date_inc() {
        return holder.format("date_inc");
    }

    /**
     * Key {@code date_inc}: {@code Returns a date changed by the given
     * amount in the given unit of time, in quotes. See
     * https://docs.openrefine.org/manual/grelfunctions/#incd-n-s-timeunit
     * for a table. The default unit is ''hour''. A positive value increases
     * the date, and a negative value moves it back in time.}.
     * 
     * @return
     *     {@code Returns a date changed by the given amount in the given unit of
     *     time, in quotes. See
     *     https://docs.openrefine.org/manual/grelfunctions/#incd-n-s-timeunit
     *     for a table. The default unit is ''hour''. A positive value increases
     *     the date, and a negative value moves it back in time.}
     */
    public static Localizable _date_inc() {
        return new Localizable(holder, "date_inc");
    }

    /**
     * Key {@code str_split_by_lengths}: {@code Returns the array of strings
     * obtained by splitting s into substrings with the given lengths. For
     * example, "internationalization".splitByLengths(5, 6, 3) returns an
     * array of 3 strings: [ "inter", "nation", "ali" ]. Excess characters
     * are discarded from the output array.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s into
     *     substrings with the given lengths. For example,
     *     "internationalization".splitByLengths(5, 6, 3) returns an array of 3
     *     strings: [ "inter", "nation", "ali" ]. Excess characters are discarded
     *     from the output array.}
     */
    public static String str_split_by_lengths() {
        return holder.format("str_split_by_lengths");
    }

    /**
     * Key {@code str_split_by_lengths}: {@code Returns the array of strings
     * obtained by splitting s into substrings with the given lengths. For
     * example, "internationalization".splitByLengths(5, 6, 3) returns an
     * array of 3 strings: [ "inter", "nation", "ali" ]. Excess characters
     * are discarded from the output array.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s into
     *     substrings with the given lengths. For example,
     *     "internationalization".splitByLengths(5, 6, 3) returns an array of 3
     *     strings: [ "inter", "nation", "ali" ]. Excess characters are discarded
     *     from the output array.}
     */
    public static Localizable _str_split_by_lengths() {
        return new Localizable(holder, "str_split_by_lengths");
    }

    /**
     * Key {@code str_split_repeat_lengths}: {@code Returns the array of strings
     * obtained by splitting s into substrings until termination with the given sequence. For
     * example, "internationalization".splitRepeatLengths(5, 2) returns an
     * array of strings: [ "inter", "na", "tiona", "li", "zatio", "n" ]. Splitting proceeds until string termination, and
     * last element may not correspond to one of the sequence lengths in the case that the sequence does not perfectly
     * divide the string.}.
     *
     * @return
     *     {@code Returns the array of strings
     *      * obtained by splitting s into substrings until termination with the given sequence. For
     *      * example, "internationalization".splitRepeatLengths(5, 2) returns an
     *      * array of strings: [ "inter", "na", "tiona", "li", "zatio", "n" ]. Splitting proceeds until string termination, and
     *      * last element may not correspond to one of the sequence lengths in the case that the sequence does not perfectly
     *      * divide the string.}.
     */
    public static String str_split_repeat_lengths() {
        return holder.format("str_split_repeat_lengths");
    }

    /**
     * Key {@code str_split_repeat_lengths}: {@code Returns the array of strings
     * obtained by splitting s into substrings until termination with the given sequence. For
     * example, "internationalization".splitRepeatLengths(5, 2) returns an
     * array of strings: [ "inter", "na", "tiona", "li", "zatio", "n" ]. Splitting proceeds until string termination, and
     * last element may not correspond to one of the sequence lengths in the case that the sequence does not perfectly
     * divide the string.}.
     *
     * @return
     *     {@code Returns the array of strings
     *      * obtained by splitting s into substrings until termination with the given sequence. For
     *      * example, "internationalization".splitRepeatLengths(5, 2) returns an
     *      * array of strings: [ "inter", "na", "tiona", "li", "zatio", "n" ]. Splitting proceeds until string termination, and
     *      * last element may not correspond to one of the sequence lengths in the case that the sequence does not perfectly
     *      * divide the string.}.
     */
    public static Localizable _str_split_repeat_lengths() {
        return new Localizable(holder, "str_split_repeat_lengths");
    }

    /**
     * Key {@code str_replace_each}: {@code Replace each occurrence of a
     * substring in a string with another substring.}.
     * 
     * @return
     *     {@code Replace each occurrence of a substring in a string with another
     *     substring.}
     */
    public static String str_replace_each() {
        return holder.format("str_replace_each");
    }

    /**
     * Key {@code str_replace_each}: {@code Replace each occurrence of a
     * substring in a string with another substring.}.
     * 
     * @return
     *     {@code Replace each occurrence of a substring in a string with another
     *     substring.}
     */
    public static Localizable _str_replace_each() {
        return new Localizable(holder, "str_replace_each");
    }

    /**
     * Key {@code math_cosh}: {@code Returns the hyperbolic cosine of a
     * value.}.
     * 
     * @return
     *     {@code Returns the hyperbolic cosine of a value.}
     */
    public static String math_cosh() {
        return holder.format("math_cosh");
    }

    /**
     * Key {@code math_cosh}: {@code Returns the hyperbolic cosine of a
     * value.}.
     * 
     * @return
     *     {@code Returns the hyperbolic cosine of a value.}
     */
    public static Localizable _math_cosh() {
        return new Localizable(holder, "math_cosh");
    }

    /**
     * Key {@code str_chomp}: {@code Returns a copy of string s with string
     * sep remoed from the end if s ends with sep; otherwies, returns s.}.
     * 
     * @return
     *     {@code Returns a copy of string s with string sep remoed from the end
     *     if s ends with sep; otherwies, returns s.}
     */
    public static String str_chomp() {
        return holder.format("str_chomp");
    }

    /**
     * Key {@code str_chomp}: {@code Returns a copy of string s with string
     * sep remoed from the end if s ends with sep; otherwies, returns s.}.
     * 
     * @return
     *     {@code Returns a copy of string s with string sep remoed from the end
     *     if s ends with sep; otherwies, returns s.}
     */
    public static Localizable _str_chomp() {
        return new Localizable(holder, "str_chomp");
    }

    /**
     * Key {@code fun_has_field}: {@code Returns a boolean indicating whether
     * o has a member field called name.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether o has a member field
     *     called name.}
     */
    public static String fun_has_field() {
        return holder.format("fun_has_field");
    }

    /**
     * Key {@code fun_has_field}: {@code Returns a boolean indicating whether
     * o has a member field called name.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether o has a member field
     *     called name.}
     */
    public static Localizable _fun_has_field() {
        return new Localizable(holder, "fun_has_field");
    }

    /**
     * Key {@code math_ln}: {@code Returns the natural logarithm of n.}.
     * 
     * @return
     *     {@code Returns the natural logarithm of n.}
     */
    public static String math_ln() {
        return holder.format("math_ln");
    }

    /**
     * Key {@code math_ln}: {@code Returns the natural logarithm of n.}.
     * 
     * @return
     *     {@code Returns the natural logarithm of n.}
     */
    public static Localizable _math_ln() {
        return new Localizable(holder, "math_ln");
    }

    /**
     * Key {@code str_unicode}: {@code Returns an array of strings describing
     * each character of s in their full unicode notation.}.
     * 
     * @return
     *     {@code Returns an array of strings describing each character of s in
     *     their full unicode notation.}
     */
    public static String str_unicode() {
        return holder.format("str_unicode");
    }

    /**
     * Key {@code str_unicode}: {@code Returns an array of strings describing
     * each character of s in their full unicode notation.}.
     * 
     * @return
     *     {@code Returns an array of strings describing each character of s in
     *     their full unicode notation.}
     */
    public static Localizable _str_unicode() {
        return new Localizable(holder, "str_unicode");
    }

    /**
     * Key {@code str_diff}: {@code For strings, takes two strings and
     * compares them, returning a string. Returns the remainder of o2
     * starting with the first character where they differ. For dates,
     * returns the difference in given time units. See the time unit table at
     * https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit.}.
     * 
     * @return
     *     {@code For strings, takes two strings and compares them, returning a
     *     string. Returns the remainder of o2 starting with the first character
     *     where they differ. For dates, returns the difference in given time
     *     units. See the time unit table at
     *     https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit.}
     */
    public static String str_diff() {
        return holder.format("str_diff");
    }

    /**
     * Key {@code str_diff}: {@code For strings, takes two strings and
     * compares them, returning a string. Returns the remainder of o2
     * starting with the first character where they differ. For dates,
     * returns the difference in given time units. See the time unit table at
     * https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit.}.
     * 
     * @return
     *     {@code For strings, takes two strings and compares them, returning a
     *     string. Returns the remainder of o2 starting with the first character
     *     where they differ. For dates, returns the difference in given time
     *     units. See the time unit table at
     *     https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit.}
     */
    public static Localizable _str_diff() {
        return new Localizable(holder, "str_diff");
    }

    /**
     * Key {@code str_parse_json}: {@code Parses a string as JSON.}.
     * 
     * @return
     *     {@code Parses a string as JSON.}
     */
    public static String str_parse_json() {
        return holder.format("str_parse_json");
    }

    /**
     * Key {@code str_parse_json}: {@code Parses a string as JSON.}.
     * 
     * @return
     *     {@code Parses a string as JSON.}
     */
    public static Localizable _str_parse_json() {
        return new Localizable(holder, "str_parse_json");
    }

    /**
     * Key {@code fun_to_string}: {@code Takes any value type (string,
     * number, date, boolean, error, null) and gives a string version of that
     * value. You can convert numbers to strings with rounding, using an
     * optional string format. See
     * https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html.
     * You can also convert dates to strings using date parsing syntax. See
     * https://docs.openrefine.org/manual/grelfunctions/#date-functions.}.
     * 
     * @return
     *     {@code Takes any value type (string, number, date, boolean, error,
     *     null) and gives a string version of that value. You can convert
     *     numbers to strings with rounding, using an optional string format. See
     *     https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html.
     *     You can also convert dates to strings using date parsing syntax. See
     *     https://docs.openrefine.org/manual/grelfunctions/#date-functions.}
     */
    public static String fun_to_string() {
        return holder.format("fun_to_string");
    }

    /**
     * Key {@code fun_to_string}: {@code Takes any value type (string,
     * number, date, boolean, error, null) and gives a string version of that
     * value. You can convert numbers to strings with rounding, using an
     * optional string format. See
     * https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html.
     * You can also convert dates to strings using date parsing syntax. See
     * https://docs.openrefine.org/manual/grelfunctions/#date-functions.}.
     * 
     * @return
     *     {@code Takes any value type (string, number, date, boolean, error,
     *     null) and gives a string version of that value. You can convert
     *     numbers to strings with rounding, using an optional string format. See
     *     https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html.
     *     You can also convert dates to strings using date parsing syntax. See
     *     https://docs.openrefine.org/manual/grelfunctions/#date-functions.}
     */
    public static Localizable _fun_to_string() {
        return new Localizable(holder, "fun_to_string");
    }

    /**
     * Key {@code str_last_index_of}: {@code Returns the first character
     * index of sub as it last occurs in s; or, returns -1 if s does not
     * contain sub. For example, "parallel".lastIndexOf("a") returns 3
     * (pointing at the second ''a'').}.
     * 
     * @return
     *     {@code Returns the first character index of sub as it last occurs in
     *     s; or, returns -1 if s does not contain sub. For example,
     *     "parallel".lastIndexOf("a") returns 3 (pointing at the second ''a'').}
     */
    public static String str_last_index_of() {
        return holder.format("str_last_index_of");
    }

    /**
     * Key {@code str_last_index_of}: {@code Returns the first character
     * index of sub as it last occurs in s; or, returns -1 if s does not
     * contain sub. For example, "parallel".lastIndexOf("a") returns 3
     * (pointing at the second ''a'').}.
     * 
     * @return
     *     {@code Returns the first character index of sub as it last occurs in
     *     s; or, returns -1 if s does not contain sub. For example,
     *     "parallel".lastIndexOf("a") returns 3 (pointing at the second ''a'').}
     */
    public static Localizable _str_last_index_of() {
        return new Localizable(holder, "str_last_index_of");
    }

    /**
     * Key {@code fun_time_since_unix_epoch_to_date}: {@code Returns a number
     * converted to a date based on Unix Epoch Time. The number can be Unix
     * Epoch Time in one of the following supported units: second,
     * millisecond, microsecond. Defaults to ''second''.}.
     * 
     * @return
     *     {@code Returns a number converted to a date based on Unix Epoch Time.
     *     The number can be Unix Epoch Time in one of the following supported
     *     units: second, millisecond, microsecond. Defaults to ''second''.}
     */
    public static String fun_time_since_unix_epoch_to_date() {
        return holder.format("fun_time_since_unix_epoch_to_date");
    }

    /**
     * Key {@code fun_time_since_unix_epoch_to_date}: {@code Returns a number
     * converted to a date based on Unix Epoch Time. The number can be Unix
     * Epoch Time in one of the following supported units: second,
     * millisecond, microsecond. Defaults to ''second''.}.
     * 
     * @return
     *     {@code Returns a number converted to a date based on Unix Epoch Time.
     *     The number can be Unix Epoch Time in one of the following supported
     *     units: second, millisecond, microsecond. Defaults to ''second''.}
     */
    public static Localizable _fun_time_since_unix_epoch_to_date() {
        return new Localizable(holder, "fun_time_since_unix_epoch_to_date");
    }

    /**
     * Key {@code html_inner_html}: {@code Returns the inner HTML of an HTML
     * element. This will include text and children elements within the
     * element selected. Use it in conjunction with parseHtml() and select()
     * to provide an element.}.
     * 
     * @return
     *     {@code Returns the inner HTML of an HTML element. This will include
     *     text and children elements within the element selected. Use it in
     *     conjunction with parseHtml() and select() to provide an element.}
     */
    public static String html_inner_html() {
        return holder.format("html_inner_html");
    }

    /**
     * Key {@code html_inner_html}: {@code Returns the inner HTML of an HTML
     * element. This will include text and children elements within the
     * element selected. Use it in conjunction with parseHtml() and select()
     * to provide an element.}.
     * 
     * @return
     *     {@code Returns the inner HTML of an HTML element. This will include
     *     text and children elements within the element selected. Use it in
     *     conjunction with parseHtml() and select() to provide an element.}
     */
    public static Localizable _html_inner_html() {
        return new Localizable(holder, "html_inner_html");
    }

    /**
     * Key {@code math_abs}: {@code Returns the absolute value of a number.}.
     * 
     * @return
     *     {@code Returns the absolute value of a number.}
     */
    public static String math_abs() {
        return holder.format("math_abs");
    }

    /**
     * Key {@code math_abs}: {@code Returns the absolute value of a number.}.
     * 
     * @return
     *     {@code Returns the absolute value of a number.}
     */
    public static Localizable _math_abs() {
        return new Localizable(holder, "math_abs");
    }

    /**
     * Key {@code math_quotient}: {@code Returns the integer portion of a
     * division (truncated, not rounded), when supplied with a numerator and
     * denominator.}.
     * 
     * @return
     *     {@code Returns the integer portion of a division (truncated, not
     *     rounded), when supplied with a numerator and denominator.}
     */
    public static String math_quotient() {
        return holder.format("math_quotient");
    }

    /**
     * Key {@code math_quotient}: {@code Returns the integer portion of a
     * division (truncated, not rounded), when supplied with a numerator and
     * denominator.}.
     * 
     * @return
     *     {@code Returns the integer portion of a division (truncated, not
     *     rounded), when supplied with a numerator and denominator.}
     */
    public static Localizable _math_quotient() {
        return new Localizable(holder, "math_quotient");
    }

    /**
     * Key {@code math_pow}: {@code Returns n1 raised to the power of n2.}.
     * 
     * @return
     *     {@code Returns n1 raised to the power of n2.}
     */
    public static String math_pow() {
        return holder.format("math_pow");
    }

    /**
     * Key {@code math_pow}: {@code Returns n1 raised to the power of n2.}.
     * 
     * @return
     *     {@code Returns n1 raised to the power of n2.}
     */
    public static Localizable _math_pow() {
        return new Localizable(holder, "math_pow");
    }

    /**
     * Key {@code math_max}: {@code Returns the greater of two numbers.}.
     * 
     * @return
     *     {@code Returns the greater of two numbers.}
     */
    public static String math_max() {
        return holder.format("math_max");
    }

    /**
     * Key {@code math_max}: {@code Returns the greater of two numbers.}.
     * 
     * @return
     *     {@code Returns the greater of two numbers.}
     */
    public static Localizable _math_max() {
        return new Localizable(holder, "math_max");
    }

    /**
     * Key {@code arr_args_to_array}: {@code Returns all arguments passed to
     * it as an array}.
     * 
     * @return
     *     {@code Returns all arguments passed to it as an array}
     */
    public static String arr_args_to_array() {
        return holder.format("arr_args_to_array");
    }

    /**
     * Key {@code arr_args_to_array}: {@code Returns all arguments passed to
     * it as an array}.
     * 
     * @return
     *     {@code Returns all arguments passed to it as an array}
     */
    public static Localizable _arr_args_to_array() {
        return new Localizable(holder, "arr_args_to_array");
    }

    /**
     * Key {@code math_even}: {@code Rounds the number up to the nearest even
     * integer.}.
     * 
     * @return
     *     {@code Rounds the number up to the nearest even integer.}
     */
    public static String math_even() {
        return holder.format("math_even");
    }

    /**
     * Key {@code math_even}: {@code Rounds the number up to the nearest even
     * integer.}.
     * 
     * @return
     *     {@code Rounds the number up to the nearest even integer.}
     */
    public static Localizable _math_even() {
        return new Localizable(holder, "math_even");
    }

    /**
     * Key {@code fun_cross}: {@code Looks up the given value in the target
     * column of the target project, returns an array of matched rows. Two
     * values match if and only if they have the same string representation.
     * The first argument will be interpreted as cell.value if set to cell.
     * The second argument will be interpreted as the current project name if
     * omitted or set to "". The third argument will be interpreted as the
     * index (starts from 0) column if omitted or set to "".}.
     * 
     * @return
     *     {@code Looks up the given value in the target column of the target
     *     project, returns an array of matched rows. Two values match if and
     *     only if they have the same string representation. The first argument
     *     will be interpreted as cell.value if set to cell. The second argument
     *     will be interpreted as the current project name if omitted or set to
     *     "". The third argument will be interpreted as the index (starts from
     *      0) column if omitted or set to "".}
     */
    public static String fun_cross() {
        return holder.format("fun_cross");
    }

    /**
     * Key {@code fun_cross}: {@code Looks up the given value in the target
     * column of the target project, returns an array of matched rows. Two
     * values match if and only if they have the same string representation.
     * The first argument will be interpreted as cell.value if set to cell.
     * The second argument will be interpreted as the current project name if
     * omitted or set to "". The third argument will be interpreted as the
     * index (starts from 0) column if omitted or set to "".}.
     * 
     * @return
     *     {@code Looks up the given value in the target column of the target
     *     project, returns an array of matched rows. Two values match if and
     *     only if they have the same string representation. The first argument
     *     will be interpreted as cell.value if set to cell. The second argument
     *     will be interpreted as the current project name if omitted or set to
     *     "". The third argument will be interpreted as the index (starts from
     *      0) column if omitted or set to "".}
     */
    public static Localizable _fun_cross() {
        return new Localizable(holder, "fun_cross");
    }

    /**
     * Key {@code math_sum}: {@code Return the sum of the numbers in the
     * array. Ignores non-number items. Returns 0 if the array does not
     * contain numbers.}.
     * 
     * @return
     *     {@code Return the sum of the numbers in the array. Ignores non-number
     *     items. Returns 0 if the array does not contain numbers.}
     */
    public static String math_sum() {
        return holder.format("math_sum");
    }

    /**
     * Key {@code math_sum}: {@code Return the sum of the numbers in the
     * array. Ignores non-number items. Returns 0 if the array does not
     * contain numbers.}.
     * 
     * @return
     *     {@code Return the sum of the numbers in the array. Ignores non-number
     *     items. Returns 0 if the array does not contain numbers.}
     */
    public static Localizable _math_sum() {
        return new Localizable(holder, "math_sum");
    }

    /**
     * Key {@code str_detect_language}: {@code Detects the language of the
     * given string and provides the language code.}.
     * 
     * @return
     *     {@code Detects the language of the given string and provides the
     *     language code.}
     */
    public static String str_detect_language() {
        return holder.format("str_detect_language");
    }

    /**
     * Key {@code str_detect_language}: {@code Detects the language of the
     * given string and provides the language code.}.
     * 
     * @return
     *     {@code Detects the language of the given string and provides the
     *     language code.}
     */
    public static Localizable _str_detect_language() {
        return new Localizable(holder, "str_detect_language");
    }

    /**
     * Key {@code str_parse_uri}: {@code Parses a URI and extracts its
     * components.}.
     * 
     * @return
     *     {@code Parses a URI and extracts its components.}
     */
    public static String str_parse_uri() {
        return holder.format("str_parse_uri");
    }

    /**
     * Key {@code str_parse_uri}: {@code Parses a URI and extracts its
     * components.}.
     * 
     * @return
     *     {@code Parses a URI and extracts its components.}
     */
    public static Localizable _str_parse_uri() {
        return new Localizable(holder, "str_parse_uri");
    }

    /**
     * Key {@code xml_xmltext}: {@code Returns a string of the text from
     * within an HTML or XML element (including all child elements), removing
     * tags and line breaks inside the string. Use it in conjunction with
     * parseHtml() or parseXml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns a string of the text from within an HTML or XML element
     *     (including all child elements), removing tags and line breaks inside
     *     the string. Use it in conjunction with parseHtml() or parseXml() and
     *     select() to provide an element.}
     */
    public static String xml_xmltext() {
        return holder.format("xml_xmltext");
    }

    /**
     * Key {@code xml_xmltext}: {@code Returns a string of the text from
     * within an HTML or XML element (including all child elements), removing
     * tags and line breaks inside the string. Use it in conjunction with
     * parseHtml() or parseXml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns a string of the text from within an HTML or XML element
     *     (including all child elements), removing tags and line breaks inside
     *     the string. Use it in conjunction with parseHtml() or parseXml() and
     *     select() to provide an element.}
     */
    public static Localizable _xml_xmltext() {
        return new Localizable(holder, "xml_xmltext");
    }

    /**
     * Key {@code str_starts_with}: {@code Returns a boolean indicating
     * whether s starts with sub. For example, "food".startsWith("foo")
     * returns true, whereas "food".startsWith("bar") returns false.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s starts with sub. For
     *     example, "food".startsWith("foo") returns true, whereas
     *     "food".startsWith("bar") returns false.}
     */
    public static String str_starts_with() {
        return holder.format("str_starts_with");
    }

    /**
     * Key {@code str_starts_with}: {@code Returns a boolean indicating
     * whether s starts with sub. For example, "food".startsWith("foo")
     * returns true, whereas "food".startsWith("bar") returns false.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s starts with sub. For
     *     example, "food".startsWith("foo") returns true, whereas
     *     "food".startsWith("bar") returns false.}
     */
    public static Localizable _str_starts_with() {
        return new Localizable(holder, "str_starts_with");
    }

    /**
     * Key {@code str_find}: {@code Outputs an array of all consecutive
     * substrings inside string s that match the substring or regex pattern
     * p. You can supply a substring by putting it in quotes.}.
     * 
     * @return
     *     {@code Outputs an array of all consecutive substrings inside string s
     *     that match the substring or regex pattern p. You can supply a
     *     substring by putting it in quotes.}
     */
    public static String str_find() {
        return holder.format("str_find");
    }

    /**
     * Key {@code str_find}: {@code Outputs an array of all consecutive
     * substrings inside string s that match the substring or regex pattern
     * p. You can supply a substring by putting it in quotes.}.
     * 
     * @return
     *     {@code Outputs an array of all consecutive substrings inside string s
     *     that match the substring or regex pattern p. You can supply a
     *     substring by putting it in quotes.}
     */
    public static Localizable _str_find() {
        return new Localizable(holder, "str_find");
    }

    /**
     * Key {@code fun_slice}: {@code Given a string, returns the substring
     * starting from character index from, and up to character index to. If
     * the to argument is omitted, will output to the end of s. Remember
     * character indices start from zero. Given an array, returns a sub-array
     * from the first index provided up to and including the last index
     * provided. If the to value is omitted, it is understood to be the end
     * of the array. Slice only.}.
     * 
     * @return
     *     {@code Given a string, returns the substring starting from character
     *     index from, and up to character index to. If the to argument is
     *     omitted, will output to the end of s. Remember character indices start
     *     from zero. Given an array, returns a sub-array from the first index
     *     provided up to and including the last index provided. If the to value
     *     is omitted, it is understood to be the end of the array. Slice only.}
     */
    public static String fun_slice() {
        return holder.format("fun_slice");
    }

    /**
     * Key {@code fun_slice}: {@code Given a string, returns the substring
     * starting from character index from, and up to character index to. If
     * the to argument is omitted, will output to the end of s. Remember
     * character indices start from zero. Given an array, returns a sub-array
     * from the first index provided up to and including the last index
     * provided. If the to value is omitted, it is understood to be the end
     * of the array. Slice only.}.
     * 
     * @return
     *     {@code Given a string, returns the substring starting from character
     *     index from, and up to character index to. If the to argument is
     *     omitted, will output to the end of s. Remember character indices start
     *     from zero. Given an array, returns a sub-array from the first index
     *     provided up to and including the last index provided. If the to value
     *     is omitted, it is understood to be the end of the array. Slice only.}
     */
    public static Localizable _fun_slice() {
        return new Localizable(holder, "fun_slice");
    }

    /**
     * Key {@code arr_sort}: {@code Sorts the array in ascending order.
     * Sorting is case-sensitive, uppercase first and lowercase second.}.
     * 
     * @return
     *     {@code Sorts the array in ascending order. Sorting is case-sensitive,
     *     uppercase first and lowercase second.}
     */
    public static String arr_sort() {
        return holder.format("arr_sort");
    }

    /**
     * Key {@code arr_sort}: {@code Sorts the array in ascending order.
     * Sorting is case-sensitive, uppercase first and lowercase second.}.
     * 
     * @return
     *     {@code Sorts the array in ascending order. Sorting is case-sensitive,
     *     uppercase first and lowercase second.}
     */
    public static Localizable _arr_sort() {
        return new Localizable(holder, "arr_sort");
    }

    /**
     * Key {@code math_min}: {@code Returns the smaller of two numbers.}.
     * 
     * @return
     *     {@code Returns the smaller of two numbers.}
     */
    public static String math_min() {
        return holder.format("math_min");
    }

    /**
     * Key {@code math_min}: {@code Returns the smaller of two numbers.}.
     * 
     * @return
     *     {@code Returns the smaller of two numbers.}
     */
    public static Localizable _math_min() {
        return new Localizable(holder, "math_min");
    }

    /**
     * Key {@code fun_length}: {@code Returns the length of string s as a
     * number, or the size of array a, meaning the number of objects inside
     * it. Arrays can be empty, in which case length() will return 0.}.
     * 
     * @return
     *     {@code Returns the length of string s as a number, or the size of
     *     array a, meaning the number of objects inside it. Arrays can be empty,
     *     in which case length() will return 0.}
     */
    public static String fun_length() {
        return holder.format("fun_length");
    }

    /**
     * Key {@code fun_length}: {@code Returns the length of string s as a
     * number, or the size of array a, meaning the number of objects inside
     * it. Arrays can be empty, in which case length() will return 0.}.
     * 
     * @return
     *     {@code Returns the length of string s as a number, or the size of
     *     array a, meaning the number of objects inside it. Arrays can be empty,
     *     in which case length() will return 0.}
     */
    public static Localizable _fun_length() {
        return new Localizable(holder, "fun_length");
    }

    /**
     * Key {@code str_partition}: {@code Returns an array of strings [ a,
     * fragment, z ] where a is the substring within s before the first
     * occurrence of fragment, and z is the substring after fragment.
     * Fragment can be a string or a regex. If omitFragment is true, frag is
     * not returned.}.
     * 
     * @return
     *     {@code Returns an array of strings [ a, fragment, z ] where a is the
     *     substring within s before the first occurrence of fragment, and z is
     *     the substring after fragment. Fragment can be a string or a regex. If
     *     omitFragment is true, frag is not returned.}
     */
    public static String str_partition() {
        return holder.format("str_partition");
    }

    /**
     * Key {@code str_partition}: {@code Returns an array of strings [ a,
     * fragment, z ] where a is the substring within s before the first
     * occurrence of fragment, and z is the substring after fragment.
     * Fragment can be a string or a regex. If omitFragment is true, frag is
     * not returned.}.
     * 
     * @return
     *     {@code Returns an array of strings [ a, fragment, z ] where a is the
     *     substring within s before the first occurrence of fragment, and z is
     *     the substring after fragment. Fragment can be a string or a regex. If
     *     omitFragment is true, frag is not returned.}
     */
    public static Localizable _str_partition() {
        return new Localizable(holder, "str_partition");
    }

    /**
     * Key {@code bool_not}: {@code Uses the logical operator NOT on a
     * boolean to output a boolean. For example, not(1 > 7) returns true
     * because 1 > 7 itself is false.}.
     * 
     * @return
     *     {@code Uses the logical operator NOT on a boolean to output a boolean.
     *     For example, not(1 > 7) returns true because 1 > 7 itself is false.}
     */
    public static String bool_not() {
        return holder.format("bool_not");
    }

    /**
     * Key {@code bool_not}: {@code Uses the logical operator NOT on a
     * boolean to output a boolean. For example, not(1 > 7) returns true
     * because 1 > 7 itself is false.}.
     * 
     * @return
     *     {@code Uses the logical operator NOT on a boolean to output a boolean.
     *     For example, not(1 > 7) returns true because 1 > 7 itself is false.}
     */
    public static Localizable _bool_not() {
        return new Localizable(holder, "bool_not");
    }

    /**
     * Key {@code str_range}: {@code Returns an array where a and b are the
     * start and the end of the range respectively and c is the step
     * (increment).}.
     * 
     * @return
     *     {@code Returns an array where a and b are the start and the end of the
     *     range respectively and c is the step (increment).}
     */
    public static String str_range() {
        return holder.format("str_range");
    }

    /**
     * Key {@code str_range}: {@code Returns an array where a and b are the
     * start and the end of the range respectively and c is the step
     * (increment).}.
     * 
     * @return
     *     {@code Returns an array where a and b are the start and the end of the
     *     range respectively and c is the step (increment).}
     */
    public static Localizable _str_range() {
        return new Localizable(holder, "str_range");
    }

    /**
     * Key {@code math_ceil}: {@code Returns the ceiling of a number.}.
     * 
     * @return
     *     {@code Returns the ceiling of a number.}
     */
    public static String math_ceil() {
        return holder.format("math_ceil");
    }

    /**
     * Key {@code math_ceil}: {@code Returns the ceiling of a number.}.
     * 
     * @return
     *     {@code Returns the ceiling of a number.}
     */
    public static Localizable _math_ceil() {
        return new Localizable(holder, "math_ceil");
    }

    /**
     * Key {@code math_log}: {@code Returns the base 10 logarithm of n.}.
     * 
     * @return
     *     {@code Returns the base 10 logarithm of n.}
     */
    public static String math_log() {
        return holder.format("math_log");
    }

    /**
     * Key {@code math_log}: {@code Returns the base 10 logarithm of n.}.
     * 
     * @return
     *     {@code Returns the base 10 logarithm of n.}
     */
    public static Localizable _math_log() {
        return new Localizable(holder, "math_log");
    }

    /**
     * Key {@code xml_parent}: {@code Returns the parent node or null if no
     * parent. Use it in conjunction with parseHtml() and select() to provide
     * an element.}.
     * 
     * @return
     *     {@code Returns the parent node or null if no parent. Use it in
     *     conjunction with parseHtml() and select() to provide an element.}
     */
    public static String xml_parent() {
        return holder.format("xml_parent");
    }

    /**
     * Key {@code xml_parent}: {@code Returns the parent node or null if no
     * parent. Use it in conjunction with parseHtml() and select() to provide
     * an element.}.
     * 
     * @return
     *     {@code Returns the parent node or null if no parent. Use it in
     *     conjunction with parseHtml() and select() to provide an element.}
     */
    public static Localizable _xml_parent() {
        return new Localizable(holder, "xml_parent");
    }

    /**
     * Key {@code str_split}: {@code Returns the array of strings obtained by
     * splitting s by sep. The separator can be either a string or a regex
     * pattern. If preserveTokens is true, empty segments are preserved.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s by sep.
     *     The separator can be either a string or a regex pattern. If
     *     preserveTokens is true, empty segments are preserved.}
     */
    public static String str_split() {
        return holder.format("str_split");
    }

    /**
     * Key {@code str_split}: {@code Returns the array of strings obtained by
     * splitting s by sep. The separator can be either a string or a regex
     * pattern. If preserveTokens is true, empty segments are preserved.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s by sep.
     *     The separator can be either a string or a regex pattern. If
     *     preserveTokens is true, empty segments are preserved.}
     */
    public static Localizable _str_split() {
        return new Localizable(holder, "str_split");
    }

    /**
     * Key {@code str_replace_chars}: {@code Returns the string obtained by
     * replacing a character in s, identified by find, with the corresponding
     * character identified in replace. You cannot use this to replace a
     * single character with more than one character.}.
     * 
     * @return
     *     {@code Returns the string obtained by replacing a character in s,
     *     identified by find, with the corresponding character identified in
     *     replace. You cannot use this to replace a single character with more
     *     than one character.}
     */
    public static String str_replace_chars() {
        return holder.format("str_replace_chars");
    }

    /**
     * Key {@code str_replace_chars}: {@code Returns the string obtained by
     * replacing a character in s, identified by find, with the corresponding
     * character identified in replace. You cannot use this to replace a
     * single character with more than one character.}.
     * 
     * @return
     *     {@code Returns the string obtained by replacing a character in s,
     *     identified by find, with the corresponding character identified in
     *     replace. You cannot use this to replace a single character with more
     *     than one character.}
     */
    public static Localizable _str_replace_chars() {
        return new Localizable(holder, "str_replace_chars");
    }

    /**
     * Key {@code xml_parsexml}: {@code Given a cell full of XML-formatted
     * text, parseXml() returns a full XML document and adds any missing
     * closing tags.}.
     * 
     * @return
     *     {@code Given a cell full of XML-formatted text, parseXml() returns a
     *     full XML document and adds any missing closing tags.}
     */
    public static String xml_parsexml() {
        return holder.format("xml_parsexml");
    }

    /**
     * Key {@code xml_parsexml}: {@code Given a cell full of XML-formatted
     * text, parseXml() returns a full XML document and adds any missing
     * closing tags.}.
     * 
     * @return
     *     {@code Given a cell full of XML-formatted text, parseXml() returns a
     *     full XML document and adds any missing closing tags.}
     */
    public static Localizable _xml_parsexml() {
        return new Localizable(holder, "xml_parsexml");
    }

    /**
     * Key {@code bool_or}: {@code Uses the logical operator OR on two or
     * more booleans to output a boolean. For example, (1 < 3).or(1 > 7)
     * returns true because at least one of the conditions (the first one) is
     * true.}.
     * 
     * @return
     *     {@code Uses the logical operator OR on two or more booleans to output
     *     a boolean. For example, (1 < 3).or(1 > 7) returns true because at
     *     least one of the conditions (the first one) is true.}
     */
    public static String bool_or() {
        return holder.format("bool_or");
    }

    /**
     * Key {@code bool_or}: {@code Uses the logical operator OR on two or
     * more booleans to output a boolean. For example, (1 < 3).or(1 > 7)
     * returns true because at least one of the conditions (the first one) is
     * true.}.
     * 
     * @return
     *     {@code Uses the logical operator OR on two or more booleans to output
     *     a boolean. For example, (1 < 3).or(1 > 7) returns true because at
     *     least one of the conditions (the first one) is true.}
     */
    public static Localizable _bool_or() {
        return new Localizable(holder, "bool_or");
    }

    /**
     * Key {@code str_fingerprint}: {@code Returns the fingerprint of s, a
     * string that is the first step in fingerprint clustering methods: it
     * will trim whitespaces, convert all characters to lowercase, remove
     * punctuation, sort words alphabetically, etc.}.
     * 
     * @return
     *     {@code Returns the fingerprint of s, a string that is the first step
     *     in fingerprint clustering methods: it will trim whitespaces, convert
     *     all characters to lowercase, remove punctuation, sort words
     *     alphabetically, etc.}
     */
    public static String str_fingerprint() {
        return holder.format("str_fingerprint");
    }

    /**
     * Key {@code str_fingerprint}: {@code Returns the fingerprint of s, a
     * string that is the first step in fingerprint clustering methods: it
     * will trim whitespaces, convert all characters to lowercase, remove
     * punctuation, sort words alphabetically, etc.}.
     * 
     * @return
     *     {@code Returns the fingerprint of s, a string that is the first step
     *     in fingerprint clustering methods: it will trim whitespaces, convert
     *     all characters to lowercase, remove punctuation, sort words
     *     alphabetically, etc.}
     */
    public static Localizable _str_fingerprint() {
        return new Localizable(holder, "str_fingerprint");
    }

    /**
     * Key {@code arr_join}: {@code Joins the items in the array with sep,
     * and returns it all as a string.}.
     * 
     * @return
     *     {@code Joins the items in the array with sep, and returns it all as a
     *     string.}
     */
    public static String arr_join() {
        return holder.format("arr_join");
    }

    /**
     * Key {@code arr_join}: {@code Joins the items in the array with sep,
     * and returns it all as a string.}.
     * 
     * @return
     *     {@code Joins the items in the array with sep, and returns it all as a
     *     string.}
     */
    public static Localizable _arr_join() {
        return new Localizable(holder, "arr_join");
    }

    /**
     * Key {@code math_odd}: {@code Rounds the number up to the nearest odd
     * integer.}.
     * 
     * @return
     *     {@code Rounds the number up to the nearest odd integer.}
     */
    public static String math_odd() {
        return holder.format("math_odd");
    }

    /**
     * Key {@code math_odd}: {@code Rounds the number up to the nearest odd
     * integer.}.
     * 
     * @return
     *     {@code Rounds the number up to the nearest odd integer.}
     */
    public static Localizable _math_odd() {
        return new Localizable(holder, "math_odd");
    }

    /**
     * Key {@code bool_and}: {@code Uses the logical operator AND on two or
     * more booleans to output a boolean. Evaluates multiple statements into
     * booleans, then returns true if all the statements are true. For
     * example, (1 < 3).and(1 < 0) returns false because one condition is
     * true and one is false.}.
     * 
     * @return
     *     {@code Uses the logical operator AND on two or more booleans to output
     *     a boolean. Evaluates multiple statements into booleans, then returns
     *     true if all the statements are true. For example, (1 < 3).and(1 < 0)
     *     returns false because one condition is true and one is false.}
     */
    public static String bool_and() {
        return holder.format("bool_and");
    }

    /**
     * Key {@code bool_and}: {@code Uses the logical operator AND on two or
     * more booleans to output a boolean. Evaluates multiple statements into
     * booleans, then returns true if all the statements are true. For
     * example, (1 < 3).and(1 < 0) returns false because one condition is
     * true and one is false.}.
     * 
     * @return
     *     {@code Uses the logical operator AND on two or more booleans to output
     *     a boolean. Evaluates multiple statements into booleans, then returns
     *     true if all the statements are true. For example, (1 < 3).and(1 < 0)
     *     returns false because one condition is true and one is false.}
     */
    public static Localizable _bool_and() {
        return new Localizable(holder, "bool_and");
    }

    /**
     * Key {@code str_index_of}: {@code Returns the first character index of
     * sub as it first occurs in s; or, returns -1 if s does not contain sub.
     * For example, "internationalization".indexOf("nation") returns 5.}.
     * 
     * @return
     *     {@code Returns the first character index of sub as it first occurs in
     *     s; or, returns -1 if s does not contain sub. For example,
     *     "internationalization".indexOf("nation") returns 5.}
     */
    public static String str_index_of() {
        return holder.format("str_index_of");
    }

    /**
     * Key {@code str_index_of}: {@code Returns the first character index of
     * sub as it first occurs in s; or, returns -1 if s does not contain sub.
     * For example, "internationalization".indexOf("nation") returns 5.}.
     * 
     * @return
     *     {@code Returns the first character index of sub as it first occurs in
     *     s; or, returns -1 if s does not contain sub. For example,
     *     "internationalization".indexOf("nation") returns 5.}
     */
    public static Localizable _str_index_of() {
        return new Localizable(holder, "str_index_of");
    }

    /**
     * Key {@code str_ngram}: {@code Returns an array of the word n-grams of
     * s. That is, it lists all the possible consecutive combinations of n
     * words in the string.}.
     * 
     * @return
     *     {@code Returns an array of the word n-grams of s. That is, it lists
     *     all the possible consecutive combinations of n words in the string.}
     */
    public static String str_ngram() {
        return holder.format("str_ngram");
    }

    /**
     * Key {@code str_ngram}: {@code Returns an array of the word n-grams of
     * s. That is, it lists all the possible consecutive combinations of n
     * words in the string.}.
     * 
     * @return
     *     {@code Returns an array of the word n-grams of s. That is, it lists
     *     all the possible consecutive combinations of n words in the string.}
     */
    public static Localizable _str_ngram() {
        return new Localizable(holder, "str_ngram");
    }

    /**
     * Key {@code math_exp}: {@code Returns e to the power of n.}.
     * 
     * @return
     *     {@code Returns e to the power of n.}
     */
    public static String math_exp() {
        return holder.format("math_exp");
    }

    /**
     * Key {@code math_exp}: {@code Returns e to the power of n.}.
     * 
     * @return
     *     {@code Returns e to the power of n.}
     */
    public static Localizable _math_exp() {
        return new Localizable(holder, "math_exp");
    }

    /**
     * Key {@code str_unescape}: {@code Unescapes s in the given escaping
     * mode. The mode can be one of: ''html'', ''xml'', ''csv'', ''url'',
     * ''javascript''. Note that quotes are required around your mode.}.
     * 
     * @return
     *     {@code Unescapes s in the given escaping mode. The mode can be one of:
     *     ''html'', ''xml'', ''csv'', ''url'', ''javascript''. Note that quotes
     *     are required around your mode.}
     */
    public static String str_unescape() {
        return holder.format("str_unescape");
    }

    /**
     * Key {@code str_unescape}: {@code Unescapes s in the given escaping
     * mode. The mode can be one of: ''html'', ''xml'', ''csv'', ''url'',
     * ''javascript''. Note that quotes are required around your mode.}.
     * 
     * @return
     *     {@code Unescapes s in the given escaping mode. The mode can be one of:
     *     ''html'', ''xml'', ''csv'', ''url'', ''javascript''. Note that quotes
     *     are required around your mode.}
     */
    public static Localizable _str_unescape() {
        return new Localizable(holder, "str_unescape");
    }

    /**
     * Key {@code xml_xmlattr}: {@code Returns a string from an attribute on
     * an XML or HTML element. Use it in conjunction with parseHtml() or
     * parseXml() to point to an element first.}.
     * 
     * @return
     *     {@code Returns a string from an attribute on an XML or HTML element.
     *     Use it in conjunction with parseHtml() or parseXml() to point to an
     *     element first.}
     */
    public static String xml_xmlattr() {
        return holder.format("xml_xmlattr");
    }

    /**
     * Key {@code xml_xmlattr}: {@code Returns a string from an attribute on
     * an XML or HTML element. Use it in conjunction with parseHtml() or
     * parseXml() to point to an element first.}.
     * 
     * @return
     *     {@code Returns a string from an attribute on an XML or HTML element.
     *     Use it in conjunction with parseHtml() or parseXml() to point to an
     *     element first.}
     */
    public static Localizable _xml_xmlattr() {
        return new Localizable(holder, "xml_xmlattr");
    }

    /**
     * Key {@code math_radians}: {@code Converts an angle in degrees to
     * radians.}.
     * 
     * @return
     *     {@code Converts an angle in degrees to radians.}
     */
    public static String math_radians() {
        return holder.format("math_radians");
    }

    /**
     * Key {@code math_radians}: {@code Converts an angle in degrees to
     * radians.}.
     * 
     * @return
     *     {@code Converts an angle in degrees to radians.}
     */
    public static Localizable _math_radians() {
        return new Localizable(holder, "math_radians");
    }

    /**
     * Key {@code str_phonetic}: {@code Returns a phonetic encoding of a
     * string, based on an available phonetic algorithm. Can be one of the
     * following supported phonetic methods: metaphone, doublemetaphone,
     * metaphone3, soundex, cologne. Defaults to ''metaphone3''.}.
     * 
     * @return
     *     {@code Returns a phonetic encoding of a string, based on an available
     *     phonetic algorithm. Can be one of the following supported phonetic
     *     methods: metaphone, doublemetaphone, metaphone3, soundex, cologne.
     *     Defaults to ''metaphone3''.}
     */
    public static String str_phonetic() {
        return holder.format("str_phonetic");
    }

    /**
     * Key {@code str_phonetic}: {@code Returns a phonetic encoding of a
     * string, based on an available phonetic algorithm. Can be one of the
     * following supported phonetic methods: metaphone, doublemetaphone,
     * metaphone3, soundex, cologne. Defaults to ''metaphone3''.}.
     * 
     * @return
     *     {@code Returns a phonetic encoding of a string, based on an available
     *     phonetic algorithm. Can be one of the following supported phonetic
     *     methods: metaphone, doublemetaphone, metaphone3, soundex, cologne.
     *     Defaults to ''metaphone3''.}
     */
    public static Localizable _str_phonetic() {
        return new Localizable(holder, "str_phonetic");
    }

    /**
     * Key {@code str_smart_split}: {@code Returns the array of strings
     * obtained by splitting s by sep, or by guessing either tab or comma
     * separation if there is no sep given. Handles quotes properly and
     * understands cancelled characters. The separator can be either a string
     * or a regex pattern.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s by sep, or
     *     by guessing either tab or comma separation if there is no sep given.
     *     Handles quotes properly and understands cancelled characters. The
     *     separator can be either a string or a regex pattern.}
     */
    public static String str_smart_split() {
        return holder.format("str_smart_split");
    }

    /**
     * Key {@code str_smart_split}: {@code Returns the array of strings
     * obtained by splitting s by sep, or by guessing either tab or comma
     * separation if there is no sep given. Handles quotes properly and
     * understands cancelled characters. The separator can be either a string
     * or a regex pattern.}.
     * 
     * @return
     *     {@code Returns the array of strings obtained by splitting s by sep, or
     *     by guessing either tab or comma separation if there is no sep given.
     *     Handles quotes properly and understands cancelled characters. The
     *     separator can be either a string or a regex pattern.}
     */
    public static Localizable _str_smart_split() {
        return new Localizable(holder, "str_smart_split");
    }

    /**
     * Key {@code fun_to_number}: {@code Returns a string converted to a
     * number. Will attempt to convert other formats into a string, then into
     * a number. If the value is already a number, it will return the
     * number.}.
     * 
     * @return
     *     {@code Returns a string converted to a number. Will attempt to convert
     *     other formats into a string, then into a number. If the value is
     *     already a number, it will return the number.}
     */
    public static String fun_to_number() {
        return holder.format("fun_to_number");
    }

    /**
     * Key {@code fun_to_number}: {@code Returns a string converted to a
     * number. Will attempt to convert other formats into a string, then into
     * a number. If the value is already a number, it will return the
     * number.}.
     * 
     * @return
     *     {@code Returns a string converted to a number. Will attempt to convert
     *     other formats into a string, then into a number. If the value is
     *     already a number, it will return the number.}
     */
    public static Localizable _fun_to_number() {
        return new Localizable(holder, "fun_to_number");
    }

    /**
     * Key {@code xml_owntext}: {@code Returns the text directly inside the
     * selected XML or HTML element only, ignoring text inside children
     * elements (for this, use innerXml()). Use it in conjunction with a
     * parser and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns the text directly inside the selected XML or HTML
     *     element only, ignoring text inside children elements (for this, use
     *     innerXml()). Use it in conjunction with a parser and select() to
     *     provide an element.}
     */
    public static String xml_owntext() {
        return holder.format("xml_owntext");
    }

    /**
     * Key {@code xml_owntext}: {@code Returns the text directly inside the
     * selected XML or HTML element only, ignoring text inside children
     * elements (for this, use innerXml()). Use it in conjunction with a
     * parser and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns the text directly inside the selected XML or HTML
     *     element only, ignoring text inside children elements (for this, use
     *     innerXml()). Use it in conjunction with a parser and select() to
     *     provide an element.}
     */
    public static Localizable _xml_owntext() {
        return new Localizable(holder, "xml_owntext");
    }

    /**
     * Key {@code str_to_title_case}: {@code Returns string s converted into
     * titlecase: a capital letter starting each word, and the rest of the
     * letters lowercase. For example, ''Once upon a midnight
     * DREARY''.toTitlecase() returns the string ''Once Upon A Midnight
     * Dreary''.}.
     * 
     * @return
     *     {@code Returns string s converted into titlecase: a capital letter
     *     starting each word, and the rest of the letters lowercase. For
     *     example, ''Once upon a midnight DREARY''.toTitlecase() returns the
     *     string ''Once Upon A Midnight Dreary''.}
     */
    public static String str_to_title_case() {
        return holder.format("str_to_title_case");
    }

    /**
     * Key {@code str_to_title_case}: {@code Returns string s converted into
     * titlecase: a capital letter starting each word, and the rest of the
     * letters lowercase. For example, ''Once upon a midnight
     * DREARY''.toTitlecase() returns the string ''Once Upon A Midnight
     * Dreary''.}.
     * 
     * @return
     *     {@code Returns string s converted into titlecase: a capital letter
     *     starting each word, and the rest of the letters lowercase. For
     *     example, ''Once upon a midnight DREARY''.toTitlecase() returns the
     *     string ''Once Upon A Midnight Dreary''.}
     */
    public static Localizable _str_to_title_case() {
        return new Localizable(holder, "str_to_title_case");
    }

    /**
     * Key {@code fun_facet_count}: {@code Returns the facet count
     * corresponding to the given choice value, by looking for the
     * facetExpression in the choiceValue in columnName.}.
     * 
     * @return
     *     {@code Returns the facet count corresponding to the given choice
     *     value, by looking for the facetExpression in the choiceValue in
     *     columnName.}
     */
    public static String fun_facet_count() {
        return holder.format("fun_facet_count");
    }

    /**
     * Key {@code fun_facet_count}: {@code Returns the facet count
     * corresponding to the given choice value, by looking for the
     * facetExpression in the choiceValue in columnName.}.
     * 
     * @return
     *     {@code Returns the facet count corresponding to the given choice
     *     value, by looking for the facetExpression in the choiceValue in
     *     columnName.}
     */
    public static Localizable _fun_facet_count() {
        return new Localizable(holder, "fun_facet_count");
    }

    /**
     * Key {@code str_match}: {@code Attempts to match the string s in its
     * entirety against the regex pattern p and, if the pattern is found,
     * outputs an array of all capturing groups (found in order).}.
     * 
     * @return
     *     {@code Attempts to match the string s in its entirety against the
     *     regex pattern p and, if the pattern is found, outputs an array of all
     *     capturing groups (found in order).}
     */
    public static String str_match() {
        return holder.format("str_match");
    }

    /**
     * Key {@code str_match}: {@code Attempts to match the string s in its
     * entirety against the regex pattern p and, if the pattern is found,
     * outputs an array of all capturing groups (found in order).}.
     * 
     * @return
     *     {@code Attempts to match the string s in its entirety against the
     *     regex pattern p and, if the pattern is found, outputs an array of all
     *     capturing groups (found in order).}
     */
    public static Localizable _str_match() {
        return new Localizable(holder, "str_match");
    }

    /**
     * Key {@code math_combin}: {@code Returns the number of combinations for
     * n2 elements as divided into n1.}.
     * 
     * @return
     *     {@code Returns the number of combinations for n2 elements as divided
     *     into n1.}
     */
    public static String math_combin() {
        return holder.format("math_combin");
    }

    /**
     * Key {@code math_combin}: {@code Returns the number of combinations for
     * n2 elements as divided into n1.}.
     * 
     * @return
     *     {@code Returns the number of combinations for n2 elements as divided
     *     into n1.}
     */
    public static Localizable _math_combin() {
        return new Localizable(holder, "math_combin");
    }

    /**
     * Key {@code str_md5}: {@code Returns the MD5 hash of an object. If fed
     * something other than a string (array, number, date, etc.), md5() will
     * convert it to a string and deliver the hash of the string.}.
     * 
     * @return
     *     {@code Returns the MD5 hash of an object. If fed something other than
     *     a string (array, number, date, etc.), md5() will convert it to a
     *     string and deliver the hash of the string.}
     */
    public static String str_md5() {
        return holder.format("str_md5");
    }

    /**
     * Key {@code str_md5}: {@code Returns the MD5 hash of an object. If fed
     * something other than a string (array, number, date, etc.), md5() will
     * convert it to a string and deliver the hash of the string.}.
     * 
     * @return
     *     {@code Returns the MD5 hash of an object. If fed something other than
     *     a string (array, number, date, etc.), md5() will convert it to a
     *     string and deliver the hash of the string.}
     */
    public static Localizable _str_md5() {
        return new Localizable(holder, "str_md5");
    }

    /**
     * Key {@code math_gcd}: {@code Returns the greatest common denominator
     * of two numbers.}.
     * 
     * @return
     *     {@code Returns the greatest common denominator of two numbers.}
     */
    public static String math_gcd() {
        return holder.format("math_gcd");
    }

    /**
     * Key {@code math_gcd}: {@code Returns the greatest common denominator
     * of two numbers.}.
     * 
     * @return
     *     {@code Returns the greatest common denominator of two numbers.}
     */
    public static Localizable _math_gcd() {
        return new Localizable(holder, "math_gcd");
    }

    /**
     * Key {@code str_decode}: {@code Decodes a string using the specified
     * encoding. Encodings include Base16, Base32Hex, Base32, Base64, and
     * Base64Url.}.
     * 
     * @return
     *     {@code Decodes a string using the specified encoding. Encodings
     *     include Base16, Base32Hex, Base32, Base64, and Base64Url.}
     */
    public static String str_decode() {
        return holder.format("str_decode");
    }

    /**
     * Key {@code str_decode}: {@code Decodes a string using the specified
     * encoding. Encodings include Base16, Base32Hex, Base32, Base64, and
     * Base64Url.}.
     * 
     * @return
     *     {@code Decodes a string using the specified encoding. Encodings
     *     include Base16, Base32Hex, Base32, Base64, and Base64Url.}
     */
    public static Localizable _str_decode() {
        return new Localizable(holder, "str_decode");
    }

    /**
     * Key {@code math_floor}: {@code Returns the floor of a number.}.
     * 
     * @return
     *     {@code Returns the floor of a number.}
     */
    public static String math_floor() {
        return holder.format("math_floor");
    }

    /**
     * Key {@code math_floor}: {@code Returns the floor of a number.}.
     * 
     * @return
     *     {@code Returns the floor of a number.}
     */
    public static Localizable _math_floor() {
        return new Localizable(holder, "math_floor");
    }

    /**
     * Key {@code str_encode}: {@code Encodes a string using the specified
     * encoding. Encodings include Base16, Base32Hex, Base32, Base64, and
     * Base64Url.}.
     * 
     * @return
     *     {@code Encodes a string using the specified encoding. Encodings
     *     include Base16, Base32Hex, Base32, Base64, and Base64Url.}
     */
    public static String str_encode() {
        return holder.format("str_encode");
    }

    /**
     * Key {@code str_encode}: {@code Encodes a string using the specified
     * encoding. Encodings include Base16, Base32Hex, Base32, Base64, and
     * Base64Url.}.
     * 
     * @return
     *     {@code Encodes a string using the specified encoding. Encodings
     *     include Base16, Base32Hex, Base32, Base64, and Base64Url.}
     */
    public static Localizable _str_encode() {
        return new Localizable(holder, "str_encode");
    }

    /**
     * Key {@code str_to_lowercase}: {@code Returns string s converted to all
     * lowercase characters.}.
     * 
     * @return
     *     {@code Returns string s converted to all lowercase characters.}
     */
    public static String str_to_lowercase() {
        return holder.format("str_to_lowercase");
    }

    /**
     * Key {@code str_to_lowercase}: {@code Returns string s converted to all
     * lowercase characters.}.
     * 
     * @return
     *     {@code Returns string s converted to all lowercase characters.}
     */
    public static Localizable _str_to_lowercase() {
        return new Localizable(holder, "str_to_lowercase");
    }

    /**
     * Key {@code arr_uniques}: {@code Returns the array with duplicates
     * removed. Case-sensitive.}.
     * 
     * @return
     *     {@code Returns the array with duplicates removed. Case-sensitive.}
     */
    public static String arr_uniques() {
        return holder.format("arr_uniques");
    }

    /**
     * Key {@code arr_uniques}: {@code Returns the array with duplicates
     * removed. Case-sensitive.}.
     * 
     * @return
     *     {@code Returns the array with duplicates removed. Case-sensitive.}
     */
    public static Localizable _arr_uniques() {
        return new Localizable(holder, "arr_uniques");
    }

    /**
     * Key {@code fun_coalesce}: {@code Returns the first non-null from a
     * series of objects (meaning any data type - string, date, number,
     * boolean, etc.).}.
     * 
     * @return
     *     {@code Returns the first non-null from a series of objects (meaning
     *     any data type - string, date, number, boolean, etc.).}
     */
    public static String fun_coalesce() {
        return holder.format("fun_coalesce");
    }

    /**
     * Key {@code fun_coalesce}: {@code Returns the first non-null from a
     * series of objects (meaning any data type - string, date, number,
     * boolean, etc.).}.
     * 
     * @return
     *     {@code Returns the first non-null from a series of objects (meaning
     *     any data type - string, date, number, boolean, etc.).}
     */
    public static Localizable _fun_coalesce() {
        return new Localizable(holder, "fun_coalesce");
    }

    /**
     * Key {@code math_random_number}: {@code Returns a random integer in the
     * interval between the lower and upper bounds (inclusively). Will output
     * a different random number in each cell in a column.}.
     * 
     * @return
     *     {@code Returns a random integer in the interval between the lower and
     *     upper bounds (inclusively). Will output a different random number in
     *     each cell in a column.}
     */
    public static String math_random_number() {
        return holder.format("math_random_number");
    }

    /**
     * Key {@code math_random_number}: {@code Returns a random integer in the
     * interval between the lower and upper bounds (inclusively). Will output
     * a different random number in each cell in a column.}.
     * 
     * @return
     *     {@code Returns a random integer in the interval between the lower and
     *     upper bounds (inclusively). Will output a different random number in
     *     each cell in a column.}
     */
    public static Localizable _math_random_number() {
        return new Localizable(holder, "math_random_number");
    }

    /**
     * Key {@code math_mod}: {@code Returns n1 modulus n2.}.
     * 
     * @return
     *     {@code Returns n1 modulus n2.}
     */
    public static String math_mod() {
        return holder.format("math_mod");
    }

    /**
     * Key {@code math_mod}: {@code Returns n1 modulus n2.}.
     * 
     * @return
     *     {@code Returns n1 modulus n2.}
     */
    public static Localizable _math_mod() {
        return new Localizable(holder, "math_mod");
    }

    /**
     * Key {@code xml_wholetext}: {@code Selects the (unencoded) text of an
     * element and its children, including any new lines and spaces, and
     * returns a string of unencoded, un-normalized text. Use it in
     * conjunction with parseHtml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Selects the (unencoded) text of an element and its children,
     *     including any new lines and spaces, and returns a string of unencoded,
     *     un-normalized text. Use it in conjunction with parseHtml() and
     *     select() to provide an element.}
     */
    public static String xml_wholetext() {
        return holder.format("xml_wholetext");
    }

    /**
     * Key {@code xml_wholetext}: {@code Selects the (unencoded) text of an
     * element and its children, including any new lines and spaces, and
     * returns a string of unencoded, un-normalized text. Use it in
     * conjunction with parseHtml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Selects the (unencoded) text of an element and its children,
     *     including any new lines and spaces, and returns a string of unencoded,
     *     un-normalized text. Use it in conjunction with parseHtml() and
     *     select() to provide an element.}
     */
    public static Localizable _xml_wholetext() {
        return new Localizable(holder, "xml_wholetext");
    }

    /**
     * Key {@code date_part}: {@code Returns part of a date. The data type
     * returned depends on the unit. See
     * https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit,
     * https://docs.openrefine.org/manual/grelfunctions#date-functions for a
     * table.}.
     * 
     * @return
     *     {@code Returns part of a date. The data type returned depends on the
     *     unit. See
     *     https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit,
     *     https://docs.openrefine.org/manual/grelfunctions#date-functions for a
     *     table.}
     */
    public static String date_part() {
        return holder.format("date_part");
    }

    /**
     * Key {@code date_part}: {@code Returns part of a date. The data type
     * returned depends on the unit. See
     * https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit,
     * https://docs.openrefine.org/manual/grelfunctions#date-functions for a
     * table.}.
     * 
     * @return
     *     {@code Returns part of a date. The data type returned depends on the
     *     unit. See
     *     https://docs.openrefine.org/manual/grelfunctions/#datepartd-s-timeunit,
     *     https://docs.openrefine.org/manual/grelfunctions#date-functions for a
     *     table.}
     */
    public static Localizable _date_part() {
        return new Localizable(holder, "date_part");
    }

    /**
     * Key {@code str_ends_with}: {@code Returns a boolean indicating whether
     * s ends with sub. For example, "food".endsWith("ood") returns true,
     * whereas "food".endsWith("odd") returns false.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s ends with sub. For
     *     example, "food".endsWith("ood") returns true, whereas
     *     "food".endsWith("odd") returns false.}
     */
    public static String str_ends_with() {
        return holder.format("str_ends_with");
    }

    /**
     * Key {@code str_ends_with}: {@code Returns a boolean indicating whether
     * s ends with sub. For example, "food".endsWith("ood") returns true,
     * whereas "food".endsWith("odd") returns false.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s ends with sub. For
     *     example, "food".endsWith("ood") returns true, whereas
     *     "food".endsWith("odd") returns false.}
     */
    public static Localizable _str_ends_with() {
        return new Localizable(holder, "str_ends_with");
    }

    /**
     * Key {@code math_round}: {@code Rounds a number to the nearest
     * integer.}.
     * 
     * @return
     *     {@code Rounds a number to the nearest integer.}
     */
    public static String math_round() {
        return holder.format("math_round");
    }

    /**
     * Key {@code math_round}: {@code Rounds a number to the nearest
     * integer.}.
     * 
     * @return
     *     {@code Rounds a number to the nearest integer.}
     */
    public static Localizable _math_round() {
        return new Localizable(holder, "math_round");
    }

    /**
     * Key {@code str_to_uppercase}: {@code Returns string s converted to all
     * uppercase characters.}.
     * 
     * @return
     *     {@code Returns string s converted to all uppercase characters.}
     */
    public static String str_to_uppercase() {
        return holder.format("str_to_uppercase");
    }

    /**
     * Key {@code str_to_uppercase}: {@code Returns string s converted to all
     * uppercase characters.}.
     * 
     * @return
     *     {@code Returns string s converted to all uppercase characters.}
     */
    public static Localizable _str_to_uppercase() {
        return new Localizable(holder, "str_to_uppercase");
    }

    /**
     * Key {@code xml_scripttext}: {@code Returns the combined data of an
     * HTML/XML Element. Data is e.g. the inside of a &lt;script&gt;
     * tag.
     * Note that data is NOT the text of the element.
     * Use htmlText() to
     * get the text that would be visible to a user, and scriptText() for the
     * contents of &lt;script&gt;, &lt;style&gt;, etc.
     * Use scriptText() in
     * conjunction with parseHtml() and select().}.
     * 
     * @return
     *     {@code Returns the combined data of an HTML/XML Element. Data is e.g.
     *     the inside of a &lt;script&gt; tag.
     *     Note that data is NOT the text of
     *     the element.
     *     Use htmlText() to get the text that would be visible to a
     *     user, and scriptText() for the contents of &lt;script&gt;,
     *     &lt;style&gt;, etc.
     *     Use scriptText() in conjunction with parseHtml()
     *     and select().}
     */
    public static String xml_scripttext() {
        return holder.format("xml_scripttext");
    }

    /**
     * Key {@code xml_scripttext}: {@code Returns the combined data of an
     * HTML/XML Element. Data is e.g. the inside of a &lt;script&gt;
     * tag.
     * Note that data is NOT the text of the element.
     * Use htmlText() to
     * get the text that would be visible to a user, and scriptText() for the
     * contents of &lt;script&gt;, &lt;style&gt;, etc.
     * Use scriptText() in
     * conjunction with parseHtml() and select().}.
     * 
     * @return
     *     {@code Returns the combined data of an HTML/XML Element. Data is e.g.
     *     the inside of a &lt;script&gt; tag.
     *     Note that data is NOT the text of
     *     the element.
     *     Use htmlText() to get the text that would be visible to a
     *     user, and scriptText() for the contents of &lt;script&gt;,
     *     &lt;style&gt;, etc.
     *     Use scriptText() in conjunction with parseHtml()
     *     and select().}
     */
    public static Localizable _xml_scripttext() {
        return new Localizable(holder, "xml_scripttext");
    }

    /**
     * Key {@code math_sin}: {@code Returns the trigonometric sine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric sine of an angle.}
     */
    public static String math_sin() {
        return holder.format("math_sin");
    }

    /**
     * Key {@code math_sin}: {@code Returns the trigonometric sine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the trigonometric sine of an angle.}
     */
    public static Localizable _math_sin() {
        return new Localizable(holder, "math_sin");
    }

    /**
     * Key {@code math_sinh}: {@code Returns the hyperbolic sine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the hyperbolic sine of an angle.}
     */
    public static String math_sinh() {
        return holder.format("math_sinh");
    }

    /**
     * Key {@code math_sinh}: {@code Returns the hyperbolic sine of an
     * angle.}.
     * 
     * @return
     *     {@code Returns the hyperbolic sine of an angle.}
     */
    public static Localizable _math_sinh() {
        return new Localizable(holder, "math_sinh");
    }

    /**
     * Key {@code str_contains}: {@code Returns a boolean indicating whether
     * s contains sub, which is either a substring or a regex pattern. For
     * example, "food".contains("oo") returns true.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s contains sub, which is
     *     either a substring or a regex pattern. For example,
     *     "food".contains("oo") returns true.}
     */
    public static String str_contains() {
        return holder.format("str_contains");
    }

    /**
     * Key {@code str_contains}: {@code Returns a boolean indicating whether
     * s contains sub, which is either a substring or a regex pattern. For
     * example, "food".contains("oo") returns true.}.
     * 
     * @return
     *     {@code Returns a boolean indicating whether s contains sub, which is
     *     either a substring or a regex pattern. For example,
     *     "food".contains("oo") returns true.}
     */
    public static Localizable _str_contains() {
        return new Localizable(holder, "str_contains");
    }

    /**
     * Key {@code str_escape}: {@code Escapes s in the given escaping mode.
     * The mode can be one of: ''html'', ''xml'', csv'', ''url'',
     * ''javascript''. Note that quotes are required around your mode.}.
     * 
     * @return
     *     {@code Escapes s in the given escaping mode. The mode can be one of:
     *     ''html'', ''xml'', csv'', ''url'', ''javascript''. Note that quotes
     *     are required around your mode.}
     */
    public static String str_escape() {
        return holder.format("str_escape");
    }

    /**
     * Key {@code str_escape}: {@code Escapes s in the given escaping mode.
     * The mode can be one of: ''html'', ''xml'', csv'', ''url'',
     * ''javascript''. Note that quotes are required around your mode.}.
     * 
     * @return
     *     {@code Escapes s in the given escaping mode. The mode can be one of:
     *     ''html'', ''xml'', csv'', ''url'', ''javascript''. Note that quotes
     *     are required around your mode.}
     */
    public static Localizable _str_escape() {
        return new Localizable(holder, "str_escape");
    }

    /**
     * Key {@code str_split_by_char_type}: {@code Returns an array of strings
     * obtained by splitting s into groups of consecutive characters each
     * time the characters change Unicode categories.}.
     * 
     * @return
     *     {@code Returns an array of strings obtained by splitting s into groups
     *     of consecutive characters each time the characters change Unicode
     *     categories.}
     */
    public static String str_split_by_char_type() {
        return holder.format("str_split_by_char_type");
    }

    /**
     * Key {@code str_split_by_char_type}: {@code Returns an array of strings
     * obtained by splitting s into groups of consecutive characters each
     * time the characters change Unicode categories.}.
     * 
     * @return
     *     {@code Returns an array of strings obtained by splitting s into groups
     *     of consecutive characters each time the characters change Unicode
     *     categories.}
     */
    public static Localizable _str_split_by_char_type() {
        return new Localizable(holder, "str_split_by_char_type");
    }

    /**
     * Key {@code math_atan}: {@code Returns the arc tangent of an angle in
     * the range of -PI/2 through PI/2.}.
     * 
     * @return
     *     {@code Returns the arc tangent of an angle in the range of -PI/2
     *     through PI/2.}
     */
    public static String math_atan() {
        return holder.format("math_atan");
    }

    /**
     * Key {@code math_atan}: {@code Returns the arc tangent of an angle in
     * the range of -PI/2 through PI/2.}.
     * 
     * @return
     *     {@code Returns the arc tangent of an angle in the range of -PI/2
     *     through PI/2.}
     */
    public static Localizable _math_atan() {
        return new Localizable(holder, "math_atan");
    }

    /**
     * Key {@code xml_innerxml}: {@code Returns the inner XML elements of an
     * XML element. Does not return the text directly inside your chosen XML
     * element - only the contents of its children. Use it in conjunction
     * with parseXml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns the inner XML elements of an XML element. Does not
     *     return the text directly inside your chosen XML element - only the
     *     contents of its children. Use it in conjunction with parseXml() and
     *     select() to provide an element.}
     */
    public static String xml_innerxml() {
        return holder.format("xml_innerxml");
    }

    /**
     * Key {@code xml_innerxml}: {@code Returns the inner XML elements of an
     * XML element. Does not return the text directly inside your chosen XML
     * element - only the contents of its children. Use it in conjunction
     * with parseXml() and select() to provide an element.}.
     * 
     * @return
     *     {@code Returns the inner XML elements of an XML element. Does not
     *     return the text directly inside your chosen XML element - only the
     *     contents of its children. Use it in conjunction with parseXml() and
     *     select() to provide an element.}
     */
    public static Localizable _xml_innerxml() {
        return new Localizable(holder, "xml_innerxml");
    }

    /**
     * Key {@code str_rpartition}: {@code Returns an array of strings [ a,
     * fragment, z ] where a is the substring within s before the last
     * occurrence of fragment, and z is the substring after the last instance
     * of fragment. If omitFragment is true, frag is not returned.}.
     * 
     * @return
     *     {@code Returns an array of strings [ a, fragment, z ] where a is the
     *     substring within s before the last occurrence of fragment, and z is
     *     the substring after the last instance of fragment. If omitFragment is
     *     true, frag is not returned.}
     */
    public static String str_rpartition() {
        return holder.format("str_rpartition");
    }

    /**
     * Key {@code str_rpartition}: {@code Returns an array of strings [ a,
     * fragment, z ] where a is the substring within s before the last
     * occurrence of fragment, and z is the substring after the last instance
     * of fragment. If omitFragment is true, frag is not returned.}.
     * 
     * @return
     *     {@code Returns an array of strings [ a, fragment, z ] where a is the
     *     substring within s before the last occurrence of fragment, and z is
     *     the substring after the last instance of fragment. If omitFragment is
     *     true, frag is not returned.}
     */
    public static Localizable _str_rpartition() {
        return new Localizable(holder, "str_rpartition");
    }

    /**
     * Key {@code math_multinomial}: {@code Calculates the multinomial of one
     * number or a series of numbers.}.
     * 
     * @return
     *     {@code Calculates the multinomial of one number or a series of
     *     numbers.}
     */
    public static String math_multinomial() {
        return holder.format("math_multinomial");
    }

    /**
     * Key {@code math_multinomial}: {@code Calculates the multinomial of one
     * number or a series of numbers.}.
     * 
     * @return
     *     {@code Calculates the multinomial of one number or a series of
     *     numbers.}
     */
    public static Localizable _math_multinomial() {
        return new Localizable(holder, "math_multinomial");
    }

    /**
     * Key {@code math_degrees}: {@code Converts an angle from radians to
     * degrees.}.
     * 
     * @return
     *     {@code Converts an angle from radians to degrees.}
     */
    public static String math_degrees() {
        return holder.format("math_degrees");
    }

    /**
     * Key {@code math_degrees}: {@code Converts an angle from radians to
     * degrees.}.
     * 
     * @return
     *     {@code Converts an angle from radians to degrees.}
     */
    public static Localizable _math_degrees() {
        return new Localizable(holder, "math_degrees");
    }

    /**
     * Key {@code math_fact}: {@code Returns the factorial of a number,
     * starting from 1.}.
     * 
     * @return
     *     {@code Returns the factorial of a number, starting from 1.}
     */
    public static String math_fact() {
        return holder.format("math_fact");
    }

    /**
     * Key {@code math_fact}: {@code Returns the factorial of a number,
     * starting from 1.}.
     * 
     * @return
     *     {@code Returns the factorial of a number, starting from 1.}
     */
    public static Localizable _math_fact() {
        return new Localizable(holder, "math_fact");
    }

    /**
     * Key {@code math_lcm}: {@code Returns the greatest common denominator
     * of two numbers.}.
     * 
     * @return
     *     {@code Returns the greatest common denominator of two numbers.}
     */
    public static String math_lcm() {
        return holder.format("math_lcm");
    }

    /**
     * Key {@code math_lcm}: {@code Returns the greatest common denominator
     * of two numbers.}.
     * 
     * @return
     *     {@code Returns the greatest common denominator of two numbers.}
     */
    public static Localizable _math_lcm() {
        return new Localizable(holder, "math_lcm");
    }

    /**
     * Key {@code fun_type}: {@code Returns a string with the data type of o,
     * such as undefined, string, number, boolean, etc.}.
     * 
     * @return
     *     {@code Returns a string with the data type of o, such as undefined,
     *     string, number, boolean, etc.}
     */
    public static String fun_type() {
        return holder.format("fun_type");
    }

    /**
     * Key {@code fun_type}: {@code Returns a string with the data type of o,
     * such as undefined, string, number, boolean, etc.}.
     * 
     * @return
     *     {@code Returns a string with the data type of o, such as undefined,
     *     string, number, boolean, etc.}
     */
    public static Localizable _fun_type() {
        return new Localizable(holder, "fun_type");
    }

    /**
     * Key {@code arr_in_array}: {@code Returns true if the array contains
     * the desired string, and false otherwise. Will not convert data
     * types.}.
     * 
     * @return
     *     {@code Returns true if the array contains the desired string, and
     *     false otherwise. Will not convert data types.}
     */
    public static String arr_in_array() {
        return holder.format("arr_in_array");
    }

    /**
     * Key {@code arr_in_array}: {@code Returns true if the array contains
     * the desired string, and false otherwise. Will not convert data
     * types.}.
     * 
     * @return
     *     {@code Returns true if the array contains the desired string, and
     *     false otherwise. Will not convert data types.}
     */
    public static Localizable _arr_in_array() {
        return new Localizable(holder, "arr_in_array");
    }

    /**
     * Key {@code str_replace}: {@code Returns the string obtained by
     * replacing the find string with the replace string in the inputted
     * string. For example, ''The cow jumps over the moon and
     * moos''.replace(''oo'', ''ee'') returns the string ''The cow jumps over
     * the meen and mees''. Find can be a regex pattern. For example, ''The
     * cow jumps over the moon and moos''.replace(/\s+/, "_") will return
     * ''The_cow_jumps_over_the_moon_and_moos''}.
     * 
     * @return
     *     {@code Returns the string obtained by replacing the find string with
     *     the replace string in the inputted string. For example, ''The cow
     *     jumps over the moon and moos''.replace(''oo'', ''ee'') returns the
     *     string ''The cow jumps over the meen and mees''. Find can be a regex
     *     pattern. For example, ''The cow jumps over the moon and
     *     moos''.replace(/\s+/, "_") will return
     *     ''The_cow_jumps_over_the_moon_and_moos''}
     */
    public static String str_replace() {
        return holder.format("str_replace");
    }

    /**
     * Key {@code str_replace}: {@code Returns the string obtained by
     * replacing the find string with the replace string in the inputted
     * string. For example, ''The cow jumps over the moon and
     * moos''.replace(''oo'', ''ee'') returns the string ''The cow jumps over
     * the meen and mees''. Find can be a regex pattern. For example, ''The
     * cow jumps over the moon and moos''.replace(/\s+/, "_") will return
     * ''The_cow_jumps_over_the_moon_and_moos''}.
     * 
     * @return
     *     {@code Returns the string obtained by replacing the find string with
     *     the replace string in the inputted string. For example, ''The cow
     *     jumps over the moon and moos''.replace(''oo'', ''ee'') returns the
     *     string ''The cow jumps over the meen and mees''. Find can be a regex
     *     pattern. For example, ''The cow jumps over the moon and
     *     moos''.replace(/\s+/, "_") will return
     *     ''The_cow_jumps_over_the_moon_and_moos''}
     */
    public static Localizable _str_replace() {
        return new Localizable(holder, "str_replace");
    }

    /**
     * Key {@code xml_selectxml}: {@code Returns an array of all the desired
     * elements from an HTML or XML document, if the element exists. Elements
     * are identified using the Jsoup selector syntax:
     * https://jsoup.org/apidocs/org/jsoup/select/Selector.html.}.
     * 
     * @return
     *     {@code Returns an array of all the desired elements from an HTML or
     *     XML document, if the element exists. Elements are identified using the
     *     Jsoup selector syntax:
     *     https://jsoup.org/apidocs/org/jsoup/select/Selector.html.}
     */
    public static String xml_selectxml() {
        return holder.format("xml_selectxml");
    }

    /**
     * Key {@code xml_selectxml}: {@code Returns an array of all the desired
     * elements from an HTML or XML document, if the element exists. Elements
     * are identified using the Jsoup selector syntax:
     * https://jsoup.org/apidocs/org/jsoup/select/Selector.html.}.
     * 
     * @return
     *     {@code Returns an array of all the desired elements from an HTML or
     *     XML document, if the element exists. Elements are identified using the
     *     Jsoup selector syntax:
     *     https://jsoup.org/apidocs/org/jsoup/select/Selector.html.}
     */
    public static Localizable _xml_selectxml() {
        return new Localizable(holder, "xml_selectxml");
    }

    /**
     * Key {@code str_ngram_fingerprint}: {@code Returns the n-gram
     * fingerprint of s.}.
     * 
     * @return
     *     {@code Returns the n-gram fingerprint of s.}
     */
    public static String str_ngram_fingerprint() {
        return holder.format("str_ngram_fingerprint");
    }

    /**
     * Key {@code str_ngram_fingerprint}: {@code Returns the n-gram
     * fingerprint of s.}.
     * 
     * @return
     *     {@code Returns the n-gram fingerprint of s.}
     */
    public static Localizable _str_ngram_fingerprint() {
        return new Localizable(holder, "str_ngram_fingerprint");
    }

    /**
     * Key {@code math_acos}: {@code Returns the arc cosine of an angle, in
     * the range 0 through PI.}.
     * 
     * @return
     *     {@code Returns the arc cosine of an angle, in the range 0 through PI.}
     */
    public static String math_acos() {
        return holder.format("math_acos");
    }

    /**
     * Key {@code math_acos}: {@code Returns the arc cosine of an angle, in
     * the range 0 through PI.}.
     * 
     * @return
     *     {@code Returns the arc cosine of an angle, in the range 0 through PI.}
     */
    public static Localizable _math_acos() {
        return new Localizable(holder, "math_acos");
    }

    /**
     * Key {@code math_atan2}: {@code Converts rectangular coordinates (n1,
     * n2) to polar (r, theta). Returns number theta.}.
     * 
     * @return
     *     {@code Converts rectangular coordinates (n1, n2) to polar (r, theta).
     *     Returns number theta.}
     */
    public static String math_atan2() {
        return holder.format("math_atan2");
    }

    /**
     * Key {@code math_atan2}: {@code Converts rectangular coordinates (n1,
     * n2) to polar (r, theta). Returns number theta.}.
     * 
     * @return
     *     {@code Converts rectangular coordinates (n1, n2) to polar (r, theta).
     *     Returns number theta.}
     */
    public static Localizable _math_atan2() {
        return new Localizable(holder, "math_atan2");
    }

    /**
     * Key {@code arr_reverse}: {@code Reverses array a.}.
     * 
     * @return
     *     {@code Reverses array a.}
     */
    public static String arr_reverse() {
        return holder.format("arr_reverse");
    }

    /**
     * Key {@code arr_reverse}: {@code Reverses array a.}.
     * 
     * @return
     *     {@code Reverses array a.}
     */
    public static Localizable _arr_reverse() {
        return new Localizable(holder, "arr_reverse");
    }

    /**
     * Key {@code fun_to_date}: {@code Returns the inputted object converted
     * to a date object. Without arguments, it returns the ISO 8601 extended
     * format. With arguments, you can control the output format. With
     * monthFirst: set false if the date is formatted with the day before the
     * month. With formatN: attempt to parse the date using an ordered list
     * of possible formats. Supply formats based on the SimpleDateFormat
     * syntax: <a
     * href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.}.
     * 
     * @return
     *     {@code Returns the inputted object converted to a date object. Without
     *     arguments, it returns the ISO 8601 extended format. With arguments,
     *     you can control the output format. With monthFirst: set false if the
     *     date is formatted with the day before the month. With formatN: attempt
     *     to parse the date using an ordered list of possible formats. Supply
     *     formats based on the SimpleDateFormat syntax: <a
     *     href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.}
     */
    public static String fun_to_date() {
        return holder.format("fun_to_date");
    }

    /**
     * Key {@code fun_to_date}: {@code Returns the inputted object converted
     * to a date object. Without arguments, it returns the ISO 8601 extended
     * format. With arguments, you can control the output format. With
     * monthFirst: set false if the date is formatted with the day before the
     * month. With formatN: attempt to parse the date using an ordered list
     * of possible formats. Supply formats based on the SimpleDateFormat
     * syntax: <a
     * href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.}.
     * 
     * @return
     *     {@code Returns the inputted object converted to a date object. Without
     *     arguments, it returns the ISO 8601 extended format. With arguments,
     *     you can control the output format. With monthFirst: set false if the
     *     date is formatted with the day before the month. With formatN: attempt
     *     to parse the date using an ordered list of possible formats. Supply
     *     formats based on the SimpleDateFormat syntax: <a
     *     href="http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.}
     */
    public static Localizable _fun_to_date() {
        return new Localizable(holder, "fun_to_date");
    }

}
