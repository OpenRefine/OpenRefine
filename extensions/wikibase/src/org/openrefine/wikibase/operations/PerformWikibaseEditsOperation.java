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
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import com.google.refine.RefineServlet;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ColumnAdditionChange;
import com.google.refine.model.changes.MassCellChange;
import com.google.refine.model.changes.MassChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

import org.openrefine.wikibase.commands.ConnectionManager;
import org.openrefine.wikibase.editing.EditBatchProcessor;
import org.openrefine.wikibase.editing.EditBatchProcessor.EditResult;
import org.openrefine.wikibase.editing.EditResultsFormatter;
import org.openrefine.wikibase.editing.NewEntityLibrary;
import org.openrefine.wikibase.manifests.Manifest;
import org.openrefine.wikibase.schema.WikibaseSchema;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.EntityEdit;

public class PerformWikibaseEditsOperation extends EngineDependentOperation {

    static final Logger logger = LoggerFactory.getLogger(PerformWikibaseEditsOperation.class);

    // only used for backwards compatibility, these things are configurable through
    // the manifest now.
    static final private String WIKIDATA_EDITGROUPS_URL_SCHEMA = "([[:toollabs:editgroups/b/OR/${batch_id}|details]])";

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("maxlag")
    private int maxlag;

    @JsonProperty("editGroupsUrlSchema")
    private String editGroupsUrlSchema;

    @JsonProperty("maxEditsPerMinute")
    private int maxEditsPerMinute;

    @JsonProperty("tag")
    private String tagTemplate;

    @JsonInclude(Include.NON_NULL)
    @JsonProperty("resultsColumnName")
    private String resultsColumnName;

    @JsonCreator
    public PerformWikibaseEditsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("summary") String summary,
            @JsonProperty("maxlag") Integer maxlag,
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
        this.maxEditsPerMinute = maxEditsPerMinute == null ? Manifest.DEFAULT_MAX_EDITS_PER_MINUTE : maxEditsPerMinute;
        this.tagTemplate = tag == null ? Manifest.DEFAULT_TAG_TEMPLATE : tag;
        // a fallback to Wikidata for backwards compatibility is done later on
        this.editGroupsUrlSchema = editGroupsUrlSchema;
        // if null, no column is created for error reporting
        this.resultsColumnName = resultsColumnName;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Perform Wikibase edits";
    }

    @Override
    public Optional<Set<String>> getColumnDependenciesWithoutEngine() {
        return Optional.of(Set.of());
    }

    @Override
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(ColumnsDiff.builder().addColumn(resultsColumnName, null).build());
    }

    @Override
    public PerformWikibaseEditsOperation renameColumns(Map<String, String> newColumnNames) {
        return new PerformWikibaseEditsOperation(
                getEngineConfig().renameColumnDependencies(newColumnNames),
                summary,
                maxlag,
                editGroupsUrlSchema,
                maxEditsPerMinute,
                tagTemplate,
                newColumnNames.getOrDefault(resultsColumnName, resultsColumnName));
    }

    @Override
    public Process createProcess(Project project, Properties options)
            throws Exception {
        return new PerformEditsProcess(
                project,
                createEngine(project),
                getBriefDescription(project),
                editGroupsUrlSchema,
                summary);
    }

    /**
     * @return the list of tags which we should attempt to add to our edits. The first existing tag in the list will be
     *         used. If none of the returned tags exist, editing will happen without any tag.
     */
    @JsonIgnore
    protected List<String> getTagCandidates(String refineVersion) {
        List<String> results = new LinkedList<>();

        if (tagTemplate.contains("${version}")) {
            Pattern pattern = Pattern.compile("^(\\d+\\.\\d+).*$");
            Matcher matcher = pattern.matcher(refineVersion);
            if (matcher.matches()) {
                results.add(tagTemplate.replace("${version}", matcher.group(1)));
            }

            // if the tag template includes the version, also add a version-independent tag as fallback.
            // for instance, if the tag template is `openrefine-${version}`, also try adding the tag `openrefine`
            // in case the version-specific tag isn't available.
            // See https://github.com/OpenRefine/OpenRefine/issues/6551
            if (tagTemplate.endsWith("-${version}")) {
                results.add(tagTemplate.substring(0, tagTemplate.length() - "-${version}".length()));
            }

        } else {
            results.add(tagTemplate);
        }

        return results;
    }

    static public class PerformWikibaseEditsChange implements Change {

        private NewEntityLibrary newEntityLibrary;

        public PerformWikibaseEditsChange(NewEntityLibrary library) {
            newEntityLibrary = library;
        }

        @Override
        public void apply(Project project) {
            // we don't re-run changes on Wikibase
            newEntityLibrary.updateReconciledCells(project, false);
        }

        @Override
        public void revert(Project project) {
            // this does not do anything on Wikibase side -
            // (we don't revert changes on Wikibase either)
            newEntityLibrary.updateReconciledCells(project, true);
        }

        @Override
        public void save(Writer writer, Properties options)
                throws IOException {
            if (newEntityLibrary != null) {
                writer.write("newItems=");
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                writer.write(mapper.writeValueAsString(newEntityLibrary) + "\n");
            }
            writer.write("/ec/\n"); // end of change
        }

        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            NewEntityLibrary library = new NewEntityLibrary();
            String line = null;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);

                if ("newItems".equals(field)) {
                    ObjectMapper mapper = new ObjectMapper();
                    library = mapper.readValue(value, NewEntityLibrary.class);
                }
            }
            return new PerformWikibaseEditsChange(library);
        }

    }

    public class PerformEditsProcess extends LongRunningProcess implements Runnable {

        protected Project _project;
        protected Engine _engine;
        protected WikibaseSchema _schema;
        protected String _editGroupsUrlSchema;
        protected String _summary;
        protected final long _historyEntryID;
        protected final List<JsonNode> onDone = new ArrayList<>();

        protected PerformEditsProcess(Project project, Engine engine, String description, String editGroupsUrlSchema, String summary) {
            super(description);
            this._project = project;
            this._engine = engine;
            this._schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            this._summary = summary;
            this._historyEntryID = HistoryEntry.allocateID();
            if (editGroupsUrlSchema == null &&
                    ApiConnection.URL_WIKIDATA_API.equals(_schema.getMediaWikiApiEndpoint())) {
                // For backward compatibility, if no editGroups schema is provided
                // and we edit Wikidata, then add Wikidata's editGroups schema
                editGroupsUrlSchema = WIKIDATA_EDITGROUPS_URL_SCHEMA;
            }
            this._editGroupsUrlSchema = editGroupsUrlSchema;

            // validate the schema
            ValidationState validation = new ValidationState(_project.columnModel);
            _schema.validate(validation);
            if (!validation.getValidationErrors().isEmpty()) {
                throw new IllegalStateException("Schema is incomplete");
            }

            // add error reporting facet
            if (resultsColumnName != null) {
                JsonNode createFacetAction;
                try {
                    createFacetAction = ParsingUtilities.mapper.readTree("{\n" +
                            "  \"action\" : \"createFacet\",\n" +
                            "  \"facetConfig\" : {\n" +
                            "  \"expression\" : \"grel:if(isError(value), 'failed edit', if(isBlank(value), 'no edit', 'successful edit'))\"\n"
                            +
                            "    },\n" +
                            "    \"facetType\" : \"list\"\n" +
                            " }");
                    ObjectNode facetConfig = (ObjectNode) createFacetAction.get("facetConfig");
                    facetConfig.put("columnName", resultsColumnName);
                    facetConfig.put("name", resultsColumnName);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(e);
                }
                onDone.add(createFacetAction);

            }

        }

        @Override
        public void run() {

            WebResourceFetcherImpl.setUserAgent("OpenRefine Wikidata extension");
            ConnectionManager manager = ConnectionManager.getInstance();
            String mediaWikiApiEndpoint = _schema.getMediaWikiApiEndpoint();
            if (!manager.isLoggedIn(mediaWikiApiEndpoint)) {
                return;
            }
            ApiConnection connection = manager.getConnection(mediaWikiApiEndpoint);
            WikibaseDataFetcher fetcher = new WikibaseDataFetcher(connection, _schema.getSiteIri());
            WikibaseDataEditor editor = new WikibaseDataEditor(connection, _schema.getSiteIri());

            String summary;
            if (StringUtils.isBlank(_editGroupsUrlSchema)) {
                summary = _summary;
            } else {
                // Generate batch id
                String batchId = Long.toHexString((new Random()).nextLong()).substring(0, 11);
                // The following replacement is a fix for: https://github.com/Wikidata/editgroups/issues/4
                // Because commas and colons are used by Wikibase to separate the auto-generated summaries
                // from the user-supplied ones, we replace these separators by similar unicode characters to
                // make sure they can be told apart.
                String summaryWithoutCommas = _summary.replaceAll(", ", "ꓹ ").replaceAll(": ", "։ ");
                summary = summaryWithoutCommas + " " + _editGroupsUrlSchema.replace("${batch_id}", batchId);
            }

            // Evaluate the schema
            List<EntityEdit> entityDocuments = _schema.evaluate(_project, _engine);

            // Prepare the edits
            NewEntityLibrary newEntityLibrary = new NewEntityLibrary();
            EditBatchProcessor processor = new EditBatchProcessor(fetcher, editor, connection, entityDocuments, newEntityLibrary, summary,
                    maxlag, getTagCandidates(RefineServlet.VERSION), 50, maxEditsPerMinute);
            EditResultsFormatter resultsFormatter = new EditResultsFormatter(mediaWikiApiEndpoint);

            // Perform edits
            logger.info("Performing edits");
            while (processor.remainingEdits() > 0) {
                try {
                    EditResult result = processor.performEdit();
                    resultsFormatter.add(result);
                } catch (InterruptedException e) {
                    _canceled = true;
                }
                _progress = processor.progress();
                if (_canceled) {
                    break;
                }
            }

            _progress = 100;

            Change errorReportingChange = null;
            logger.info("Storing editing results in: " + resultsColumnName);
            if (resultsColumnName != null) {
                Column resultsColumn = _project.columnModel.getColumnByName(resultsColumnName);
                int resultsCellIndex = resultsColumn != null ? resultsColumn.getCellIndex() : -1;

                List<CellAtRow> cells = resultsFormatter.toCells();

                if (resultsCellIndex != -1) {
                    // column already exists, we overwrite cells where we made edits
                    CellChange[] cellChanges = new CellChange[cells.size()];
                    int i = 0;
                    for (CellAtRow errorCell : cells) {
                        int rowId = errorCell.row;
                        Cell oldCell = _project.rows.get(rowId).getCell(resultsCellIndex);
                        cellChanges[i] = new CellChange(rowId, resultsCellIndex,
                                oldCell, errorCell.cell);
                        i++;
                    }
                    errorReportingChange = new MassCellChange(cellChanges, resultsColumnName, false);
                } else {
                    // column does not exist yet, let's create it.
                    errorReportingChange = new ColumnAdditionChange(resultsColumnName, _project.columnModel.columns.size(), cells);
                }
            }

            if (!_canceled) {
                Change reconUpdateChange = new PerformWikibaseEditsChange(newEntityLibrary);
                Change fullChange = errorReportingChange == null ? reconUpdateChange
                        : new MassChange(Arrays.asList(reconUpdateChange, errorReportingChange), false);

                HistoryEntry historyEntry = new HistoryEntry(_historyEntryID, _project, _description,
                        PerformWikibaseEditsOperation.this, fullChange);

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @JsonProperty("onDone")
        public List<JsonNode> onDoneActions() {
            return onDone;
        }
    }
}
