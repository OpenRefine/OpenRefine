package org.openrefine.wikidata.qa;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import java.util.regex.Pattern;

public class ConstraintFetcherTests {
    
    private ConstraintFetcher fetcher;
    
    private PropertyIdValue headOfGovernment;
    private PropertyIdValue startTime;
    private PropertyIdValue endTime;
    
    public ConstraintFetcherTests() {
        fetcher = new ConstraintFetcher();
        headOfGovernment = Datamodel.makeWikidataPropertyIdValue("P6");
        startTime = Datamodel.makeWikidataPropertyIdValue("P580");
        endTime = Datamodel.makeWikidataPropertyIdValue("P582");
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
    
    @Test
    public void testAllowedQualifiers() {
        Assert.assertTrue(fetcher.allowedQualifiers(headOfGovernment).contains(startTime));
        Assert.assertTrue(fetcher.allowedQualifiers(headOfGovernment).contains(endTime));
        Assert.assertFalse(fetcher.allowedQualifiers(headOfGovernment).contains(headOfGovernment));
        Assert.assertNull(fetcher.allowedQualifiers(startTime));
    }
    
    @Test
    public void testMandatoryQualifiers() {
        Assert.assertTrue(fetcher.mandatoryQualifiers(headOfGovernment).contains(startTime));
        Assert.assertFalse(fetcher.mandatoryQualifiers(headOfGovernment).contains(endTime));
        Assert.assertNull(fetcher.allowedQualifiers(startTime));
    }
}
