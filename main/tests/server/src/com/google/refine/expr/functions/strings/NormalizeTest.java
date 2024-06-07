package com.google.refine.expr.functions.strings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.Test;

public class NormalizeTest {

    @Test
    public void testBasicNormalizationDefaultForm() {
        Normalize normalizeFunction = new Normalize();
        // Example with default form (NFD)
        String input = "gödel";
        String expected = "godel";
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertEquals(expected, result);
    }

    @Test
    public void testNormalizationNFC() {
        Normalize normalizeFunction = new Normalize();
        // Example with NFC form
        String input = "é";
        String expected = "e";
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input, "NFC"});
        assertEquals(expected, result);
    }

    @Test
    public void testNormalizationNFKC() {
        Normalize normalizeFunction = new Normalize();
        // Example with NFKC form
        String input = "João Batista";
        String expected = "joao batista"; // 'ão' should become 'ao', and everything should be lowercased
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input, "NFKC"});
        assertEquals(expected, result);
    }

    @Test
    public void testNormalizationNFKD() {
        Normalize normalizeFunction = new Normalize();
        // Example with NFKD form
        String input = "Ægir";
        String expected = "aegir";
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input, "NFKD"});
        assertEquals(expected, result);
    }

    @Test
    public void testWithExtendedWesternCharacters() {
        Normalize normalizeFunction = new Normalize();
        // Example with extended western characters
        String input = "Straße";
        String expected = "strasse"; // 'ß' should become 'ss'
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertEquals(expected, result);
    }

    @Test
    public void testWithCombiningDiacriticalMarks() {
        Normalize normalizeFunction = new Normalize();
        // Example with combining diacritical marks
        String input = "étudiant";
        String expected = "etudiant"; // 'é' should become 'e'
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertEquals(expected, result);
    }

    @Test
    public void testWithEmptyString() {
        Normalize normalizeFunction = new Normalize();
        // Example with empty string
        String input = "";
        String expected = "";
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertEquals(expected, result);
    }

    @Test
    public void testWithNullInput() {
        Normalize normalizeFunction = new Normalize();
        // Example with null input
        String input = null;
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertNull(result); // Should return null
    }

    @Test
    public void testWithNonStringInput() {
        Normalize normalizeFunction = new Normalize();
        // Example with non-string input
        Object input = 12345;
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        String expected = "12345"; // Should remain unchanged as no normalization is needed
        assertEquals(expected, result);
    }

    @Test
    public void testWithSymbolsAndPunctuation() {
        Normalize normalizeFunction = new Normalize();
        // Example with symbols and punctuation
        String input = "Dvořák!!";
        String expected = "dvorak"; // Should remove punctuation and normalize characters
        String result = (String) normalizeFunction.call(new Properties(), new Object[]{input});
        assertEquals(expected, result);
    }
}