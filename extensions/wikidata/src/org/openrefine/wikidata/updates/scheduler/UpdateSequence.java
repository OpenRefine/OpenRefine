package org.openrefine.wikidata.updates.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

/**
 * Helper class to store a list of updates where each subject
 * appears at most once. It preserves order of insertion.
 * 
 * @author Antonin Delpeuch
 */
public class UpdateSequence {
    /**
     * The list of updates stored by this container
     */
    private List<ItemUpdate> updates = new ArrayList<>();
    /**
     * An index to keep track of where each item is touched in the sequence
     */
    private Map<ItemIdValue, Integer> index = new HashMap<>();
    
    /**
     * Adds a new update to the list, merging it with any existing
     * one with the same subject.
     * 
     * @param update
     */
    public void add(ItemUpdate update) {
        ItemIdValue subject = update.getItemId();
        if(index.containsKey(subject)) {
            int i = index.get(subject);
            ItemUpdate oldUpdate = updates.get(i);
            updates.set(i, oldUpdate.merge(update));
        } else {
            index.put(subject, updates.size());
            updates.add(update);
        }
    }
    
    /**
     * @return the list of merged updates
     */
    public List<ItemUpdate> getUpdates() {
        return updates;
    }
    
    /**
     * @return the set of touched subjects
     */
    public Set<ItemIdValue> getSubjects() {
        return index.keySet();
    }
}