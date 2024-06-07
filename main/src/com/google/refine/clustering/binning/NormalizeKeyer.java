package com.google.refine.clustering.binning;

import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.CharMatcher;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

/**
 * A keyer class that provides functionality to normalize strings by removing diacritics,
 * normalizing extended Western characters to their ASCII representation, and handling Unicode normalization forms.
 */
public class NormalizeKeyer extends Keyer {

    // Define regex patterns for punctuation control characters and diacritics
    static final Pattern punctctrl = Pattern.compile("\\p{Punct}|[\\x00-\\x08\\x0E-\\x1F\\x7F\\x80-\\x84\\x86-\\x9F]",
            Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern
            .compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

    // Define a pattern for whitespace characters
    private static final Pattern WHITESPACE = Pattern.compile("\\s+",
            Pattern.UNICODE_CHARACTER_CLASS);

    // Initialize the Python interpreter and import the unidecode library
    private static PythonInterpreter interpreter;
    static {
        interpreter = new PythonInterpreter();
        interpreter.exec("import sys");
        //The location of the jython
        interpreter.exec("sys.path.append(r'D:\\jython2.7.3\\Lib\\site-packages')");
        interpreter.exec("from unidecode import unidecode");
    }

    /**
     * Key method that normalizes the input string by removing diacritics, normalizing extended Western characters,
     * and applying Unicode normalization form "NFD".
     * @param s The input string to be normalized.
     * @param o Additional parameters (not used in this implementation).
     * @return The normalized string.
     */
    @Override
    public String key(String s, Object... o) {
        if (s == null || o != null && o.length > 0) {
            throw new IllegalArgumentException("NormalizeKeyer accepts a single string parameter");
        }
        return WHITESPACE.splitAsStream(normalize(s, true, "NFD")).sorted().distinct().collect(Collectors.joining(" "));
    }

    /**
     * Key method that normalizes the input string using a specific Unicode normalization form.
     * @param s The input string to be normalized.
     * @param form The Unicode normalization form to be applied (e.g., NFC, NFKC, NFD, NFKD).
     * @return The normalized string.
     */
    public String keyWithForm(String s, String form) {
        // Bypass sorting when normalization form is explicitly provided
        return normalize(s, true, form);
    }

    /**
     * Helper method to normalize the input string by removing diacritics and applying Unicode normalization.
     * @param s The input string to be normalized.
     * @return The normalized string.
     */
    protected String normalize(String s) {
        s = normalize(s, false, "NFD");
        return s;
    }

    /**
     * Helper method to normalize the input string with options for whitespace trimming, case folding,
     * diacritics removal, and applying Unicode normalization.
     * @param s The input string to be normalized.
     * @param strong Indicates whether to perform strong normalization (whitespace trimming and case folding).
     * @param form The Unicode normalization form to be applied (e.g., NFC, NFKC, NFD, NFKD).
     * @return The normalized string.
     */
    protected String normalize(String s, boolean strong, String form) {
        if (strong) {
            s = CharMatcher.whitespace().trimFrom(s);
            s = s.toLowerCase();
        }
        s = stripDiacritics(s, form);
        s = stripNonDiacritics(s);
        if (strong) {
            s = punctctrl.matcher(s).replaceAll("");
        }
        return s;
    }

    /**
     * Deprecated method to normalize the input string by removing diacritics.
     * @param s The input string to be normalized.
     * @return The normalized string.
     * @deprecated Use {@link #normalize(String)} or {@link #normalize(String, boolean, String)} instead.
     */
    @Deprecated
    protected String asciify(String s) {
        return normalize(s);
    }

    /**
     * Helper method to strip diacritics from the input string using a specific Unicode normalization form.
     * @param str The input string to be processed.
     * @param form The Unicode normalization form to be applied (e.g., NFC, NFKC, NFD, NFKD).
     * @return The string with diacritics removed.
     */
    protected static String stripDiacritics(String str, String form) {
        Normalizer.Form normalizationForm = getNormalizationForm(form);
        str = Normalizer.normalize(str, normalizationForm);
        str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
        return str;
    }

    /**
     * Helper method to strip non-diacritic characters from the input string using the unidecode library.
     * @param orig The input string to be processed.
     * @return The string with non-diacritic characters stripped.
     */
    private static String stripNonDiacritics(String orig) {
        orig = orig.replace("\\", "\\\\").replace("'", "\\'");
        PyObject result = interpreter.eval(String.format("unidecode(u'%s')", orig));
        return result.toString();
    }

    /**
     * Helper method to get the Normalizer.Form enum value based on the specified Unicode normalization form string.
     * @param form The Unicode normalization form string (e.g., NFC, NFKC, NFD, NFKD).
     * @return The corresponding Normalizer.Form enum value.
     */
    private static Normalizer.Form getNormalizationForm(String form) {
        switch (form) {
            case "NFC":
                return Normalizer.Form.NFC;
            case "NFKC":
                return Normalizer.Form.NFKC;
            case "NFKD":
                return Normalizer.Form.NFKD;
            case "NFD":
            default:
                return Normalizer.Form.NFD;
        }
    }
}
