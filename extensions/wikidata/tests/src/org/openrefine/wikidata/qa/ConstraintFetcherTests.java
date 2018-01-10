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
    private PropertyIdValue instanceOf;
    private PropertyIdValue gridId;
    private PropertyIdValue hasPart;
    private PropertyIdValue partOf;
    private PropertyIdValue referenceURL;
    private PropertyIdValue reasonForDeprecation;
    
    public ConstraintFetcherTests() {
        fetcher = new ConstraintFetcher();
        headOfGovernment = Datamodel.makeWikidataPropertyIdValue("P6");
        startTime = Datamodel.makeWikidataPropertyIdValue("P580");
        endTime = Datamodel.makeWikidataPropertyIdValue("P582");
        instanceOf = Datamodel.makeWikidataPropertyIdValue("P31");
        gridId = Datamodel.makeWikidataPropertyIdValue("P2427");
        hasPart = Datamodel.makeWikidataPropertyIdValue("P527");
        partOf = Datamodel.makeWikidataPropertyIdValue("P361");
        referenceURL = Datamodel.makeWikidataPropertyIdValue("P854");
        reasonForDeprecation = Datamodel.makeWikidataPropertyIdValue("P2241");
    }
    
    @Test
    public void testGetFormatConstraint() {
        String regex = fetcher.getFormatRegex(gridId);
        Pattern pattern = Pattern.compile(regex);
        
        Assert.assertTrue(pattern.matcher("grid.470811.b").matches());
        Assert.assertFalse(pattern.matcher("501100006367").matches());
        
        Assert.assertNull(fetcher.getFormatRegex(instanceOf));
    }
    
    @Test
    public void testGetInverseConstraint() {
        Assert.assertEquals(fetcher.getInversePid(partOf), hasPart);
    }
    
    @Test
    public void testOnlyReferences() {
        Assert.assertTrue(fetcher.isForReferencesOnly(referenceURL));
        Assert.assertFalse(fetcher.isForReferencesOnly(reasonForDeprecation));
    }
    
    @Test
    public void testOnlyQualifiers() {
        Assert.assertTrue(fetcher.isForQualifiersOnly(reasonForDeprecation));
        Assert.assertFalse(fetcher.isForQualifiersOnly(headOfGovernment));
    }
    
    @Test
    public void testOnlyValues() {
        Assert.assertTrue(fetcher.isForValuesOnly(headOfGovernment));
        Assert.assertFalse(fetcher.isForValuesOnly(referenceURL));
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
