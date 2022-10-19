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

import org.openrefine.wikibase.testing.JacksonSerializationTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QAWarningStoreTest {

    public static String exampleJson = "{\"max_severity\":\"CRITICAL\",\"nb_warnings\":5,"
            + "\"warnings\":[{\"type\":\"new-item-without-label\",\"bucketId\":null,\"aggregationId\":\"new-item-without-label\","
            + "\"severity\":\"CRITICAL\",\"count\":3,\"facetable\":true},{\"type\":\"add-statements-with-invalid-format\","
            + "\"bucketId\":\"P2427\",\"aggregationId\":\"add-statements-with-invalid-format_P2427\",\"severity\":\"IMPORTANT\",\"count\":2,\"facetable\":true}]}";

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
