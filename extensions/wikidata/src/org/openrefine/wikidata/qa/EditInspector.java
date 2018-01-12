package org.openrefine.wikidata.qa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrefine.wikidata.qa.scrutinizers.DistinctValuesScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.EditScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.FormatConstraintScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.InverseConstraintScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.NewItemScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.NoEditsMadeScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.QualifierCompatibilityScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.RestrictedPositionScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.SelfReferentialScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.SingleValueScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.UnsourcedScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.WhitespaceScrutinizer;
import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

/**
 * Runs a collection of edit scrutinizers on an edit batch
 * @author antonin
 *
 */
public class EditInspector {
    private Map<String, EditScrutinizer> scrutinizers;
    private QAWarningStore warningStore;
    
    public EditInspector(QAWarningStore warningStore) {
        this.scrutinizers = new HashMap<>();
        this.warningStore = warningStore;
        
        // Register all known scrutinizers here
        register(new NewItemScrutinizer());
        register(new FormatConstraintScrutinizer());
        register(new InverseConstraintScrutinizer());
        register(new SelfReferentialScrutinizer());
        register(new UnsourcedScrutinizer());
        register(new RestrictedPositionScrutinizer());
        register(new QualifierCompatibilityScrutinizer());
        register(new SingleValueScrutinizer());
        register(new DistinctValuesScrutinizer());
        register(new NoEditsMadeScrutinizer());
        register(new WhitespaceScrutinizer());
    }
    
    /**
     * Adds a new scrutinizer to the inspector
     * @param scrutinizer
     */
    public void register(EditScrutinizer scrutinizer) {
        String key = scrutinizer.getClass().getName();
        scrutinizers.put(key, scrutinizer);
        scrutinizer.setStore(warningStore);
    }
    
    
    /**
     * Inspect a batch of edits with the registered scrutinizers
     * @param editBatch
     */
    public void inspect(List<ItemUpdate> editBatch) {
        Map<EntityIdValue, ItemUpdate> updates =  ItemUpdate.groupBySubject(editBatch);
        List<ItemUpdate> mergedUpdates = updates.values().stream().collect(Collectors.toList());
        for(EditScrutinizer scrutinizer : scrutinizers.values()) {
            scrutinizer.scrutinize(mergedUpdates);
        }
        
        if (warningStore.getNbWarnings() == 0) {
            warningStore.addWarning(new QAWarning(
                "no-issue-detected", null, QAWarning.Severity.INFO, 0));
        }
    }
}
