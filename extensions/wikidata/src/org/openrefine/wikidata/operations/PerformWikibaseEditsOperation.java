package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import org.openrefine.wikidata.editing.ConnectionManager;
import org.openrefine.wikidata.editing.EditBatchProcessor;
import org.openrefine.wikidata.editing.NewItemLibrary;
import org.openrefine.wikidata.editing.ReconEntityRewriter;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.openrefine.wikidata.updates.scheduler.ImpossibleSchedulingException;
import org.openrefine.wikidata.updates.scheduler.UpdateScheduler;
import org.openrefine.wikidata.updates.scheduler.WikibaseAPIUpdateScheduler;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.openrefine.wikidata.schema.entityvalues.ReconEntityIdValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.DataObjectFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.TermStatementUpdate;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.refine.browsing.Engine;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.Pool;


public class PerformWikibaseEditsOperation extends EngineDependentOperation {
    static final Logger logger = LoggerFactory
            .getLogger(PerformWikibaseEditsOperation.class);
    
    private String summary;
    
    public PerformWikibaseEditsOperation(
            JSONObject engineConfig,
            String summary) {
        super(engineConfig);
        this.summary = summary;

        // getEngine(request, project);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj)
            throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        String summary = null;
        if (obj.has("summary")) {
            summary = obj.getString("summary");
        }
        return new PerformWikibaseEditsOperation(
                engineConfig,
                summary);
    }

    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("op");
        writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description");
        writer.value("Perform Wikibase edits");
        writer.key("summary");
        writer.value(summary);
        writer.key("engineConfig");
        writer.value(getEngineConfig());
        writer.endObject();
    }
    
    @Override
    protected String getBriefDescription(Project project) {
        return "Peform edits on Wikidata";
    }
    
    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        return new PerformEditsProcess(
            project, 
            createEngine(project),
            getBriefDescription(project),
            summary
        );
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
                writer.write(mapper.writeValueAsString(newItemLibrary)+"\n");
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
        protected final long _historyEntryID;

        protected PerformEditsProcess(Project project, 
            Engine engine, String description, String summary) {
            super(description);
            this._project = project;
            this._engine = engine;
            this._schema = (WikibaseSchema) project.overlayModels.get("wikibaseSchema");
            this._summary = summary;
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
            
            // Evaluate the schema
            List<ItemUpdate> itemDocuments = _schema.evaluate(_project, _engine);
            
            // Prepare the edits
            NewItemLibrary newItemLibrary = new NewItemLibrary();
            EditBatchProcessor processor = new EditBatchProcessor(wbdf,
                    wbde, itemDocuments, newItemLibrary, _summary, 50);
            
            // Perform edits
            logger.info("Performing edits");
            while(processor.remainingEdits() > 0) {
                try {
                    processor.performEdit();
                } catch(InterruptedException e) {
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
                
                HistoryEntry historyEntry = new HistoryEntry(
                    _historyEntryID,
                    _project, 
                    _description, 
                    PerformWikibaseEditsOperation.this, 
                    change
                );
                
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
