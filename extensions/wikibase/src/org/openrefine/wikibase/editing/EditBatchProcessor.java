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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityUpdate;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.EditingResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiErrorMessage;

import org.openrefine.wikibase.schema.entityvalues.ReconEntityIdValue;
import org.openrefine.wikibase.schema.exceptions.NewEntityNotCreatedYetException;
import org.openrefine.wikibase.updates.EntityEdit;
import org.openrefine.wikibase.updates.FullMediaInfoUpdate;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.scheduler.ImpossibleSchedulingException;
import org.openrefine.wikibase.updates.scheduler.WikibaseAPIUpdateScheduler;

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
    private LinkedList<String> tagCandidates;

    private String currentTag;
    private List<EntityEdit> remainingUpdates;
    private List<EntityEdit> currentBatch;
    private int batchCursor;
    private int globalCursor;
    private Map<String, EntityDocument> currentDocs;
    private int batchSize;
    private int filePageWaitTime;
    private int filePageMaxWaitTime;

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
     * @param tagCandidates
     *            the list of tags to try to apply to edits. The first existing tag will be added to all edits (if any).
     * @param batchSize
     *            the number of entities that should be retrieved in one go from the API
     * @param maxEditsPerMinute
     *            the maximum number of edits per minute to do
     */
    public EditBatchProcessor(WikibaseDataFetcher fetcher, WikibaseDataEditor editor, ApiConnection connection,
            List<EntityEdit> entityDocuments,
            NewEntityLibrary library, String summary, int maxLag, List<String> tagCandidates, int batchSize, int maxEditsPerMinute) {
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
        this.tagCandidates = new LinkedList<>(tagCandidates);
        this.batchSize = batchSize;
        this.filePageWaitTime = 1000;
        this.filePageMaxWaitTime = 60000;

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
    public EditResult performEdit()
            throws InterruptedException {
        if (remainingEdits() == 0) {
            throw new IllegalStateException("No edit to perform");
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
            batchCursor++;
            return new EditResult(update.getContributingRowIds(),
                    "rewrite-failed",
                    "Failed to rewrite update on entity " + update.getEntityId() + ". Missing entity: " + e.getMissingEntity(),
                    0L, OptionalLong.empty(), null);
        }

        // Pick a tag to apply to the edits
        if (currentTag == null && !tagCandidates.isEmpty()) {
            currentTag = tagCandidates.remove();
        }
        List<String> tags = currentTag == null ? Collections.emptyList() : Collections.singletonList(currentTag);

        long oldRevisionId = 0L;
        OptionalLong lastRevisionId = OptionalLong.empty();
        String newEntityUrl = null;
        try {
            if (update.isNew()) {
                // New entities
                ReconEntityIdValue newCell = (ReconEntityIdValue) update.getEntityId();
                EntityIdValue createdDocId;
                if (update instanceof MediaInfoEdit) {
                    MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                    createdDocId = ((MediaInfoEdit) update).uploadNewFile(editor, mediaFileUtils, summary, tags, filePageWaitTime,
                            filePageMaxWaitTime);
                } else {
                    createdDocId = editor.createEntityDocument(update.toNewEntity(), summary, tags).getEntityId();
                }
                library.setId(newCell.getReconInternalId(), createdDocId.getId());
                newEntityUrl = createdDocId.getSiteIri() + createdDocId.getId();
            } else {
                // Existing entities
                EntityUpdate entityUpdate = null;
                if (update.requiresFetchingExistingState()) {
                    String entityId = update.getEntityId().getId();
                    if (currentDocs.get(entityId) != null) {
                        entityUpdate = update.toEntityUpdate(currentDocs.get(entityId));
                    } else {
                        logger.warn(String.format("Skipping editing of %s as it could not be retrieved", entityId));
                        entityUpdate = null;
                    }
                } else if (!update.isEmpty()) {
                    entityUpdate = update.toEntityUpdate(null);
                }

                if (entityUpdate != null) {
                    oldRevisionId = entityUpdate.getBaseRevisionId();
                }

                if (entityUpdate != null && !entityUpdate.isEmpty()) { // skip updates which do not change anything
                    EditingResult result = editor.editEntityDocument(entityUpdate, false, summary, tags);
                    lastRevisionId = result.getLastRevisionId();
                }
                // custom code for handling our custom updates to mediainfo, which cover editing more than Wikibase
                if (entityUpdate instanceof FullMediaInfoUpdate) {
                    FullMediaInfoUpdate fullMediaInfoUpdate = (FullMediaInfoUpdate) entityUpdate;
                    if (fullMediaInfoUpdate.isOverridingWikitext() && fullMediaInfoUpdate.getWikitext() != null) {
                        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                        long pageId = Long.parseLong(fullMediaInfoUpdate.getEntityId().getId().substring(1));
                        long revisionId = mediaFileUtils.editPage(pageId, fullMediaInfoUpdate.getWikitext(), summary, tags);
                        lastRevisionId = OptionalLong.of(revisionId);
                    } else {
                        // manually purge the wikitext page associated with this mediainfo
                        MediaFileUtils mediaFileUtils = new MediaFileUtils(connection);
                        mediaFileUtils.purgePage(Long.parseLong(entityUpdate.getEntityId().getId().substring(1)));
                    }
                }
            }
        } catch (MediaWikiApiErrorException e) {
            if ("badtags".equals(e.getErrorCode()) && currentTag != null) {
                // if we tried editing with a tag that does not exist, clear the tag and try again
                currentTag = null;
                return performEdit();
            } else {
                batchCursor++;
                if ("failed-save".equals(e.getErrorCode())) {
                    // special case for the failed-save error which isn't very informative.
                    // We look for a better error message.
                    for (MediaWikiErrorMessage detailedMessage : e.getDetailedMessages()) {
                        if (!"wikibase-api-failed-save".equals(detailedMessage.getName())) {
                            return new EditResult(update.getContributingRowIds(), detailedMessage.getName(),
                                    detailedMessage.getHTMLText(), oldRevisionId, OptionalLong.empty(), null);
                        }
                    }
                }
                return new EditResult(update.getContributingRowIds(), e.getErrorCode(), e.getErrorMessage(), oldRevisionId,
                        OptionalLong.empty(), null);
            }
        } catch (IOException e) {
            batchCursor++;
            return new EditResult(update.getContributingRowIds(), "network-error", e.getMessage(), oldRevisionId, lastRevisionId,
                    newEntityUrl);
        }

        batchCursor++;
        return new EditResult(update.getContributingRowIds(), null, null, oldRevisionId, lastRevisionId, newEntityUrl);
    }

    public static class EditResult {

        private final Set<Integer> correspondingRowIds;
        private final String errorCode;
        private final String errorMessage;
        private final long baseRevisionId;
        private final OptionalLong lastRevisionId;
        private final String newEntityUrl;

        public EditResult(Set<Integer> correspondingRowIds,
                String errorCode, String errorMessage,
                long baseRevisionId,
                OptionalLong lastRevisionId,
                String newEntityUrl) {
            this.correspondingRowIds = correspondingRowIds;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.baseRevisionId = baseRevisionId;
            this.lastRevisionId = lastRevisionId;
            this.newEntityUrl = newEntityUrl;
        }

        public Set<Integer> getCorrespondingRowIds() {
            return correspondingRowIds;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getBaseRevisionId() {
            return baseRevisionId;
        }

        public OptionalLong getLastRevisionId() {
            return lastRevisionId;
        }

        public String getNewEntityUrl() {
            return newEntityUrl;
        }
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
