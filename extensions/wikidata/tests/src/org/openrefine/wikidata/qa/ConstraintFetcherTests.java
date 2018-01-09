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
    
    @Test
    public void testOnlyReferences() {
        Assert.assertTrue(fetcher.isForReferencesOnly("P854"));
        Assert.assertFalse(fetcher.isForReferencesOnly("P2241"));
    }
    
    @Test
    public void testOnlyQualifiers() {
        Assert.assertTrue(fetcher.isForQualifiersOnly("P2241"));
        Assert.assertFalse(fetcher.isForQualifiersOnly("P6"));
    }
    
    @Test
    public void testOnlyValues() {
        Assert.assertTrue(fetcher.isForValuesOnly("P6"));
        Assert.assertFalse(fetcher.isForValuesOnly("P854"));
    }
}
