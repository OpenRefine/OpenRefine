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
package org.openrefine.wikidata.qa.scrutinizers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.MockConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.testng.annotations.BeforeMethod;

public abstract class ScrutinizerTest {

    public abstract EditScrutinizer getScrutinizer();

    private EditScrutinizer scrutinizer;
    private QAWarningStore store;
    private ConstraintFetcher fetcher;

    @BeforeMethod
    public void setUp() {
        store = new QAWarningStore();
        fetcher = new MockConstraintFetcher();
        scrutinizer = getScrutinizer();
        scrutinizer.setStore(store);
        scrutinizer.setFetcher(fetcher);
    }

    public void scrutinize(ItemUpdate... updates) {
        scrutinizer.batchIsBeginning();
        for(ItemUpdate update : Arrays.asList(updates)) {
            if(!update.isNull()) {
                scrutinizer.scrutinize(update);
            }
        }
        scrutinizer.batchIsFinished();
    }

    public void assertWarningsRaised(String... types) {
        assertEquals(Arrays.asList(types).stream().collect(Collectors.toSet()), getWarningTypes());
    }

    public void assertWarningRaised(QAWarning warning) {
        assertTrue(store.getWarnings().contains(warning));
    }

    public void assertNoWarningRaised() {
        assertWarningsRaised();
    }

    public Set<String> getWarningTypes() {
        return store.getWarnings().stream().map(w -> w.getType()).collect(Collectors.toSet());
    }
}
