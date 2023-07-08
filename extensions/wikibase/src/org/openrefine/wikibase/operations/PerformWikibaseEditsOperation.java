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

package org.openrefine.wikibase.operations;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.openrefine.RefineServlet;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.expr.EvalError;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.ModelException;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowChangeDataJoiner;
import org.openrefine.model.changes.RowChangeDataProducer;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.exceptions.ChangeDataFetchingException;
import org.openrefine.operations.exceptions.IOOperationException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.wikibase.commands.ConnectionManager;
import org.openrefine.wikibase.editing.EditBatchProcessor;
import org.openrefine.wikibase.editing.NewEntityLibrary;
import org.openrefine.wikibase.manifests.Manifest;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.EntityEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

public class PerformWikibaseEditsOperation extends EngineDependentOperation {

    static final Logger logger = LoggerFactory.getLogger(PerformWikibaseEditsOperation.class);

    static final String description = "Perform Wikibase edits";
    static final protected String changeDataId = "newEntities";

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("maxlag")
    private int maxlag;

    @JsonProperty("batchSize")
    private int batchSize;

    @JsonProperty("editGroupsUrlSchema")
    private String editGroupsUrlSchema;

    @JsonProperty("maxEditsPerMinute")
    private int maxEditsPerMinute;

    @JsonProperty("tag")
    private String tagTemplate;

    @JsonProperty("resultsColumnName")
    private String resultsColumnName;

    @JsonCreator
    public PerformWikibaseEditsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("summary") String summary,
            @JsonProperty("maxlag") Integer maxlag,
            @JsonProperty("batchSize") Integer batchSize,
            @JsonProperty("editGroupsUrlSchema") String editGroupsUrlSchema,
            @JsonProperty("maxEditsPerMinute") Integer maxEditsPerMinute,
            @JsonProperty("tag") String tag,
            @JsonProperty("resultsColumnName") String resultsColumnName) {
        super(engineConfig);
        Validate.notNull(summary, "An edit summary must be provided.");
        Validate.notEmpty(summary, "An edit summary must be provided.");
        this.summary = summary;
        if (maxlag == null) {
            // For backward compatibility, if the maxlag parameter is not included
            // in the serialized JSON text, set it to 5.
            maxlag = 5;
        }
        this.maxlag = maxlag;
        if (batchSize == null) {
            batchSize = 50;
        }
        this.batchSize = batchSize;
        this.maxEditsPerMinute = maxEditsPerMinute == null ? Manifest.DEFAULT_MAX_EDITS_PER_MINUTE : maxEditsPerMinute;
        this.tagTemplate = tag == null ? Manifest.DEFAULT_TAG_TEMPLATE : tag;
        // a fallback to Wikidata for backwards compatibility is done later on
        this.editGroupsUrlSchema = editGroupsUrlSchema;
        // if null, no column is created for error reporting
        this.resultsColumnName = resultsColumnName;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        String tag = tagTemplate;
        if (tag.contains("${version}")) {
            Pattern pattern = Pattern.compile("^(\\d+\\.\\d+).*$");
            Matcher matcher = pattern.matcher(RefineServlet.VERSION);
            if (matcher.matches()) {
                tag = tag.replace("${version}", matcher.group(1));
            }
        }
        List<String> tags = Collections.singletonList(tag);
        Engine engine = new Engine(projectState, _engineConfig, 1234L);

        // Validate schema
        WikibaseSchema schema = (WikibaseSchema) projectState.getOverlayModels().get("wikibaseSchema");
        ValidationState validation = new ValidationState(projectState.getColumnModel());
        schema.validate(validation);
        if (!validation.getValidationErrors().isEmpty()) {
            throw new IllegalStateException("Schema is incomplete");
        }

        // TODO if we want to preserve the batch id after the operation is restarted (for instance after a crash),
        // we could store this id as a separate change data.
        String editGroupId = Long.toHexString((new Random()).nextLong()).substring(0, 11);

        ChangeData<RowEditingResults> changeData = null;
        try {
            changeData = context.getChangeData(changeDataId, new RowNewReconUpdateSerializer(), existingChangeData -> {
                // TODO resume from existing change data
                NewEntityLibrary library = new NewEntityLibrary();

                // Get Wikibase connection
                ConnectionManager manager = ConnectionManager.getInstance();
                String mediaWikiApiEndpoint = schema.getMediaWikiApiEndpoint();
                if (!manager.isLoggedIn(mediaWikiApiEndpoint)) {
                    throw new ChangeDataFetchingException("You need to be logged in to Wikibase via OpenRefine to perform edits. " +
                            "This process can be restarted once you are logged in.", true);
                }
                ApiConnection connection = manager.getConnection(mediaWikiApiEndpoint);

                // Prepare edit summary
                String summary;
                if (StringUtils.isBlank(editGroupsUrlSchema)) {
                    summary = this.summary;
                } else {
                    // The following replacement is a fix for: https://github.com/Wikidata/editgroups/issues/4
                    // Because commas and colons are used by Wikibase to separate the auto-generated summaries
                    // from the user-supplied ones, we replace these separators by similar unicode characters to
                    // make sure they can be told apart.
                    String summaryWithoutCommas = this.summary.replaceAll(", ", "ꓹ ").replaceAll(": ", "։ ");
                    summary = summaryWithoutCommas + " " + this.editGroupsUrlSchema.replace("${batch_id}", editGroupId);
                }

                RowEditingResultsProducer changeProducer = new RowEditingResultsProducer(
                        connection,
                        schema,
                        projectState.getColumnModel(),
                        summary,
                        batchSize,
                        maxlag,
                        tags,
                        maxEditsPerMinute,
                        library);
                ChangeData<RowEditingResults> newChangeData = projectState.mapRows(engine.combinedRowFilters(), changeProducer,
                        existingChangeData);
                return newChangeData;
            });
        } catch (IOException e) {
            throw new IOOperationException(e);
        }

        ColumnModel columnModel = projectState.getColumnModel();
        int resultsColumnIndex = -1;
        boolean insert = false;
        ColumnModel newColumnModel = columnModel;
        List<FacetConfig> createdFacets = new ArrayList<>();
        if (resultsColumnName != null) {
            resultsColumnIndex = columnModel.getColumnIndexByName(resultsColumnName);
            if (resultsColumnIndex == -1) {
                insert = true;
                resultsColumnIndex = columnModel.getColumns().size();
                try {
                    newColumnModel = columnModel.insertColumn(resultsColumnIndex, new ColumnMetadata(resultsColumnName));
                } catch (ModelException e) {
                    // cannot happen: we checked earlier that the column does not exist.
                }
            }
            // TODO localize
            createdFacets.add(new ListFacet.ListFacetConfig(
                    "Wikibase editing status",
                    "grel:if(isError(value), 'failed edit', 'successful edit')",
                    resultsColumnName));
        }
        String baseUrl = schema.getMediaWikiApiEndpoint();
        baseUrl = baseUrl.substring(0, baseUrl.length() - "w/api.php".length());
        NewReconRowJoiner joiner = new NewReconRowJoiner(resultsColumnIndex, insert, baseUrl);
        Grid joined = projectState.join(changeData, joiner, newColumnModel);
        return new ChangeResult(joined, GridPreservation.PRESERVES_RECORDS, createdFacets, null);
    }

    @Override
    public String getDescription() {
        return description;
    }

    protected static class RowEditingResults implements Serializable {

        private final Map<Long, String> newEntities;
        private final List<String> errors;
        private final List<Long> revisionIds;

        protected RowEditingResults(
                @JsonProperty("newEntities") Map<Long, String> newEntities,
                @JsonProperty("errors") List<String> errors,
                @JsonProperty("revisionIds") List<Long> revisionIds) {
            this.newEntities = newEntities;
            this.errors = errors;
            this.revisionIds = revisionIds;
        }

        @JsonProperty("newEntities")
        public Map<Long, String> getNewEntities() {
            return newEntities;
        }

        @JsonProperty("errors")
        public List<String> getErrors() {
            return errors;
        }

        @JsonProperty("revisionIds")
        public List<Long> getRevisionIds() {
            return revisionIds;
        }
    }

    protected static class RowEditingResultsProducer implements RowChangeDataProducer<RowEditingResults> {

        protected final ApiConnection connection;
        protected final WikibaseSchema schema;
        protected final ColumnModel columnModel;
        protected final String summary;
        protected final int batchSize;
        protected final int maxLag;
        protected final int maxEditsPerMinute;
        protected final List<String> tags;
        protected final NewEntityLibrary library;
        protected final WikibaseDataFetcher fetcher;
        protected final WikibaseDataEditor editor;

        RowEditingResultsProducer(ApiConnection connection, WikibaseSchema schema, ColumnModel columnModel, String summary, int batchSize,
                int maxLag, List<String> tags, int maxEditsPerMinute, NewEntityLibrary library) {
            this.schema = schema;
            this.columnModel = columnModel;
            this.summary = summary;
            this.connection = connection;
            this.batchSize = batchSize;
            this.maxLag = maxLag;
            this.maxEditsPerMinute = maxEditsPerMinute;
            this.tags = tags;
            this.library = library;
            fetcher = new WikibaseDataFetcher(connection, schema.getSiteIri());
            editor = new WikibaseDataEditor(connection, schema.getSiteIri());
        }

        @Override
        public RowEditingResults call(long rowId, Row row) {
            return callRowBatch(Collections.singletonList(new IndexedRow(rowId, row))).get(0);
        }

        @Override
        public List<RowEditingResults> callRowBatch(List<IndexedRow> rows) {
            List<EntityEdit> edits = schema.evaluate(columnModel, rows, null);
            EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, edits, library, summary,
                    maxLag, tags, batchSize, maxEditsPerMinute);
            Map<Long, List<String>> rowEditingErrors = new HashMap<>();
            Map<Long, List<Long>> rowRevisionIds = new HashMap<>();
            while (processor.remainingEdits() > 0) {
                try {
                    EditBatchProcessor.EditResult editResult = processor.performEdit();
                    long firstLine = editResult.getCorrespondingRowIds().stream().min(Comparator.naturalOrder()).get();
                    if (editResult.getErrorCode() != null || editResult.getErrorMessage() != null) {
                        List<String> logLine = rowEditingErrors.get(firstLine);
                        if (logLine == null) {
                            logLine = new ArrayList<>();
                            rowEditingErrors.put(firstLine, logLine);
                        }
                        logLine.add(String.format("[%s] %s", editResult.getErrorCode(), editResult.getErrorMessage()));
                    } else if (editResult.getLastRevisionId().isPresent()) {
                        List<Long> revisions = rowRevisionIds.get(firstLine);
                        if (revisions == null) {
                            revisions = new ArrayList<>();
                            rowRevisionIds.put(firstLine, revisions);
                        }
                        revisions.add(editResult.getLastRevisionId().getAsLong());
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
            List<RowEditingResults> rowEditingResults = new ArrayList<>();
            for (IndexedRow indexedRow : rows) {
                Map<Long, String> newEntityIds = new HashMap<>();
                indexedRow.getRow().getCells().stream()
                        .forEach(cell -> {
                            if (cell != null && cell.recon != null && Judgment.New.equals(cell.recon.judgment)) {
                                long id = cell.recon.id;
                                String entityId = library.getId(id);
                                if (entityId != null) {
                                    newEntityIds.put(id, entityId);
                                }
                            }
                        });
                List<String> errors = rowEditingErrors.getOrDefault(indexedRow.getIndex(), Collections.emptyList());
                List<Long> revisionIds = rowRevisionIds.getOrDefault(indexedRow.getIndex(), Collections.emptyList());
                rowEditingResults.add(new RowEditingResults(newEntityIds, errors, revisionIds));
            }
            return rowEditingResults;
        }

        @Override
        public int getBatchSize() {
            return batchSize;
        }

        @Override
        public int getMaxConcurrency() {
            return 1;
        }
    }

    protected static class NewReconRowJoiner implements RowChangeDataJoiner<RowEditingResults> {

        private static final long serialVersionUID = -1042195464154951531L;

        private final int resultsColumnIndex;
        private final boolean insert;
        private final String wikiBaseUrl;

        /**
         * @param resultsColumnIndex
         *            the index of the column where to insert editing results
         * @param insert
         *            whether the column should be created or if it is already existing
         * @param wikiBaseUrl
         *            the baseUrl of the wiki, such as "https://www.wikidata.org/"
         */
        public NewReconRowJoiner(int resultsColumnIndex, boolean insert, String wikiBaseUrl) {
            this.resultsColumnIndex = resultsColumnIndex;
            this.insert = insert;
            this.wikiBaseUrl = wikiBaseUrl;
        }

        @Override
        public Row call(Row row, IndexedData<RowEditingResults> indexedData) {
            RowEditingResults changeData = indexedData.getData();
            if (changeData == null && !indexedData.isPending()) {
                return row;
            }
            List<Cell> newCells = row.getCells().stream()
                    .map(cell -> {
                        if (cell != null && cell.recon != null && Judgment.New.equals(cell.recon.judgment)) {
                            long id = cell.recon.id;
                            if (indexedData.isPending()) {
                                return new Cell(cell.value, cell.recon, true);
                            }
                            String entityId = changeData.getNewEntities().get(id);
                            if (entityId == null) {
                                return cell;
                            } else {
                                Recon recon = cell.recon;
                                ReconCandidate newMatch = new ReconCandidate(entityId, cell.value.toString(),
                                        new String[0], 100);
                                recon = recon
                                        .withJudgment(Recon.Judgment.Matched)
                                        .withMatch(newMatch)
                                        .withCandidate(newMatch);
                                return new Cell(cell.value, recon);
                            }
                        } else {
                            return cell;
                        }
                    })
                    .collect(Collectors.toList());

            Row updatedRow = new Row(newCells, row.flagged, row.starred);
            if (resultsColumnIndex != -1) {
                Cell resultsCell = null;
                if (indexedData.isPending()) {
                    resultsCell = Cell.PENDING_NULL;
                } else if (!changeData.getErrors().isEmpty()) {
                    resultsCell = new Cell(new EvalError(String.join("; ", changeData.getErrors())), null);
                } else if (!changeData.getRevisionIds().isEmpty()) {
                    resultsCell = new Cell(revisionIdsToUrl(changeData.getRevisionIds()), null);
                }
                if (insert) {
                    updatedRow = updatedRow.insertCell(resultsColumnIndex, resultsCell);
                } else {
                    updatedRow = updatedRow.withCell(resultsColumnIndex, resultsCell);
                }
            }
            return updatedRow;
        }

        String revisionIdsToUrl(List<Long> revisionIds) {
            List<String> urls = revisionIds.stream()
                    .map(r -> wikiBaseUrl + "w/index.php?diff=prev&oldid=" + r)
                    .collect(Collectors.toList());
            return String.join(" ", urls);
        }

        @Override
        public boolean preservesRecordStructure() {
            return true; // existing blank cells are left blank
        }

    }

    protected static class RowNewReconUpdateSerializer implements ChangeDataSerializer<RowEditingResults> {

        private static final long serialVersionUID = -165445357950934740L;

        @Override
        public String serialize(RowEditingResults changeDataItem) {
            try {
                return ParsingUtilities.mapper.writeValueAsString(changeDataItem);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public RowEditingResults deserialize(String serialized) throws IOException {
            return ParsingUtilities.mapper.readValue(serialized, RowEditingResults.class);
        }

    }

    protected static class EditGroupIdSerializer implements ChangeDataSerializer<String> {

        private static final long serialVersionUID = -8739862435042915677L;

        @Override
        public String serialize(String changeDataItem) {
            return changeDataItem;
        }

        @Override
        public String deserialize(String serialized) throws IOException {
            return serialized;
        }

    }
}
