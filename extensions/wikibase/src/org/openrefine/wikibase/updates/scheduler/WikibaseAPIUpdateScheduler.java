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

import java.util.*;
import java.util.stream.Collectors;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.ItemEditBuilder;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.MediaInfoEditBuilder;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.TermedStatementEntityEdit;

/**
 * A simple scheduler for batches committed via the Wikibase API.
 * 
 * The strategy is quite simple and makes at most two edits per touched entity (which is not minimal though). Each
 * update is split between statements making references to new entities, and statements not making these references. All
 * updates with no references to new entities are done first (which creates all new entities), then all other updates
 * are done.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WikibaseAPIUpdateScheduler implements UpdateScheduler {

    /**
     * The first part of updates: the ones which create new entities without referring to any other new entity.
     */
    private UpdateSequence pointerFreeUpdates;
    /**
     * The second part of the updates: all existing entities, plus all parts of new entities that refer to other new
     * entities.
     */
    private UpdateSequence pointerFullUpdates;
    /**
     * The set of all new entities referred to in the whole batch, mapping to a row id where they are referred to
     */
    private Map<EntityIdValue, Integer> allPointers;

    private PointerExtractor extractor = new PointerExtractor();

    @Override
    public List<EntityEdit> schedule(List<EntityEdit> updates) throws ImpossibleSchedulingException {
        List<EntityEdit> result = new ArrayList<>();
        pointerFreeUpdates = new UpdateSequence();
        pointerFullUpdates = new UpdateSequence();
        allPointers = new HashMap<>();

        for (EntityEdit update : updates) {
            splitUpdate(update);
        }

        // Part 1: add all the pointer free updates
        result.addAll(pointerFreeUpdates.getUpdates());

        // Part 1': add the remaining new entities that have not been touched
        Map<EntityIdValue, Integer> unseenPointers = new HashMap<>(allPointers);
        for (EntityIdValue id : pointerFreeUpdates.getSubjects()) {
            unseenPointers.remove(id);
        }
        // Only items can be created explicitly: other entity types need at least some non-blank field.
        // Therefore, we check that all entities are items.
        Optional<Map.Entry<EntityIdValue, Integer>> uncreatableEntity = unseenPointers
                .entrySet()
                .stream()
                .filter(t -> !(t.getKey() instanceof ItemIdValue))
                .findAny();
        if (uncreatableEntity.isPresent()) {
            throw new ImpossibleSchedulingException(
                    "The batch contains on row " + uncreatableEntity.get().getValue() + " a reference to a new entity (" +
                            uncreatableEntity.get().getKey().toString() + ") which is never explicitly created in the batch. " +
                            "It cannot be created implicitly as creating a blank entity of this type is impossible.");
        }
        // At this stage, we know that all entities are items, thanks to the check above
        result.addAll(unseenPointers.entrySet().stream().map(entry -> new ItemEditBuilder(entry.getKey())
                .addContributingRowId(entry.getValue())
                .build()).collect(Collectors.toList()));

        // Part 2: add all the pointer full updates
        result.addAll(pointerFullUpdates.getUpdates());

        return result;
    }

    /**
     * Splits an update into two parts
     * 
     * @param edit
     */
    protected void splitUpdate(EntityEdit edit) {
        // TODO (antonin, 2022-05-08): there is a lot of duplication in the two cases below (Item / MediaInfo),
        // could we refactor that?
        int rowId = edit.getContributingRowIds().stream().findAny().get();
        if (edit instanceof ItemEdit) {
            ItemEdit update = (ItemEdit) edit;
            ItemEditBuilder pointerFreeBuilder = new ItemEditBuilder(update.getEntityId())
                    .addLabels(update.getLabels(), true)
                    .addLabels(update.getLabelsIfNew(), false)
                    .addDescriptions(update.getDescriptions(), true)
                    .addDescriptions(update.getDescriptionsIfNew(), false)
                    .addAliases(update.getAliases())
                    .addContributingRowIds(update.getContributingRowIds());
            ItemEditBuilder pointerFullBuilder = new ItemEditBuilder(update.getEntityId())
                    .addContributingRowIds(update.getContributingRowIds());

            for (StatementEdit statement : update.getStatementEdits()) {
                Set<ReconEntityIdValue> pointers = extractor.extractPointers(statement.getStatement());
                if (pointers.isEmpty()) {
                    pointerFreeBuilder.addStatement(statement);
                } else {
                    pointerFullBuilder.addStatement(statement);
                }
                pointers.stream().forEach(pointer -> allPointers.put(pointer, rowId));
            }

            if (update.isNew()) {
                // If the update is new, we might need to split it
                // in two (if it refers to any other new entity).
                TermedStatementEntityEdit pointerFree = pointerFreeBuilder.build();
                if (!pointerFree.isNull()) {
                    pointerFreeUpdates.add(pointerFree);
                }
                TermedStatementEntityEdit pointerFull = pointerFullBuilder.build();
                if (!pointerFull.isEmpty()) {
                    pointerFullUpdates.add(pointerFull);
                }
            } else {
                // Otherwise, we just make sure this edit is done after
                // all entity creations.
                pointerFullUpdates.add(update);
            }
        } else if (edit instanceof MediaInfoEdit) {
            MediaInfoEdit update = (MediaInfoEdit) edit;
            MediaInfoEditBuilder pointerFreeBuilder = new MediaInfoEditBuilder(update.getEntityId())
                    .addFileName(update.getFileName())
                    .addFilePath(update.getFilePath())
                    .addWikitext(update.getWikitext())
                    .setOverrideWikitext(update.isOverridingWikitext())
                    .addLabels(update.getLabels(), true)
                    .addLabels(update.getLabelsIfNew(), false)
                    .addContributingRowIds(update.getContributingRowIds());
            MediaInfoEditBuilder pointerFullBuilder = new MediaInfoEditBuilder(update.getEntityId())
                    .addContributingRowIds(update.getContributingRowIds());

            for (StatementEdit statement : update.getStatementEdits()) {
                Set<ReconEntityIdValue> pointers = extractor.extractPointers(statement.getStatement());
                if (pointers.isEmpty()) {
                    pointerFreeBuilder.addStatement(statement);
                } else {
                    pointerFullBuilder.addStatement(statement);
                }
                pointers.stream().forEach(pointer -> allPointers.put(pointer, rowId));
            }

            if (update.isNew()) {
                // If the update is new, we might need to split it
                // in two (if it refers to any other new entity).
                MediaInfoEdit pointerFree = pointerFreeBuilder.build();
                if (!pointerFree.isNull()) {
                    pointerFreeUpdates.add(pointerFree);
                }
                MediaInfoEdit pointerFull = pointerFullBuilder.build();
                if (!pointerFull.isEmpty()) {
                    pointerFullUpdates.add(pointerFull);
                }
            } else {
                // Otherwise, we just make sure this edit is done after
                // all entity creations.
                pointerFullUpdates.add(update);
            }
        }
    }

}
