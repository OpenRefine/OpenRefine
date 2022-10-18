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

package org.openrefine.wikibase.updates.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;

public class QuickStatementsUpdateScheduler implements UpdateScheduler {

    private PointerExtractor extractor = new PointerExtractor();

    /**
     * This map holds for each new entity id value a list of updates that refer to this id (and should hence be
     * scheduled right after creation of that entity).
     */
    private Map<EntityIdValue, UpdateSequence> pointerUpdates;

    /**
     * This contains all updates which do not refer to any new entity apart from possibly the subject, in the order that
     * they were supplied to us.
     */
    private UpdateSequence pointerFreeUpdates;

    /**
     * Separates out the statements which refer to new entities from the rest of the update. The resulting updates are
     * stored in {@link #pointerUpdates} and {@link #pointerFreeUpdates}.
     * 
     * @param edit
     *            the edit to be split
     * @throws ImpossibleSchedulingException
     *             if two new entity ids are referred to in the same statement
     */
    protected void splitUpdate(EntityEdit edit)
            throws ImpossibleSchedulingException {
        if (edit instanceof ItemEdit) {
            ItemEdit update = (ItemEdit) edit;
            ItemEditBuilder remainingUpdateBuilder = new ItemEditBuilder(update.getEntityId())
                    .addLabels(update.getLabels(), true)
                    .addLabels(update.getLabelsIfNew(), false)
                    .addDescriptions(update.getDescriptions(), true)
                    .addDescriptions(update.getDescriptionsIfNew(), false)
                    .addAliases(update.getAliases());
            Map<EntityIdValue, ItemEditBuilder> referencingUpdates = new HashMap<>();

            for (StatementEdit statement : update.getStatementEdits()) {
                Set<ReconEntityIdValue> pointers = extractor.extractPointers(statement.getStatement());
                if (pointers.isEmpty()) {
                    remainingUpdateBuilder.addStatement(statement);
                } else if (pointers.size() == 1 && !update.isNew()) {
                    EntityIdValue pointer = pointers.stream().findFirst().get();
                    ItemEditBuilder referencingBuilder = referencingUpdates.get(pointer);
                    if (referencingBuilder == null) {
                        referencingBuilder = new ItemEditBuilder(update.getEntityId());
                    }
                    referencingBuilder.addStatement(statement);
                    referencingUpdates.put(pointer, referencingBuilder);
                } else if (pointers.size() == 1 && pointers.stream().findFirst().get().equals(update.getEntityId())) {
                    remainingUpdateBuilder.addStatement(statement);
                } else {
                    throw new ImpossibleSchedulingException(
                            "An item refers to multiple new entities, which is not supported by QuickStatements");
                }
            }

            // Add the update that is not referring to anything to the schedule
            ItemEdit pointerFree = remainingUpdateBuilder.build();
            if (!pointerFree.isNull()) {
                pointerFreeUpdates.add(pointerFree);
            }
            // Add the other updates to the map
            for (Entry<EntityIdValue, ItemEditBuilder> entry : referencingUpdates.entrySet()) {
                TermedStatementEntityEdit pointerUpdate = entry.getValue().build();
                UpdateSequence pointerUpdatesForKey = pointerUpdates.get(entry.getKey());
                if (pointerUpdatesForKey == null) {
                    pointerUpdatesForKey = new UpdateSequence();
                }
                pointerUpdatesForKey.add(pointerUpdate);
                pointerUpdates.put(entry.getKey(), pointerUpdatesForKey);
            }
        } else if (edit instanceof MediaInfoEdit) {
            MediaInfoEdit update = (MediaInfoEdit) edit;
            MediaInfoEditBuilder remainingUpdateBuilder = new MediaInfoEditBuilder(update.getEntityId())
                    .addLabels(update.getLabels(), true)
                    .addLabels(update.getLabelsIfNew(), false);
            Map<EntityIdValue, MediaInfoEditBuilder> referencingUpdates = new HashMap<>();

            for (StatementEdit statement : update.getStatementEdits()) {
                Set<ReconEntityIdValue> pointers = extractor.extractPointers(statement.getStatement());
                if (pointers.isEmpty()) {
                    remainingUpdateBuilder.addStatement(statement);
                } else if (pointers.size() == 1 && !update.isNew()) {
                    EntityIdValue pointer = pointers.stream().findFirst().get();
                    MediaInfoEditBuilder referencingBuilder = referencingUpdates.get(pointer);
                    if (referencingBuilder == null) {
                        referencingBuilder = new MediaInfoEditBuilder(update.getEntityId());
                    }
                    referencingBuilder.addStatement(statement);
                    referencingUpdates.put(pointer, referencingBuilder);
                } else if (pointers.size() == 1 && pointers.stream().findFirst().get().equals(update.getEntityId())) {
                    remainingUpdateBuilder.addStatement(statement);
                } else {
                    throw new ImpossibleSchedulingException(
                            "A mediainfo entity refers to multiple new entities, which is not supported by QuickStatements");
                }
            }

            // Add the update that is not referring to anything to the schedule
            MediaInfoEdit pointerFree = remainingUpdateBuilder.build();
            if (!pointerFree.isNull()) {
                pointerFreeUpdates.add(pointerFree);
            }
            // Add the other updates to the map
            for (Entry<EntityIdValue, MediaInfoEditBuilder> entry : referencingUpdates.entrySet()) {
                MediaInfoEdit pointerUpdate = entry.getValue().build();
                UpdateSequence pointerUpdatesForKey = pointerUpdates.get(entry.getKey());
                if (pointerUpdatesForKey == null) {
                    pointerUpdatesForKey = new UpdateSequence();
                }
                pointerUpdatesForKey.add(pointerUpdate);
                pointerUpdates.put(entry.getKey(), pointerUpdatesForKey);
            }
        }
    }

    @Override
    public List<EntityEdit> schedule(List<EntityEdit> updates)
            throws ImpossibleSchedulingException {
        pointerUpdates = new HashMap<>();
        pointerFreeUpdates = new UpdateSequence();

        for (EntityEdit update : updates) {
            splitUpdate(update);
        }

        // Reconstruct
        List<EntityEdit> fullSchedule = new ArrayList<>();
        Set<EntityIdValue> mentionedNewEntities = new HashSet<>(pointerUpdates.keySet());
        for (EntityEdit update : pointerFreeUpdates.getUpdates()) {
            fullSchedule.add(update);
            UpdateSequence backPointers = pointerUpdates.get(update.getEntityId());
            if (backPointers != null) {
                fullSchedule.addAll(backPointers.getUpdates());
            }
            mentionedNewEntities.remove(update.getEntityId());
        }

        // Create any entity that was referred to but untouched
        // (this is just for the sake of correctness, it would be bad to do that
        // as the entities would remain blank in this batch).
        for (EntityIdValue missingId : mentionedNewEntities) {
            fullSchedule.add(new ItemEditBuilder(missingId).build());
            fullSchedule.addAll(pointerUpdates.get(missingId).getUpdates());
        }
        return fullSchedule;
    }

}
