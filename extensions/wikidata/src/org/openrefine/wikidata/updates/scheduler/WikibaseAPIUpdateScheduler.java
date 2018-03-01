package org.openrefine.wikidata.updates.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A simple scheduler for batches commited via the Wikibase API.
 * 
 * The strategy is quite simple and makes at most two edits
 * per touched item (which is not minimal though). Each update
 * is split between statements making references to new items,
 * and statements not making these references. All updates with no
 * references to new items are done first (which creates all new
 * items), then all other updates are done.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WikibaseAPIUpdateScheduler implements UpdateScheduler {
    
    /**
     * The first part of updates: the ones which create new items
     * without referring to any other new item.
     */
    private UpdateSequence pointerFreeUpdates;
    /**
     * The second part of the updates: all existing items, plus
     * all parts of new items that refer to other new items.
     */
    private UpdateSequence pointerFullUpdates;
    /**
     * The set of all new items referred to in the whole batch.
     */
    private Set<ItemIdValue> allPointers;
    
    private PointerExtractor extractor = new PointerExtractor();

    @Override
    public List<ItemUpdate> schedule(List<ItemUpdate> updates) {
        List<ItemUpdate> result = new ArrayList<>();
        pointerFreeUpdates = new UpdateSequence();
        pointerFullUpdates = new UpdateSequence();
        allPointers = new HashSet<>();
        
        for(ItemUpdate update : updates) {
            splitUpdate(update);
        }
        
        // Part 1: add all the pointer free updates
        result.addAll(pointerFreeUpdates.getUpdates());
        
        // Part 1': add the remaining new items that have not been touched
        Set<ItemIdValue> unseenPointers = new HashSet<>(allPointers);
        unseenPointers.removeAll(pointerFreeUpdates.getSubjects());
        
        result.addAll(unseenPointers.stream()
                .map(e -> new ItemUpdateBuilder(e).build())
                .collect(Collectors.toList()));
        
        // Part 2: add all the pointer full updates
        result.addAll(pointerFullUpdates.getUpdates());

        return result;
    }

    /**
     * Splits an update into two parts
     * @param update
     */
    protected void splitUpdate(ItemUpdate update) {
        ItemUpdateBuilder pointerFreeBuilder = new ItemUpdateBuilder(update.getItemId())
                .addLabels(update.getLabels())
                .addDescriptions(update.getDescriptions())
                .addAliases(update.getAliases())
                .deleteStatements(update.getDeletedStatements());
        ItemUpdateBuilder pointerFullBuilder = new ItemUpdateBuilder(update.getItemId());
        
        for(Statement statement : update.getAddedStatements()) {
            Set<ReconItemIdValue> pointers = extractor.extractPointers(statement);
            if (pointers.isEmpty()) {
                pointerFreeBuilder.addStatement(statement);
            } else {
                pointerFullBuilder.addStatement(statement);
            }
            allPointers.addAll(pointers);
        }
        
        if(update.isNew()) {
            // If the update is new, we might need to split it
            // in two (if it refers to any other new entity).
            ItemUpdate pointerFree = pointerFreeBuilder.build();
            if (!pointerFree.isNull()) {
                pointerFreeUpdates.add(pointerFree);
            }
            ItemUpdate pointerFull = pointerFullBuilder.build();
            if (!pointerFull.isEmpty()) {
                pointerFullUpdates.add(pointerFull);
            }
        } else {
            // Otherwise, we just make sure this edit is done after
            // all item creations.
            pointerFullUpdates.add(update);
        }
    }
    
}
