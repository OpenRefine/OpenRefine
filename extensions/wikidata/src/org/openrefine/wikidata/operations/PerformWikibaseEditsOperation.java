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
package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.openrefine.wikidata.commands.ConnectionManager;
import org.openrefine.wikidata.editing.EditBatchProcessor;
import org.openrefine.wikidata.editing.NewItemLibrary;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.RefineServlet;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.Pool;

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

    @JsonCreator
    public PerformWikibaseEditsOperation(
    		@JsonProperty("engineConfig")
    		EngineConfig engineConfig,
    		@JsonProperty("summary")
    		String summary,
            @JsonProperty("maxlag")
            Integer maxlag,
            @JsonProperty("editGroupsUrlSchema")
            String editGroupsUrlSchema) {
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
        // a fallback to Wikidata for backwards compatibility is done later on
        this.editGroupsUrlSchema = editGroupsUrlSchema;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Perform Wikibase edits";
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

    static public class PerformWikibaseEditsChange implements Change {

        private NewItemLibrary newItemLibrary;

        public PerformWikibaseEditsChange(NewItemLibrary library) {
            newItemLibrary = library;
        }

        @Override
        public void apply(Project project) {
            // we don't re-run changes on Wikidata
            newItemLibrary.updateReconciledCells(project, false);
        }

        @Override
        public void revert(Project project) {
            // this does not do anything on Wikibase side -
            // (we don't revert changes on Wikidata either)
            newItemLibrary.updateReconciledCells(project, true);
        }

        @Override
        public void save(Writer writer, Properties options)
                throws IOException {
            if (newItemLibrary != null) {
                writer.write("newItems=");
                ObjectMapper mapper = new ObjectMapper();
                writer.write(mapper.writeValueAsString(newItemLibrary) + "\n");
            }
            writer.write("/ec/\n"); // end of change
        }

        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            NewItemLibrary library = new NewItemLibrary();
            String line = null;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);

                if ("newItems".equals(field)) {
                    ObjectMapper mapper = new ObjectMapper();
                    library = mapper.readValue(value, NewItemLibrary.class);
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
        protected List<String> _tags;
        protected final long _historyEntryID;

        protected PerformEditsProcess(Project project, Engine engine, String description, String editGroupsUrlSchema, String summary) {
            super(description);
            this._project = project;
            this._engine = engine;
            this._schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            this._summary = summary;
            String tag = "openrefine";
            Pattern pattern = Pattern.compile("^(\\d+\\.\\d+).*$");
            Matcher matcher = pattern.matcher(RefineServlet.VERSION);
            if (matcher.matches()) {
                tag += "-"+matcher.group(1);
            }
            this._tags = Arrays.asList(tag);
            this._historyEntryID = HistoryEntry.allocateID();
            if (editGroupsUrlSchema == null &&
                    ApiConnection.URL_WIKIDATA_API.equals(_schema.getMediaWikiApiEndpoint())) {
                // For backward compatibility, if no editGroups schema is provided
                // and we edit Wikidata, then add Wikidata's editGroups schema
                editGroupsUrlSchema = WIKIDATA_EDITGROUPS_URL_SCHEMA;
            }
            this._editGroupsUrlSchema = editGroupsUrlSchema;
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

            WikibaseDataFetcher wbdf = new WikibaseDataFetcher(connection, _schema.getSiteIri());
            WikibaseDataEditor wbde = new WikibaseDataEditor(connection, _schema.getSiteIri());

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
                String summaryWithoutCommas = _summary.replaceAll(", ","ꓹ ").replaceAll(": ","։ ");
                summary = summaryWithoutCommas + " " + _editGroupsUrlSchema.replace("${batch_id}", batchId);
            }

            // Evaluate the schema
            List<TermedStatementEntityUpdate> itemDocuments = _schema.evaluate(_project, _engine);

            // Prepare the edits
            NewItemLibrary newItemLibrary = new NewItemLibrary();
            EditBatchProcessor processor = new EditBatchProcessor(wbdf, wbde, itemDocuments, newItemLibrary, summary,
                    maxlag, _tags, 50);

            // Perform edits
            logger.info("Performing edits");
            while (processor.remainingEdits() > 0) {
                try {
                    processor.performEdit();
                } catch (InterruptedException e) {
                    _canceled = true;
                }
                _progress = processor.progress();
                if (_canceled) {
                    break;
                }
            }

            _progress = 100;

            if (!_canceled) {
                Change change = new PerformWikibaseEditsChange(newItemLibrary);

                HistoryEntry historyEntry = new HistoryEntry(_historyEntryID, _project, _description,
                        PerformWikibaseEditsOperation.this, change);

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }
    }
}
