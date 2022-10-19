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

package org.openrefine.wikibase.editing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.schema.exceptions.NewEntityNotCreatedYetException;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.FullMediaInfoUpdate;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.scheduler.ImpossibleSchedulingException;
import org.openrefine.wikibase.updates.scheduler.WikibaseAPIUpdateScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityUpdate;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

/**
 * Schedules and performs a list of updates to entities via the API.
 * 
 * @author Antonin Delpeuch
 *
 */
public class EditBatchProcessor {

    static final Logger logger = LoggerFactory.getLogger(EditBatchProcessor.class);

    private WikibaseDataFetcher fetcher;
    private WikibaseDataEditor editor;
    private ApiConnection connection;
    private NewEntityLibrary library;
    private List<EntityEdit> scheduled;
    private String summary;
    private List<String> tags;

    private List<EntityEdit> remainingUpdates;
    private List<EntityEdit> currentBatch;
    private int batchCursor;
    private int globalCursor;
    private Map<String, EntityDocument> currentDocs;
    private int batchSize;

    /**
     * Initiates the process of pushing a batch of updates to Wikibase. This schedules the updates and is a prerequisite
     * for calling {@link #performEdit()}.
     * 
     * @param fetcher
     *            the data fetcher to fetch the existing state of the entities to edit
     * @param editor
     *            the editor to perform the edits
     * @param connection
     *            the connection to use to retrieve the current state of entities and edit them
     * @param entityDocuments
     *            the list of entity updates to perform
     * @param library
     *            the library to use to keep track of new entity creation
     * @param summary
     *            the summary to append to all edits
     * @param tags
     *            the list of tags to apply to all edits
     * @param batchSize
     *            the number of entities that should be retrieved in one go from the API
     * @param maxEditsPerMinute
     *            the maximum number of edits per minute to do
     */
    public EditBatchProcessor(WikibaseDataFetcher fetcher, WikibaseDataEditor editor, ApiConnection connection,
            List<EntityEdit> entityDocuments,
            NewEntityLibrary library, String summary, int maxLag, List<String> tags, int batchSize, int maxEditsPerMinute) {
        this.fetcher = fetcher;
        this.editor = editor;
        this.connection = connection;
        editor.setEditAsBot(true); // this will not do anything if the user does not
        // have a bot flag, and this is generally wanted if they have one.

        // edit at 60 edits/min by default. If the Wikibase is overloaded
        // it will slow us down via the maxlag mechanism.
        editor.setAverageTimePerEdit(maxEditsPerMinute <= 0 ? 0 : (int) (1000 * (maxEditsPerMinute / 60.)));
        // set maxlag based on preference store
        editor.setMaxLag(maxLag);

        this.library = library;
        this.summary = summary;
        this.tags = tags;
        this.batchSize = batchSize;

        // Schedule the edit batch
        WikibaseAPIUpdateScheduler scheduler = new WikibaseAPIUpdateScheduler();
        try {
            this.scheduled = scheduler.schedule(entityDocuments);
        } catch (ImpossibleSchedulingException e) {
            throw new IllegalArgumentException(e);
        }
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
        EntityEdit update = currentBatch.get(batchCursor);

        // Rewrite mentions to new entities
        ReconEntityRewriter rewriter = new ReconEntityRewriter(library, update.getEntityId());
        try {
            update = rewriter.rewrite(update);
        } catch (NewEntityNotCreatedYetException e) {
            logger.warn("Failed to rewrite update on entity " + update.getEntityId() + ". Missing entity: " + e.getMissingEntity()
                    + ". Skipping update.");
            batchCursor++;
            return;
        }

        try {
            if (update.isNew()) {
                // New entities
                ReconEntityIdValue newCell = (ReconEntityIdValue) update.getEntityId();
                EntityIdValue createdDocId;
                if (update instanceof MediaInfoEdit) {
                    MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                    createdDocId = ((MediaInfoEdit) update).uploadNewFile(editor, mediaFileUtils, summary, tags);
                } else {
                    createdDocId = editor.createEntityDocument(update.toNewEntity(), summary, tags).getEntityId();
                }
                library.setId(newCell.getReconInternalId(), createdDocId.getId());
            } else {
                // Existing entities
                EntityUpdate entityUpdate;
                if (update.requiresFetchingExistingState()) {
                    entityUpdate = update.toEntityUpdate(currentDocs.get(update.getEntityId().getId()));
                } else {
                    entityUpdate = update.toEntityUpdate(null);
                }

                if (!entityUpdate.isEmpty()) { // skip updates which do not change anything
                    editor.editEntityDocument(entityUpdate, false, summary, tags);
                }
                // custom code for handling our custom updates to mediainfo, which cover editing more than Wikibase
                if (entityUpdate instanceof FullMediaInfoUpdate) {
                    FullMediaInfoUpdate fullMediaInfoUpdate = (FullMediaInfoUpdate) entityUpdate;
                    if (fullMediaInfoUpdate.isOverridingWikitext() && fullMediaInfoUpdate.getWikitext() != null) {
                        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                        long pageId = Long.parseLong(fullMediaInfoUpdate.getEntityId().getId().substring(1));
                        mediaFileUtils.editPage(pageId, fullMediaInfoUpdate.getWikitext(), summary, tags);
                    } else {
                        // manually purge the wikitext page associated with this mediainfo
                        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                        mediaFileUtils.purgePage(Long.parseLong(entityUpdate.getEntityId().getId().substring(1)));
                    }
                }
            }
        } catch (MediaWikiApiErrorException e) {
            // TODO find a way to report these errors to the user in a nice way
            logger.warn("MediaWiki error while editing [" + e.getErrorCode()
                    + "]: " + e.getErrorMessage());
        } catch (IOException e) {
            logger.warn("IO error while editing: " + e.getMessage());
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
        List<String> idsToFetch = currentBatch.stream()
                .filter(u -> u.requiresFetchingExistingState())
                .map(u -> u.getEntityId().getId())
                .collect(Collectors.toList());

        // Get the current documents for this batch of updates
        logger.info("Requesting documents");
        currentDocs = null;
        int retries = 5;
        int backoff = 2;
        int sleepTime = 5000;
        while (currentDocs == null && retries > 0 && !idsToFetch.isEmpty()) {
            try {
                currentDocs = fetcher.getEntityDocuments(idsToFetch);
            } catch (MediaWikiApiErrorException e) {
                logger.warn("MediaWiki error while fetching documents to edit [" + e.getErrorCode()
                        + "]: " + e.getErrorMessage());
            } catch (IOException e) {
                logger.warn("IO error while fetching documents to edit: " + e.getMessage());
            }
            retries--;
            sleepTime *= backoff;
            if ((currentDocs == null || currentDocs.isEmpty()) && retries > 0 && !idsToFetch.isEmpty()) {
                logger.warn("Retrying in " + sleepTime + " ms");
                Thread.sleep(sleepTime);
            }
        }
        if (currentDocs == null && !idsToFetch.isEmpty()) {
            logger.warn("Giving up on fetching documents to edit. Skipping " + remainingEdits() + " remaining edits.");
            globalCursor = scheduled.size();
        }
        batchCursor = 0;
    }

}
