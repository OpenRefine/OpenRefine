package org.openrefine.wikidata.qa.scrutinizers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        scrutinizer.scrutinize(Arrays.asList(updates));
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
