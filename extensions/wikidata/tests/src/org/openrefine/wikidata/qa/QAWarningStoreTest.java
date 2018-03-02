package org.openrefine.wikidata.qa;

import static org.junit.Assert.assertEquals;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QAWarningStoreTest {

    public static String exampleJson = "{\"max_severity\":\"CRITICAL\",\"nb_warnings\":5,"
            +"\"warnings\":[{\"type\":\"new-item-without-label\",\"bucketId\":null,"
            +"\"severity\":\"CRITICAL\",\"count\":3},{\"type\":\"add-statements-with-invalid-format\","
            +"\"bucketId\":\"P2427\",\"severity\":\"IMPORTANT\",\"count\":2}]}";
    
    private QAWarningStore store;
    private QAWarning otherWarning;
    
    @BeforeMethod
    public void setUp() {
        store = new QAWarningStore();
        store.addWarning(QAWarningTest.exampleWarning);
        store.addWarning(QAWarningTest.exampleWarning);
        otherWarning = new QAWarning("new-item-without-label", null, QAWarning.Severity.CRITICAL, 3);
        store.addWarning(otherWarning);
    }
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.testSerialize(store, exampleJson);
    }
    
    @Test
    public void testCount() {
        assertEquals(5, store.getNbWarnings());
        assertEquals(2, store.getWarnings().size());
    }
    
    @Test
    public void testMaxSeverity() {
        assertEquals(QAWarning.Severity.CRITICAL, store.getMaxSeverity());
        assertEquals(QAWarning.Severity.INFO, (new QAWarningStore()).getMaxSeverity());
    }
}
