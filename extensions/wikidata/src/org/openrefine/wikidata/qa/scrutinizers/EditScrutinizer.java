package org.openrefine.wikidata.qa.scrutinizers;

import java.util.List;
import java.util.Map;

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarning.Severity;
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
     
    protected void addIssue(QAWarning warning) {
        _store.addWarning(warning);
    }
    
    protected void addIssue(String type, String aggregationId, Severity severity, int count) {
        addIssue(new QAWarning(type, aggregationId, severity, count));
    }

    /**
     * Helper to be used by subclasses to emit simple INFO warnings
     * @param warning
     */
    protected void info(String type) {
        addIssue(type, null, QAWarning.Severity.INFO, 1);
        
    }

    /**
     * Helper to be used by subclasses to emit simple warnings
     * @param warning
     */
    protected void warning(String type) {
        addIssue(type, null, QAWarning.Severity.WARNING, 1);
    }
    
    /**
     * Helper to be used by subclasses to emit simple important warnings
     * @param warning
     */
    protected void important(String type) {
        addIssue(type, null, QAWarning.Severity.IMPORTANT, 1);
    }
    
    /**
     * Helper to be used by subclasses to emit simple critical warnings
     * @param warning
     */
    protected void critical(String type) {
        addIssue(type, null, QAWarning.Severity.CRITICAL, 1);
    }
}
