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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import org.openrefine.RefineServlet;
import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ParsingException;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.*;
import org.openrefine.model.changes.*;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconCandidate;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.wikibase.commands.ConnectionManager;
import org.openrefine.wikibase.editing.EditBatchProcessor;
import org.openrefine.wikibase.editing.NewEntityLibrary;
import org.openrefine.wikibase.manifests.Manifest;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.EntityEdit;

public class PerformWikibaseEditsOperation extends EngineDependentOperation {

    static final Logger logger = LoggerFactory.getLogger(PerformWikibaseEditsOperation.class);

    static final String description = "Perform Wikibase edits";
    static final protected String changeDataId = "newEntities";

    // only used for backwards compatibility, these things are configurable through
    // the manifest now.
    static final private String WIKIDATA_EDITGROUPS_URL_SCHEMA = "([[:toollabs:editgroups/b/OR/${batch_id}|details]])";

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

    @JsonCreator
    public PerformWikibaseEditsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("summary") String summary,
            @JsonProperty("maxlag") Integer maxlag,
            @JsonProperty("batchSize") Integer batchSize,
            @JsonProperty("editGroupsUrlSchema") String editGroupsUrlSchema,
            @JsonProperty("maxEditsPerMinute") Integer maxEditsPerMinute,
            @JsonProperty("tag") String tag) {
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
    }

    @Override
    public Change createChange() throws ParsingException {
        String editGroupId = Long.toHexString((new Random()).nextLong()).substring(0, 11);
        return new PerformWikibaseEditsChange(editGroupId, this);
    }

    @Override
    public String getDescription() {
        return description;
    }

    static public class PerformWikibaseEditsChange implements Change {

        private final String editGroupId;
        private final PerformWikibaseEditsOperation operation;

        @JsonCreator
        public PerformWikibaseEditsChange(
                @JsonProperty("editGroupId") String editGroupId,
                @JsonProperty("operation") PerformWikibaseEditsOperation operation) {
            this.editGroupId = editGroupId;
            this.operation = operation;
        }

        @Override
        public ChangeResult apply(Grid projectState, ChangeContext context) throws DoesNotApplyException {
            String tag = operation.tagTemplate;
            if (tag.contains("${version}")) {
                Pattern pattern = Pattern.compile("^(\\d+\\.\\d+).*$");
                Matcher matcher = pattern.matcher(RefineServlet.VERSION);
                if (matcher.matches()) {
                    tag = tag.replace("${version}", matcher.group(1));
                }
            }
            List<String> tags = Collections.singletonList(tag);
            Engine engine = new Engine(projectState, operation._engineConfig, 1234L);

            // Validate schema
            WikibaseSchema schema = (WikibaseSchema) projectState.getOverlayModels().get("wikibaseSchema");
            ValidationState validation = new ValidationState(projectState.getColumnModel());
            schema.validate(validation);
            if (!validation.getValidationErrors().isEmpty()) {
                throw new IllegalStateException("Schema is incomplete");
            }

            ChangeData<RowEditingResults> changeData = null;
            try {
                changeData = context.getChangeData(changeDataId, new RowNewReconUpdateSerializer(), existingChangeData -> {
                    // TODO resume from existing change data
                    NewEntityLibrary library = new NewEntityLibrary();

                    // Get Wikibase connection
                    ConnectionManager manager = ConnectionManager.getInstance();
                    String mediaWikiApiEndpoint = schema.getMediaWikiApiEndpoint();
                    if (!manager.isLoggedIn(mediaWikiApiEndpoint)) {
                        // TODO find a way to signal to the user that they should re-login
                        return existingChangeData.orElse(projectState.getRunner().changeDataFromList(Collections.emptyList()));
                    }
                    ApiConnection connection = manager.getConnection(mediaWikiApiEndpoint);

                    // Prepare edit summary
                    String summary;
                    if (StringUtils.isBlank(operation.editGroupsUrlSchema)) {
                        summary = operation.summary;
                    } else {
                        // The following replacement is a fix for: https://github.com/Wikidata/editgroups/issues/4
                        // Because commas and colons are used by Wikibase to separate the auto-generated summaries
                        // from the user-supplied ones, we replace these separators by similar unicode characters to
                        // make sure they can be told apart.
                        String summaryWithoutCommas = operation.summary.replaceAll(", ", "ꓹ ").replaceAll(": ", "։ ");
                        summary = summaryWithoutCommas + " " + operation.editGroupsUrlSchema.replace("${batch_id}", editGroupId);
                    }

                    RowEditingResultsProducer changeProducer = new RowEditingResultsProducer(
                            connection,
                            schema,
                            projectState.getColumnModel(),
                            summary,
                            operation.batchSize,
                            operation.maxlag,
                            tags,
                            operation.maxEditsPerMinute,
                            library);
                    ChangeData<RowEditingResults> newChangeData = projectState.mapRows(engine.combinedRowFilters(), changeProducer,
                            existingChangeData);
                    return newChangeData;
                });
            } catch (IOException e) {
                throw new DoesNotApplyException(String.format("Unable to retrieve change data '%s'", changeDataId));
            }
            NewReconRowJoiner joiner = new NewReconRowJoiner();
            Grid joined = projectState.join(changeData, joiner, projectState.getColumnModel());
            return new ChangeResult(joined, GridPreservation.PRESERVES_RECORDS, null);
        }

        @Override
        public boolean isImmediate() {
            return false;
        }

    }

    protected static class RowEditingResults implements Serializable {

        private final Map<Long, String> newEntities;
        private final List<String> errors;

        protected RowEditingResults(
                @JsonProperty("newEntities") Map<Long, String> newEntities,
                @JsonProperty("errors") List<String> errors) {
            this.newEntities = newEntities;
            this.errors = errors;
        }

        @JsonProperty("newEntities")
        public Map<Long, String> getNewEntities() {
            return newEntities;
        }

        @JsonProperty("errors")
        public List<String> getErrors() {
            return errors;
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
            while (processor.remainingEdits() > 0) {
                try {
                    EditBatchProcessor.EditResult editResult = processor.performEdit();
                    if (editResult.getErrorCode() != null || editResult.getErrorMessage() != null) {
                        long firstLine = editResult.getCorrespondingRowIds().stream().min(Comparator.naturalOrder()).get();
                        List<String> logLine = rowEditingErrors.get(firstLine);
                        if (logLine == null) {
                            logLine = new ArrayList<>();
                            rowEditingErrors.put(firstLine, logLine);
                        }
                        logLine.add(String.format("[%s] %s", editResult.getErrorCode(), editResult.getErrorMessage()));
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
                rowEditingResults.add(new RowEditingResults(newEntityIds, errors));
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
            return new Row(newCells, row.flagged, row.starred);
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
}
