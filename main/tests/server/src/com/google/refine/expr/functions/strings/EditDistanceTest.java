package com.google.refine.expr.functions.strings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Properties;

import org.testng.annotations.Test;

public class EditDistanceTest {

    // Test case to check edit distance between valid strings
    @Test
    public void testWithValidStrings1() {
        EditDistance editDistance = new EditDistance();
        String s1 = "New York";
        String s2 = "newyork";
        Integer expected = 3; // Expected edit distance
        assertEquals(expected, editDistance.call(new Properties(), new Object[]{s1, s2}));
    }

    @Test
    public void testWithValidStrings2() {
        EditDistance editDistance = new EditDistance();
        String s1 = "M. Makeba";
        String s2 = "Miriam Makeba";
        Integer expected = 5; // Expected edit distance
        assertEquals(expected, editDistance.call(new Properties(), new Object[]{s1, s2}));
    }

    // Test case to check edit distance between empty strings
    @Test
    public void testWithEmptyStrings() {
        EditDistance editDistance = new EditDistance();
        String s1 = "";
        String s2 = "";
        Integer expected = 0; // Expected edit distance for empty strings
        assertEquals(expected, editDistance.call(new Properties(), new Object[]{s1, s2}));
    }

    // Test case to check edit distance between single character strings
    @Test
    public void testWithSingleCharacterStrings() {
        EditDistance editDistance = new EditDistance();
        String s1 = "a";
        String s2 = "b";
        Integer expected = 1; // Expected edit distance for single character strings
        assertEquals(expected, editDistance.call(new Properties(), new Object[]{s1, s2}));
    }

    // Test case to check edit distance between strings of different lengths
    @Test
    public void testWithDifferentLengthStrings() {
        EditDistance editDistance = new EditDistance();
        String s1 = "abcdef";
        String s2 = "abc";
        Integer expected = 3; // Expected edit distance for strings of different lengths
        assertEquals(expected, editDistance.call(new Properties(), new Object[]{s1, s2}));
    }

    // Test case to check handling of null arguments
    //The call method does not explicitly check for null so it fails
    @Test
    public void testWithNullArguments() {
        EditDistance editDistance = new EditDistance();
        assertThrows(IllegalArgumentException.class, () -> {
            editDistance.call(new Properties(), null);
        });
    }

    // Test case to check handling of invalid argument count
    @Test
    public void testWithInvalidArgsCount() {
        EditDistance editDistance = new EditDistance();
        String s1 = "test";

        assertThrows(IllegalArgumentException.class, () -> {
            editDistance.call(new Properties(), new Object[]{s1});
        });
    }

    // Test case to check handling of invalid argument types
    @Test
    public void testWithInvalidArgsTypes() {
        EditDistance editDistance = new EditDistance();
        String s1 = "test";
        Object s2 = new Object(); // Non-string object to test type checking

        assertThrows(IllegalArgumentException.class, () -> {
            editDistance.call(new Properties(), new Object[]{s1, s2});
        });
    }
}
