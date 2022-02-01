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

import org.openrefine.wikidata.manifests.Manifest;
import org.openrefine.wikidata.qa.ConstraintFetcher;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarning.Severity;
import org.openrefine.wikidata.qa.QAWarningStore;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.ArrayList;
import java.util.List;

/**
 * Inspects an edit batch and emits warnings.
 * 
 * @author Antonin Delpeuch
 */
public abstract class EditScrutinizer {

    protected QAWarningStore _store;
    protected ConstraintFetcher _fetcher;
    protected Manifest manifest;

    public void setStore(QAWarningStore store) {
        _store = store;
    }

    /**
     * The fetcher will be set to null if 'property_constraint_pid' is missing in the manifest.
     */
    public void setFetcher(ConstraintFetcher fetcher) {
        _fetcher = fetcher;
    }

    public void setManifest(Manifest manifest) {
        this.manifest = manifest;
    }

    public String getConstraintsRelatedId(String name) {
        return manifest.getConstraintsRelatedId(name);
    }

    /**
     * Prepare the dependencies(i.e. constraint-related pids and qids) needed by the scrutinizer.
     *
     * Called before {@link EditScrutinizer#batchIsBeginning()}.
     *
     * @return false if any necessary dependency is missing, true otherwise.
     */
    public abstract boolean prepareDependencies();
    
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
    public abstract void scrutinize(TermedStatementEntityUpdate edit);
    
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
     */
    protected void info(String type) {
        addIssue(type, null, QAWarning.Severity.INFO, 1);

    }

    /**
     * Helper to be used by subclasses to emit simple warnings
     */
    protected void warning(String type) {
        addIssue(type, null, QAWarning.Severity.WARNING, 1);
    }

    /**
     * Helper to be used by subclasses to emit simple important warnings
     */
    protected void important(String type) {
        addIssue(type, null, QAWarning.Severity.IMPORTANT, 1);
    }

    /**
     * Helper to be used by subclasses to emit simple critical warnings
     */
    protected void critical(String type) {
        addIssue(type, null, QAWarning.Severity.CRITICAL, 1);
    }

    /**
     * Returns the values of a given property in qualifiers
     *
     * @param groups
     *            the qualifiers
     * @param pid
     *            the property to filter on
     * @return
     */
    protected List<Value> findValues(List<SnakGroup> groups, String pid) {
        List<Value> results = new ArrayList<>();
        for (SnakGroup group : groups) {
            if (group.getProperty().getId().equals(pid)) {
                for (Snak snak : group.getSnaks())
                    if (snak instanceof ValueSnak) {
                        results.add(((ValueSnak)snak).getValue());
                    }
            }
        }
        return results;
    }
}
