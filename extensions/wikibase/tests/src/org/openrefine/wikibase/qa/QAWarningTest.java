/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.qa;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.Test;

public class QAWarningTest {

    public static QAWarning exampleWarning = new QAWarning("add-statements-with-invalid-format", "P2427",
            QAWarning.Severity.IMPORTANT, 1);
    public static String exampleJson = "{\"severity\":\"IMPORTANT\","
            + "\"count\":1,\"bucketId\":\"P2427\",\"type\":\"add-statements-with-invalid-format\","
            + "\"aggregationId\":\"add-statements-with-invalid-format_P2427\",\"facetable\":true}";

    @Test
    public void testSerialize() {
        JacksonSerializationTest.testSerialize(exampleWarning, exampleJson);
    }

    @Test
    public void testAggregate() {
        QAWarning firstWarning = new QAWarning("add-statements-with-invalid-format", "P2427", QAWarning.Severity.INFO,
                1);
        firstWarning.setProperty("foo", "bar");
        firstWarning.setFacetable(false);
        assertEquals(exampleWarning.getAggregationId(), firstWarning.getAggregationId());
        QAWarning merged = firstWarning.aggregate(exampleWarning);
        assertEquals(2, merged.getCount());
        assertEquals(exampleWarning.getAggregationId(), merged.getAggregationId());
        assertEquals(exampleWarning.getType(), merged.getType());
        assertEquals(exampleWarning.getSeverity(), merged.getSeverity());
        assertEquals("bar", merged.getProperties().get("foo"));
        assertFalse(merged.isFacetable());
    }

    @Test
    public void testCompare() {
        QAWarning otherWarning = new QAWarning("no-reference", "no-reference", QAWarning.Severity.WARNING, 1);
        assertEquals(1, otherWarning.compareTo(exampleWarning));
        assertEquals(-1, exampleWarning.compareTo(otherWarning));
        assertEquals(0, exampleWarning.compareTo(exampleWarning));
    }

}
