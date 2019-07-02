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
package org.openrefine.wikidata.updates.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.ItemUpdateBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class QuickStatementsUpdateScheduler implements UpdateScheduler {

    private PointerExtractor extractor = new PointerExtractor();

    /**
     * This map holds for each new entity id value a list of updates that refer to
     * this id (and should hence be scheduled right after creation of that entity).
     */
    private Map<ItemIdValue, UpdateSequence> pointerUpdates;

    /**
     * This contains all updates which do not refer to any new entity apart from
     * possibly the subject, in the order that they were supplied to us.
     */
    private UpdateSequence pointerFreeUpdates;

    /**
     * Separates out the statements which refer to new items from the rest of the
     * update. The resulting updates are stored in {@link referencingUpdates} and
     * {@link updatesWithoutReferences}.
     * 
     * @param update
     * @throws ImpossibleSchedulingException
     *             if two new item ids are referred to in the same statement
     */
    protected void splitUpdate(ItemUpdate update)
            throws ImpossibleSchedulingException {
        ItemUpdateBuilder remainingUpdateBuilder = new ItemUpdateBuilder(update.getItemId())
                .addLabels(update.getLabels(), true)
                .addLabels(update.getLabelsIfNew(), false)
                .addDescriptions(update.getDescriptions(), true)
                .addDescriptions(update.getDescriptionsIfNew(), false)
                .addAliases(update.getAliases())
                .deleteStatements(update.getDeletedStatements());
        Map<ItemIdValue, ItemUpdateBuilder> referencingUpdates = new HashMap<>();

        for (Statement statement : update.getAddedStatements()) {
            Set<ReconItemIdValue> pointers = extractor.extractPointers(statement);
            if (pointers.isEmpty()) {
                remainingUpdateBuilder.addStatement(statement);
            } else if (pointers.size() == 1 && !update.isNew()) {
                ItemIdValue pointer = pointers.stream().findFirst().get();
                ItemUpdateBuilder referencingBuilder = referencingUpdates.get(pointer);
                if (referencingBuilder == null) {
                    referencingBuilder = new ItemUpdateBuilder(update.getItemId());
                }
                referencingBuilder.addStatement(statement);
                referencingUpdates.put(pointer, referencingBuilder);
            } else if (pointers.size() == 1 && pointers.stream().findFirst().get().equals(update.getItemId())) {
                remainingUpdateBuilder.addStatement(statement);
            } else {
                throw new ImpossibleSchedulingException();
            }
        }

        // Add the update that is not referring to anything to the schedule
        ItemUpdate pointerFree = remainingUpdateBuilder.build();
        if (!pointerFree.isNull()) {
            pointerFreeUpdates.add(pointerFree);
        }
        // Add the other updates to the map
        for (Entry<ItemIdValue, ItemUpdateBuilder> entry : referencingUpdates.entrySet()) {
            ItemUpdate pointerUpdate = entry.getValue().build();
            UpdateSequence pointerUpdatesForKey = pointerUpdates.get(entry.getKey());
            if (pointerUpdatesForKey == null) {
                pointerUpdatesForKey = new UpdateSequence();
            }
            pointerUpdatesForKey.add(pointerUpdate);
            pointerUpdates.put(entry.getKey(), pointerUpdatesForKey);
        }
    }

    @Override
    public List<ItemUpdate> schedule(List<ItemUpdate> updates)
            throws ImpossibleSchedulingException {
        pointerUpdates = new HashMap<>();
        pointerFreeUpdates = new UpdateSequence();

        for (ItemUpdate update : updates) {
            splitUpdate(update);
        }

        // Reconstruct
        List<ItemUpdate> fullSchedule = new ArrayList<>();
        Set<ItemIdValue> mentionedNewEntities = new HashSet<>(pointerUpdates.keySet());
        for (ItemUpdate update : pointerFreeUpdates.getUpdates()) {
            fullSchedule.add(update);
            UpdateSequence backPointers = pointerUpdates.get(update.getItemId());
            if (backPointers != null) {
                fullSchedule.addAll(backPointers.getUpdates());
            }
            mentionedNewEntities.remove(update.getItemId());
        }

        // Create any item that was referred to but untouched
        // (this is just for the sake of correctness, it would be bad to do that
        // as the items would remain blank in this batch).
        for (ItemIdValue missingId : mentionedNewEntities) {
            fullSchedule.add(new ItemUpdateBuilder(missingId).build());
            fullSchedule.addAll(pointerUpdates.get(missingId).getUpdates());
        }
        return fullSchedule;
    }

}
