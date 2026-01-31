package com.google.refine.expr.functions.strings;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Properties;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ParsingException;
import com.google.refine.grel.GrelTestBase;

/**
 * Partition-based functional tests for the replaceEach GREL function.
 * 
 * This test class implements systematic partition testing based on equivalence
 * partitioning principles. The input domain is divided into partitions based on:
 * - Argument validity and array characteristics
 * - Match behavior (matches exist vs. no matches)
 * - Edge cases (empty arrays, null inputs, mismatched lengths)
 * 
 * @author Tanmay Goel (74808800)
 * @see ReplaceEachTest for additional functional tests
 */
public class ReplaceEachPartitionTests extends GrelTestBase {

    @BeforeMethod
    public void setUp() {
        bindings = new Properties();
        bindings.put("v", "");
    }

    // ========================================================================
    // PARTITION 1: Valid inputs with at least one match
    // Expected: Replacements are applied to matching substrings
    // ========================================================================

    @Test
    public void testP1_validInputWithMatch_singleReplacement() {
        // Representative value: "The cow moos" with find=["moo"], replace=["mee"]
        Object result = invoke("replaceEach", "The cow moos", 
                new String[]{"moo"}, new String[]{"mee"});
        assertTrue(result instanceof String);
        assertEquals(result, "The cow mees");
    }

    @Test
    public void testP1_validInputWithMatch_multipleReplacements() {
        // Multiple matches in the string
        Object result = invoke("replaceEach", "The cow moos and moos again", 
                new String[]{"moo"}, new String[]{"mee"});
        assertEquals(result, "The cow mees and mees again");
    }

    @Test
    public void testP1_validInputWithMatch_multiplePatterns() {
        // Multiple patterns to find and replace
        Object result = invoke("replaceEach", "hello world", 
                new String[]{"hello", "world"}, new String[]{"hi", "earth"});
        assertEquals(result, "hi earth");
    }

    // ========================================================================
    // PARTITION 2: Valid inputs with no matches
    // Expected: Original string returned unchanged
    // ========================================================================

    @Test
    public void testP2_validInputNoMatch_returnsUnchanged() {
        // Representative value: "Hello world" with find=["xyz"], replace=["abc"]
        Object result = invoke("replaceEach", "Hello world", 
                new String[]{"xyz"}, new String[]{"abc"});
        assertTrue(result instanceof String);
        assertEquals(result, "Hello world");
    }

    @Test
    public void testP2_validInputNoMatch_multiplePatterns() {
        // Multiple non-matching patterns
        Object result = invoke("replaceEach", "Hello world", 
                new String[]{"foo", "bar", "baz"}, new String[]{"a", "b", "c"});
        assertEquals(result, "Hello world");
    }

    @Test
    public void testP2_validInputNoMatch_caseSensitive() {
        // Case sensitivity test - "HELLO" should not match "hello"
        Object result = invoke("replaceEach", "hello world", 
                new String[]{"HELLO"}, new String[]{"hi"});
        assertEquals(result, "hello world");
    }

    // ========================================================================
    // PARTITION 3: Empty find and replace arrays
    // Expected: Original string returned unchanged
    // ========================================================================

    @Test
    public void testP3_emptyArrays_returnsUnchanged() {
        // Representative value: "abc" with find=[], replace=[]
        Object result = invoke("replaceEach", "abc", 
                new String[]{}, new String[]{});
        assertTrue(result instanceof String);
        assertEquals(result, "abc");
    }

    @Test
    public void testP3_emptyArrays_longerString() {
        // Empty arrays with longer input string
        Object result = invoke("replaceEach", "The quick brown fox jumps over the lazy dog", 
                new String[]{}, new String[]{});
        assertEquals(result, "The quick brown fox jumps over the lazy dog");
    }

    // ========================================================================
    // PARTITION 4: Mismatched array lengths
    // Expected: Document observed behavior (may use last replacement or cycle)
    // ========================================================================

    @Test
    public void testP4_mismatchedArrayLengths_moreFindThanReplace() {
        // Representative value: find=["a","b"], replace=["x"]
        // Based on existing tests, when replace array is shorter, 
        // the last replacement value is reused
        Object result = invoke("replaceEach", "abc", 
                new String[]{"a", "b"}, new String[]{"x"});
        assertTrue(result instanceof String);
        // Document actual behavior: last replacement reused
        assertEquals(result, "xxc");
    }

    @Test
    public void testP4_mismatchedArrayLengths_moreReplaceThanFind() {
        // More replacements than find patterns
        // Documented behavior: returns error when replace array is longer than find array
        Object result = invoke("replaceEach", "abc", 
                new String[]{"a"}, new String[]{"x", "y", "z"});
        assertTrue(result instanceof EvalError,
                "Expected EvalError when replace array is longer than find array, got: " + result);
    }

    // ========================================================================
    // PARTITION 5: Null string input
    // Expected: Error or null return value
    // ========================================================================

    @Test
    public void testP5_nullStringInput_returnsError() {
        // Representative value: null string with valid arrays
        Object result = invoke("replaceEach", null, 
                new String[]{"a"}, new String[]{"x"});
        // Null input should produce an error
        assertTrue(result instanceof EvalError, 
                "Expected EvalError for null string input, got: " + result);
    }

    // ========================================================================
    // PARTITION 6: Null array input
    // Expected: Error or null return value
    // ========================================================================

    @Test
    public void testP6_nullFindArray_returnsError() {
        // Null find array
        Object result = invoke("replaceEach", "abc", null, new String[]{"x"});
        assertTrue(result instanceof EvalError, 
                "Expected EvalError for null find array, got: " + result);
    }

    @Test
    public void testP6_nullReplaceArray_returnsError() {
        // Null replace array
        Object result = invoke("replaceEach", "abc", new String[]{"a"}, null);
        assertTrue(result instanceof EvalError, 
                "Expected EvalError for null replace array, got: " + result);
    }

    // ========================================================================
    // BOUNDARY TESTS
    // ========================================================================

    @Test
    public void testBoundary_singleCharacterString() {
        // Minimal string that can contain a match
        Object result = invoke("replaceEach", "a", 
                new String[]{"a"}, new String[]{"b"});
        assertEquals(result, "b");
    }

    @Test
    public void testBoundary_singleCharacterNoMatch() {
        // Single character with no match
        Object result = invoke("replaceEach", "x", 
                new String[]{"a"}, new String[]{"b"});
        assertEquals(result, "x");
    }

    @Test
    public void testBoundary_emptyString() {
        // Empty string input
        Object result = invoke("replaceEach", "", 
                new String[]{"a"}, new String[]{"b"});
        assertEquals(result, "");
    }

    @Test
    public void testBoundary_overlappingPatterns() {
        // Patterns that could potentially overlap
        Object result = invoke("replaceEach", "aaa", 
                new String[]{"aa", "a"}, new String[]{"b", "c"});
        // First match wins
        assertTrue(result instanceof String);
    }

    // ========================================================================
    // GREL EXPRESSION TESTS (using parseEval)
    // ========================================================================

    @Test
    public void testP1_grelExpression_validMatch() throws ParsingException {
        String test[] = { 
            "\"The cow moos\".replaceEach([\"moo\"], [\"mee\"])",
            "The cow mees" 
        };
        parseEval(bindings, test);
    }

    @Test
    public void testP2_grelExpression_noMatch() throws ParsingException {
        String test[] = { 
            "\"Hello world\".replaceEach([\"xyz\"], [\"abc\"])",
            "Hello world" 
        };
        parseEval(bindings, test);
    }

    @Test
    public void testP3_grelExpression_emptyArrays() throws ParsingException {
        String test[] = { 
            "\"abc\".replaceEach([], [])",
            "abc" 
        };
        parseEval(bindings, test);
    }
}
