package org.openrefine.wikidata.qa;

import static org.junit.Assert.assertEquals;

import org.openrefine.wikidata.testing.JacksonSerializationTest;
import org.testng.annotations.Test;

public class QAWarningTest {
    
    public static QAWarning exampleWarning = new QAWarning("add-statements-with-invalid-format",
            "P2427",
            QAWarning.Severity.IMPORTANT,
            1);
    public static String exampleJson =
            "{\"severity\":\"IMPORTANT\","+
            "\"count\":1,\"bucketId\":\"P2427\",\"type\":\"add-statements-with-invalid-format\"}";
    
    @Test
    public void testSerialize() {
        JacksonSerializationTest.testSerialize(exampleWarning, exampleJson);
    }
    
    @Test
    public void testAggregate() {
        QAWarning firstWarning = new QAWarning("add-statements-with-invalid-format",
                "P2427",
                QAWarning.Severity.INFO,
                1);
        firstWarning.setProperty("foo", "bar");
        assertEquals(exampleWarning.getAggregationId(), firstWarning.getAggregationId());
        QAWarning merged = firstWarning.aggregate(exampleWarning);
        assertEquals(2, merged.getCount());
        assertEquals(exampleWarning.getAggregationId(), merged.getAggregationId());
        assertEquals(exampleWarning.getType(), merged.getType());
        assertEquals(exampleWarning.getSeverity(), merged.getSeverity());
        assertEquals("bar", merged.getProperties().get("foo"));
    }
    
    @Test
    public void testCompare() {
        QAWarning otherWarning = new QAWarning("no-reference",
                "no-reference",
                QAWarning.Severity.WARNING,
                1);
        assertEquals(1, otherWarning.compareTo(exampleWarning));
        assertEquals(-1, exampleWarning.compareTo(otherWarning));
        assertEquals(0, exampleWarning.compareTo(exampleWarning));
    }

}
