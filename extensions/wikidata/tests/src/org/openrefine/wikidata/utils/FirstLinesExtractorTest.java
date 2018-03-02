package org.openrefine.wikidata.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.testng.annotations.Test;

public class FirstLinesExtractorTest {
    
    @Test
    public void testShort() throws IOException {
        assertEquals("a\nb\nc\n", FirstLinesExtractor.extractFirstLines("a\nb\nc\n", 5));
    }
    
    @Test
    public void testLong() throws IOException {
        assertEquals("a\nb\n...", FirstLinesExtractor.extractFirstLines("a\nb\nc", 3));
    }
}
