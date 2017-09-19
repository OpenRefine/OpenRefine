package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import com.google.common.collect.Lists;

import org.openrefine.wikidata.editing.ConnectionManager;
import org.openrefine.wikidata.operations.SaveWikibaseSchemaOperation.WikibaseSchemaChange;
import org.openrefine.wikidata.schema.ItemUpdate;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.wikidata.wdtk.datamodel.helpers.EntityDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.StatementUpdate;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import com.google.refine.browsing.Engine;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.recon.ReconOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;


public class PerformWikibaseEditsOperation extends EngineDependentOperation {
    public enum DuplicateDetectionStrategy {
        PROPERTY, SNAK, SNAK_QUALIFIERS
    }
    
    public enum OnDuplicateAction {
        SKIP, MERGE
    }
    
    private DuplicateDetectionStrategy strategy;
    private OnDuplicateAction duplicateAction;
    
    public PerformWikibaseEditsOperation(
            JSONObject engineConfig,
            DuplicateDetectionStrategy strategy,
            OnDuplicateAction duplicateAction) {
        super(engineConfig);
        this.strategy = strategy;
        this.duplicateAction = duplicateAction;

        // getEngine(request, project);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj)
            throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        String strategy = obj.getString("duplicate_strategy");
        String action = obj.getString("duplicate_action");
        return new PerformWikibaseEditsOperation(
                engineConfig,
                DuplicateDetectionStrategy.valueOf(strategy),
                OnDuplicateAction.valueOf(action));
    }

    
    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("op");
        writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description");
        writer.value("Perform Wikibase edits");
        writer.key("duplicate_strategy");
        writer.value(strategy.name());
        writer.key("duplicate_action");
        writer.value(duplicateAction.name());
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
            "#openrefine"
        );
    }
    static public class PerformWikibaseEditsChange implements Change {

        @Override
        public void apply(Project project) {
            // this does not do anything to the project (we don't re-run changes on Wikidata)
        }

        @Override
        public void revert(Project project) {
            // this does not do anything (we don't revert changes on Wikidata either)
        }

        @Override
        public void save(Writer writer, Properties options)
                throws IOException {
            writer.write("/ec/\n"); // end of change
        }
        
        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            return new PerformWikibaseEditsChange();
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
            // TODO Auto-generated method stub
            
            WebResourceFetcherImpl.setUserAgent("OpenRefine Wikidata extension");
            ConnectionManager manager = ConnectionManager.getInstance();
            if (!manager.isLoggedIn()) {
                return;
            }
            ApiConnection connection = manager.getConnection();

            //WikibaseDataFetcher wbdf = new WikibaseDataFetcher(connection, schema.getBaseUri());
            WikibaseDataEditor wbde = new WikibaseDataEditor(connection, _schema.getBaseUri());
            //wbde.disableEditing();
            
            // Evaluate the schema
            List<ItemUpdate> itemDocuments = _schema.evaluate(_project, _engine);
            
            // Group statements by item
            Map<EntityIdValue, ItemUpdate> updates = new HashMap<EntityIdValue, ItemUpdate>();
            for(ItemUpdate update : itemDocuments) {
                if (update.isNull()) {
                    continue;
                }
                
                ItemIdValue qid = update.getItemId();
                if (updates.containsKey(qid)) {
                    ItemUpdate oldUpdate = updates.get(qid);
                    oldUpdate.merge(update);
                } else {
                    updates.put(qid, update);
                }
            }
            
            /**
             * TODO:
             * - support for new items
             * - support for duplicate strategy and action
             */
            
            // Perform edits
            int totalItemUpdates = updates.size();
            int updatesDone = 0;
            for(ItemUpdate update : updates.values()) {
                try {
                    wbde.updateStatements(update.getItemId(), update.getAddedStatements(), update.getDeletedStatements(), _summary);
                    
                } catch (MediaWikiApiErrorException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                updatesDone++;
                _progress = (100*updatesDone) / totalItemUpdates;
                
                if(_canceled) {
                    break;
                }
            }
            _progress = 100;
            
            if (!_canceled) {
                Change change = new PerformWikibaseEditsChange();
                
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
