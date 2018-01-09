package org.openrefine.wikidata.qa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.wikidata.qa.scrutinizers.EditScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.FormatConstraintScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.InverseConstraintScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.NewItemScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.QualifierCompatibilityScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.RestrictedPositionScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.SelfReferentialScrutinizer;
import org.openrefine.wikidata.qa.scrutinizers.UnsourcedScrutinizer;
import org.openrefine.wikidata.schema.ItemUpdate;

/**
 * Runs a collection of edit scrutinizers on an edit batch
 * @author antonin
 *
 */
public class EditInspector {
    private Map<String, EditScrutinizer> scrutinizers;
    private QAWarningStore warningStore;
    
    public EditInspector() {
        scrutinizers = new HashMap<>();
        warningStore = new QAWarningStore();
        
        // Register all known scrutinizers here
        register(new NewItemScrutinizer());
        register(new FormatConstraintScrutinizer());
        register(new InverseConstraintScrutinizer());
        register(new SelfReferentialScrutinizer());
        register(new UnsourcedScrutinizer());
        register(new RestrictedPositionScrutinizer());
        register(new QualifierCompatibilityScrutinizer());
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
        for(EditScrutinizer scrutinizer : scrutinizers.values()) {
            scrutinizer.scrutinize(editBatch);
        }
    }
    
    /**
     * Retrieve the warnings after inspection of the edits
     * @return
     */
    public List<QAWarning> getWarnings() {
        return warningStore.getWarnings();
    }
    
}
