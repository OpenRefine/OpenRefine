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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.apache.log4j.spi.LoggerRepository;
import org.openrefine.wikidata.editing.ConnectionManager;
import org.openrefine.wikidata.editing.EditBatchProcessor;
import org.openrefine.wikidata.editing.NewItemLibrary;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.updates.ItemUpdate;
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

    @JsonProperty("summary")
    private String summary;

    @JsonCreator
    public PerformWikibaseEditsOperation(
    		@JsonProperty("engineConfig")
    		EngineConfig engineConfig,
    		@JsonProperty("summary")
    		String summary) {
        super(engineConfig);
        Validate.notNull(summary, "An edit summary must be provided.");
        Validate.notEmpty(summary, "An edit summary must be provided.");
        this.summary = summary;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Perform Wikibase edits";
    }

    @Override
    public Process createProcess(Project project, Properties options)
            throws Exception {
        return new PerformEditsProcess(project, createEngine(project), getBriefDescription(project), summary);
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
        protected String _summary;
        protected List<String> _tags;
        protected final long _historyEntryID;

        protected PerformEditsProcess(Project project, Engine engine, String description, String summary) {
            super(description);
            this._project = project;
            this._engine = engine;
            this._schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            this._summary = summary;
            // TODO this is one of the attributes that should be configured on a per-wiki basis
            // TODO enable this tag once 3.3 final is released and create 3.4 tag without AbuseFilter
            String tag = "openrefine";
            Pattern pattern = Pattern.compile("^(\\d+\\.\\d+).*$");
            Matcher matcher = pattern.matcher(RefineServlet.VERSION);
            if (matcher.matches()) {
                tag += "-"+matcher.group(1);
            }
            this._tags = Collections.emptyList(); // TODO Arrays.asList(tag);
            this._historyEntryID = HistoryEntry.allocateID();
        }

        @Override
        public void run() {

            WebResourceFetcherImpl.setUserAgent("OpenRefine Wikidata extension");
            ConnectionManager manager = ConnectionManager.getInstance();
            if (!manager.isLoggedIn()) {
                return;
            }
            ApiConnection connection = manager.getConnection();

            WikibaseDataFetcher wbdf = new WikibaseDataFetcher(connection, _schema.getBaseIri());
            WikibaseDataEditor wbde = new WikibaseDataEditor(connection, _schema.getBaseIri());
            
            // Generate batch token
            long token = (new Random()).nextLong();
            // The following replacement is a fix for: https://github.com/Wikidata/editgroups/issues/4
            // Because commas and colons are used by Wikibase to separate the auto-generated summaries
            // from the user-supplied ones, we replace these separators by similar unicode characters to
            // make sure they can be told apart.
            String summaryWithoutCommas = _summary.replaceAll(", ","ꓹ ").replaceAll(": ","։ ");
            String summary = summaryWithoutCommas + String.format(" ([[:toollabs:editgroups/b/OR/%s|details]])",
                    (Long.toHexString(token).substring(0, 10)));

            // Evaluate the schema
            List<ItemUpdate> itemDocuments = _schema.evaluate(_project, _engine);

            // Prepare the edits
            NewItemLibrary newItemLibrary = new NewItemLibrary();
            EditBatchProcessor processor = new EditBatchProcessor(wbdf, wbde, itemDocuments, newItemLibrary, summary,
                    _tags, 50);

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
