package com.google.refine.clustering.binning;

import static org.junit.Assert.assertEquals;

import java.text.Collator;
import java.util.Locale;

import org.junit.Test;

public class LocaleSensitiveComparatorTests {
    private final String str1 = "Björn";
    private final String str2 = "Borg";
    private final Locale locale = new Locale("sv", "SE");

    @Test
    public void testCompareUsingPrimaryStrength() {
        int expected = 0;
        int result = LocaleSensitiveComparator.compareStrings(str1, str2, locale, Collator.PRIMARY);
        assertEquals("Primary strength comparison should ignore diacritics.", expected, result);
    }

    @Test
    public void testCompareUsingSecondaryStrength() {
        int expected = 1;
        int result = LocaleSensitiveComparator.compareStrings(str1, str2, locale, Collator.SECONDARY);
        assertEquals("Secondary strength comparison should consider diacritics.", expected, result);
    }

    @Test
    public void testCompareUsingTertiaryStrength() {
        int expected = 1;
        int result = LocaleSensitiveComparator.compareStrings(str1, str2, locale, Collator.TERTIARY);
        assertEquals("Tertiary strength comparison should consider both diacritics and case.", expected, result);
    }
}