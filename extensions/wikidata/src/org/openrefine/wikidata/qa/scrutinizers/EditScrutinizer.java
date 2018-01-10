package org.openrefine.wikidata.qa.scrutinizers;

import java.util.List;
import java.util.Properties;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.schema.ItemUpdate;

/**
 * Interface for any class that 
 * @author antonin
 *
 */
public abstract class EditScrutinizer {
    
    protected QAWarningStore _store;
    protected ConstraintFetcher _fetcher;
    
    public EditScrutinizer() {
        _fetcher = new ConstraintFetcher();
    }
    
    public void setStore(QAWarningStore store) {
        _store = store;
    }
    
    /**
     * Reads the candidate edits and emits warnings in the store
     * @param edit: the list of ItemUpdates to scrutinize
     */
    public abstract void scrutinize(List<ItemUpdate> edit);
    
    /**
     * Helper to be used by subclasses to emit INFO warnings
     * @param warning
     */
    protected void info(String type) {
        _store.addWarning(new QAWarning(type, null, QAWarning.Severity.INFO, 1, new Properties()));
    }
    
    /**
     * Helper to be used by subclasses to emit warnings
     * @param warning
     */
    protected void warning(String type) {
        _store.addWarning(new QAWarning(type, null, QAWarning.Severity.WARNING, 1, new Properties()));
    }
    
    /**
     * Helper to be used by subclasses to emit important warnings
     * @param warning
     */
    protected void important(String type) {
        _store.addWarning(new QAWarning(type, null, QAWarning.Severity.IMPORTANT, 1, new Properties()));
    }
    
    /**
     * Helper to be used by subclasses to emit critical warnings
     * @param warning
     */
    protected void critical(String type) {
        _store.addWarning(new QAWarning(type, null, QAWarning.Severity.CRITICAL, 1, new Properties()));
    }
}
