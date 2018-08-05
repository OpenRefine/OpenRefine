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
package org.openrefine.wikidata.editing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrefine.wikidata.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.scheduler.WikibaseAPIUpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

/**
 * Schedules and performs a list of updates to items via the API.
 * 
 * @author Antonin Delpeuch
 *
 */
public class EditBatchProcessor {

    static final Logger logger = LoggerFactory.getLogger(EditBatchProcessor.class);

    private WikibaseDataFetcher fetcher;
    private WikibaseDataEditor editor;
    private NewItemLibrary library;
    private List<ItemUpdate> scheduled;
    private String summary;

    private List<ItemUpdate> remainingUpdates;
    private List<ItemUpdate> currentBatch;
    private int batchCursor;
    private int globalCursor;
    private Map<String, EntityDocument> currentDocs;
    private int batchSize;

    /**
     * Initiates the process of pushing a batch of updates to Wikibase. This
     * schedules the updates and is a prerequisite for calling
     * {@link performOneEdit}.
     * 
     * @param fetcher
     *            the fetcher to use to retrieve the current state of items
     * @param editor
     *            the object to use to perform the edits
     * @param updates
     *            the list of item updates to perform
     * @param library
     *            the library to use to keep track of new item creation
     * @param summary
     *            the summary to append to all edits
     * @param batchSize
     *            the number of items that should be retrieved in one go from the
     *            API
     */
    public EditBatchProcessor(WikibaseDataFetcher fetcher, WikibaseDataEditor editor, List<ItemUpdate> updates,
            NewItemLibrary library, String summary, int batchSize) {
        this.fetcher = fetcher;
        this.editor = editor;
        editor.setEditAsBot(true); // this will not do anything if the user does not
        // have a bot flag, and this is generally wanted if they have one.
        this.library = library;
        this.summary = summary;
        this.batchSize = batchSize;

        // Schedule the edit batch
        WikibaseAPIUpdateScheduler scheduler = new WikibaseAPIUpdateScheduler();
        this.scheduled = scheduler.schedule(updates);
        this.globalCursor = 0;

        this.batchCursor = 0;
        this.remainingUpdates = new ArrayList<>(scheduled);
        this.currentBatch = Collections.emptyList();
        this.currentDocs = Collections.emptyMap();
    }

    /**
     * Performs the next edit in the batch.
     * 
     * @throws InterruptedException
     */
    public void performEdit()
            throws InterruptedException {
        if (remainingEdits() == 0) {
            return;
        }
        if (batchCursor == currentBatch.size()) {
            prepareNewBatch();
        }
        ItemUpdate update = currentBatch.get(batchCursor);

        // Rewrite mentions to new items
        ReconEntityRewriter rewriter = new ReconEntityRewriter(library, update.getItemId());
        update = rewriter.rewrite(update);

        try {
            // New item
            if (update.isNew()) {
                ReconEntityIdValue newCell = (ReconEntityIdValue) update.getItemId();
                update = update.normalizeLabelsAndAliases();

                ItemDocument itemDocument = Datamodel.makeItemDocument(update.getItemId(),
                        update.getLabels().stream().collect(Collectors.toList()),
                        update.getDescriptions().stream().collect(Collectors.toList()),
                        update.getAliases().stream().collect(Collectors.toList()), update.getAddedStatementGroups(),
                        Collections.emptyMap());

                ItemDocument createdDoc = editor.createItemDocument(itemDocument, summary);
                library.setQid(newCell.getReconInternalId(), createdDoc.getItemId().getId());
            } else {
                // Existing item
                ItemDocument currentDocument = (ItemDocument) currentDocs.get(update.getItemId().getId());
                editor.updateTermsStatements(currentDocument, update.getLabels().stream().collect(Collectors.toList()),
                        update.getDescriptions().stream().collect(Collectors.toList()),
                        update.getAliases().stream().collect(Collectors.toList()),
                        new ArrayList<MonolingualTextValue>(),
                        update.getAddedStatements().stream().collect(Collectors.toList()),
                        update.getDeletedStatements().stream().collect(Collectors.toList()), summary);
            }
        } catch (MediaWikiApiErrorException e) {
            // TODO find a way to report these errors to the user in a nice way
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        batchCursor++;
    }

    /**
     * @return the number of edits that remain to be done in the current batch
     */
    public int remainingEdits() {
        return scheduled.size() - (globalCursor + batchCursor);
    }

    /**
     * @return the progress, measured as a percentage
     */
    public int progress() {
        return (100 * (globalCursor + batchCursor)) / scheduled.size();
    }

    protected void prepareNewBatch()
            throws InterruptedException {
        // remove the previous batch from the remainingUpdates
        globalCursor += currentBatch.size();
        currentBatch.clear();

        if (remainingUpdates.size() < batchSize) {
            currentBatch = remainingUpdates;
            remainingUpdates = Collections.emptyList();
        } else {
            currentBatch = remainingUpdates.subList(0, batchSize);
        }
        List<String> qidsToFetch = currentBatch.stream().filter(u -> !u.isNew()).map(u -> u.getItemId().getId())
                .collect(Collectors.toList());

        // Get the current documents for this batch of updates
        logger.info("Requesting documents");
        currentDocs = null;
        int retries = 3;
        while (currentDocs == null && retries > 0) {
            try {
                currentDocs = fetcher.getEntityDocuments(qidsToFetch);
            } catch (MediaWikiApiErrorException e) {
                e.printStackTrace();
                Thread.sleep(5000);
            }
            retries--;
        }
        if (currentDocs == null) {
            throw new InterruptedException("Fetching current documents failed.");
        }
        batchCursor = 0;
    }

}
