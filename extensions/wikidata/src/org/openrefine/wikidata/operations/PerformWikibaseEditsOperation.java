package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import org.openrefine.wikidata.editing.ConnectionManager;
import org.openrefine.wikidata.editing.NewItemLibrary;
import org.openrefine.wikidata.editing.CellCoordinates;
import org.openrefine.wikidata.editing.CellCoordinatesKeyDeserializer;
import org.openrefine.wikidata.schema.ItemUpdate;
import org.openrefine.wikidata.schema.NewEntityIdValue;
import org.openrefine.wikidata.schema.WikibaseSchema;
import org.wikidata.wdtk.datamodel.implementation.DataObjectFactoryImpl;
import org.wikidata.wdtk.datamodel.interfaces.DataObjectFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;
import org.wikidata.wdtk.datamodel.interfaces.SiteLink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

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
    public enum DuplicateDetectionStrategy {
        PROPERTY, SNAK, SNAK_QUALIFIERS
    }
    
    public enum OnDuplicateAction {
        SKIP, MERGE
    }
    
    private DuplicateDetectionStrategy strategy;
    private OnDuplicateAction duplicateAction;
    private String summary;
    
    public PerformWikibaseEditsOperation(
            JSONObject engineConfig,
            DuplicateDetectionStrategy strategy,
            OnDuplicateAction duplicateAction,
            String summary) {
        super(engineConfig);
        this.strategy = strategy;
        this.duplicateAction = duplicateAction;
        this.summary = summary;

        // getEngine(request, project);
    }
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj)
            throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        String strategy = obj.getString("duplicate_strategy");
        String action = obj.getString("duplicate_action");
        String summary = obj.getString("summary");
        if (summary == null) {
            summary = "#openrefine";
        }
        return new PerformWikibaseEditsOperation(
                engineConfig,
                DuplicateDetectionStrategy.valueOf(strategy),
                OnDuplicateAction.valueOf(action),
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
        writer.key("duplicate_strategy");
        writer.value(strategy.name());
        writer.key("duplicate_action");
        writer.value(duplicateAction.name());
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
                    SimpleModule simpleModule = new SimpleModule();
                    simpleModule.addKeyDeserializer(CellCoordinates.class, new CellCoordinatesKeyDeserializer());
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
            Map<EntityIdValue, ItemUpdate> updates =  ItemUpdate.groupBySubject(itemDocuments);
            
            /**
             * TODO:
             * - support for new items
             * - support for duplicate strategy and action
             */
            
            // Perform edits
            NewItemLibrary newItemLibrary = new NewItemLibrary();
            DataObjectFactory factory = new DataObjectFactoryImpl();
            int totalItemUpdates = updates.size();
            int updatesDone = 0;
            for(ItemUpdate update : updates.values()) {
                try {
                    // New item
                    if (update.getItemId().getId() == "Q0") {
                        NewEntityIdValue newCell = (NewEntityIdValue)update.getItemId();
                        update.normalizeLabelsAndAliases();

                        
                        ItemDocument itemDocument = factory.getItemDocument(
                                update.getItemId(),
                                update.getLabels(),
                                update.getDescriptions(),
                                update.getAliases(),
                                update.getAddedStatementGroups(),
                                new HashMap<String,SiteLink>(),
                                0L);
                                
                        ItemDocument createdDoc = wbde.createItemDocument(itemDocument, _summary);
                        newItemLibrary.setQid(newCell, createdDoc.getItemId().getId());
                    } else {
                        // Existing item
                        wbde.updateTermsStatements(update.getItemId(),
                                update.getLabels(),
                                update.getDescriptions(),
                                update.getAliases(),
                                new ArrayList<MonolingualTextValue>(),
                                update.getAddedStatements(),
                                update.getDeletedStatements(), _summary);   
                    }
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
