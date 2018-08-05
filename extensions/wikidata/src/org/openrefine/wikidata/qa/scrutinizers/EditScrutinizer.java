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

import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarning.Severity;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.updates.ItemUpdate;

/**
 * Inspects an edit batch and emits warnings.
 * 
 * @author Antonin Delpeuch
 */
public abstract class EditScrutinizer {

    protected QAWarningStore _store;
    protected ConstraintFetcher _fetcher;

    public EditScrutinizer() {
        _fetcher = null;
        _store = null;
    }

    public void setStore(QAWarningStore store) {
        _store = store;
    }

    public void setFetcher(ConstraintFetcher fetcher) {
        _fetcher = fetcher;
    }
    
    /**
     * Called before an edit batch is scrutinized.
     */
    public void batchIsBeginning() {
        
    }

    /**
     * Reads the candidate edits and emits warnings in the store
     * 
     * @param edit:
     *            the list of ItemUpdates to scrutinize
     */
    public abstract void scrutinize(ItemUpdate edit);
    
    /**
     * Method called once the edit batch has been read entirely
     */
    public void batchIsFinished() {
        
    }
    
    /**
     * Emits an issue that will be reported to the user,
     * after merging with other issues of the same kind.
     * 
     * @param warning
     *    the issue to report
     */
    protected void addIssue(QAWarning warning) {
        _store.addWarning(warning);
    }

    protected void addIssue(String type, String aggregationId, Severity severity, int count) {
        addIssue(new QAWarning(type, aggregationId, severity, count));
    }

    /**
     * Helper to be used by subclasses to emit simple INFO warnings
     * 
     * @param warning
     */
    protected void info(String type) {
        addIssue(type, null, QAWarning.Severity.INFO, 1);

    }

    /**
     * Helper to be used by subclasses to emit simple warnings
     * 
     * @param warning
     */
    protected void warning(String type) {
        addIssue(type, null, QAWarning.Severity.WARNING, 1);
    }

    /**
     * Helper to be used by subclasses to emit simple important warnings
     * 
     * @param warning
     */
    protected void important(String type) {
        addIssue(type, null, QAWarning.Severity.IMPORTANT, 1);
    }

    /**
     * Helper to be used by subclasses to emit simple critical warnings
     * 
     * @param warning
     */
    protected void critical(String type) {
        addIssue(type, null, QAWarning.Severity.CRITICAL, 1);
    }
}
