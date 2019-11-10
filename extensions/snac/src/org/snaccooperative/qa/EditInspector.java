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
package org.snaccooperative.qa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.snaccooperative.qa.scrutinizers.DistinctValuesScrutinizer;
import org.snaccooperative.qa.scrutinizers.EditScrutinizer;
import org.snaccooperative.qa.scrutinizers.EntityTypeScrutinizer;
import org.snaccooperative.qa.scrutinizers.FormatScrutinizer;
import org.snaccooperative.qa.scrutinizers.InverseConstraintScrutinizer;
import org.snaccooperative.qa.scrutinizers.NewItemScrutinizer;
import org.snaccooperative.qa.scrutinizers.NoEditsMadeScrutinizer;
import org.snaccooperative.qa.scrutinizers.QualifierCompatibilityScrutinizer;
import org.snaccooperative.qa.scrutinizers.QuantityScrutinizer;
import org.snaccooperative.qa.scrutinizers.RestrictedPositionScrutinizer;
import org.snaccooperative.qa.scrutinizers.RestrictedValuesScrutinizer;
import org.snaccooperative.qa.scrutinizers.SelfReferentialScrutinizer;
import org.snaccooperative.qa.scrutinizers.SingleValueScrutinizer;
import org.snaccooperative.qa.scrutinizers.UnsourcedScrutinizer;
import org.snaccooperative.qa.scrutinizers.WhitespaceScrutinizer;
import org.snaccooperative.updates.ItemUpdate;
import org.snaccooperative.updates.scheduler.SNACAPIUpdateScheduler;
// import org.SNAC.wdtk.datamodel.interfaces.EntityIdValue;

/**
 * Runs a collection of edit scrutinizers on an edit batch.
 * 
 * @author Antonin Delpeuch
 *
 */
public class EditInspector {

    private Map<String, EditScrutinizer> scrutinizers;
    private QAWarningStore warningStore;
    private ConstraintFetcher fetcher;

    public EditInspector(QAWarningStore warningStore) {
        this.scrutinizers = new HashMap<>();
        this.fetcher = new SNACConstraintFetcher();
        this.warningStore = warningStore;

        // Register all known scrutinizers here
        register(new NewItemScrutinizer());
        register(new FormatScrutinizer());
        register(new InverseConstraintScrutinizer());
        register(new SelfReferentialScrutinizer());
        register(new UnsourcedScrutinizer());
        register(new RestrictedPositionScrutinizer());
        register(new QualifierCompatibilityScrutinizer());
        register(new SingleValueScrutinizer());
        register(new DistinctValuesScrutinizer());
        register(new NoEditsMadeScrutinizer());
        register(new WhitespaceScrutinizer());
        register(new QuantityScrutinizer());
        register(new RestrictedValuesScrutinizer());
        register(new EntityTypeScrutinizer());
    }

    /**
     * Adds a new scrutinizer to the inspector
     * 
     * @param scrutinizer
     */
    public void register(EditScrutinizer scrutinizer) {
        String key = scrutinizer.getClass().getName();
        scrutinizers.put(key, scrutinizer);
        scrutinizer.setStore(warningStore);
        scrutinizer.setFetcher(fetcher);
    }

    /**
     * Inspect a batch of edits with the registered scrutinizers
     * 
     * @param editBatch
     */
    public void inspect(List<ItemUpdate> editBatch) {
        // First, schedule them with some scheduler,
        // so that all newly created entities appear in the batch
        SNACAPIUpdateScheduler scheduler = new SNACAPIUpdateScheduler();
        editBatch = scheduler.schedule(editBatch);

        Map<EntityIdValue, ItemUpdate> updates = ItemUpdate.groupBySubject(editBatch);
        List<ItemUpdate> mergedUpdates = updates.values().stream().collect(Collectors.toList());
        
        for (EditScrutinizer scrutinizer : scrutinizers.values()) {
            scrutinizer.batchIsBeginning();
        }
        
        for(ItemUpdate update : mergedUpdates) {
            if(!update.isNull()) {
                for (EditScrutinizer scrutinizer : scrutinizers.values()) {
                    scrutinizer.scrutinize(update);
                }
            }
        }
        
        for(EditScrutinizer scrutinizer : scrutinizers.values()) {
            scrutinizer.batchIsFinished();
        }

        if (warningStore.getNbWarnings() == 0) {
            warningStore.addWarning(new QAWarning("no-issue-detected", null, QAWarning.Severity.INFO, 0));
        }
    }
}
