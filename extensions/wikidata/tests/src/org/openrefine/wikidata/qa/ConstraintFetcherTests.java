package org.openrefine.wikidata.qa;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.regex.Pattern;

public class ConstraintFetcherTests {
    
    private ConstraintFetcher fetcher;
    
    public ConstraintFetcherTests() {
        fetcher = new ConstraintFetcher();
    }
    
    @Test
    public void testGetFormatConstraint() {
        String regex = fetcher.getFormatRegex("P2427");
        Pattern pattern = Pattern.compile(regex);
        
        Assert.assertTrue(pattern.matcher("grid.470811.b").matches());
        Assert.assertFalse(pattern.matcher("501100006367").matches());
        
        Assert.assertNull(fetcher.getFormatRegex("P31"));
    }
    
    @Test
    public void testGetInverseConstraint() {
        Assert.assertEquals(fetcher.getInversePid("P361"), "P527");
    }
}
