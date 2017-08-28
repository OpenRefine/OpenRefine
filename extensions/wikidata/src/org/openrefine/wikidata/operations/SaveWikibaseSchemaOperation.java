package org.openrefine.wikidata.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.openrefine.wikidata.schema.WikibaseSchema;

import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class SaveWikibaseSchemaOperation extends AbstractOperation {

    final protected WikibaseSchema _schema;

    public SaveWikibaseSchemaOperation(WikibaseSchema schema) {
        this._schema = schema;
    }

    static public AbstractOperation reconstruct(Project project, JSONObject obj)
            throws Exception {
        return new SaveWikibaseSchemaOperation(WikibaseSchema.reconstruct(obj
                .getJSONObject("schema")));
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("op");
        writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description");
        writer.value("Save RDF schema skeleton");
        writer.key("schema");
        _schema.write(writer, options);
        writer.endObject();

    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Save Wikibase schema skelton";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project,
            long historyEntryID) throws Exception {
        String description = "Save Wikibase schema skeleton";
        
        Change change = new WikibaseSchemaChange(_schema);
        
        return new HistoryEntry(historyEntryID, project, description,
                SaveWikibaseSchemaOperation.this, change);
    }

    static public class WikibaseSchemaChange implements Change {
        final protected WikibaseSchema _newSchema;
        protected WikibaseSchema _oldSchema;
        public final static String overlayModelKey = "wikibaseSchema";
        
        public WikibaseSchemaChange(WikibaseSchema schema) {
            _newSchema = schema;
        }
        
        public void apply(Project project) {
            synchronized (project) {
                _oldSchema = (WikibaseSchema) project.overlayModels.get(overlayModelKey);
                project.overlayModels.put(overlayModelKey, _newSchema);
            }
        }
        
        public void revert(Project project) {
            synchronized (project) {
                if (_oldSchema == null) {
                    project.overlayModels.remove(overlayModelKey);
                } else {
                    project.overlayModels.put(overlayModelKey, _oldSchema);
                }
            }
        }
        
        public void save(Writer writer, Properties options) throws IOException {
            writer.write("newSchema=");
            writeWikibaseSchema(_newSchema, writer);
            writer.write('\n');
            writer.write("oldSchema=");
            writeWikibaseSchema(_oldSchema, writer);
            writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
        }
        
        static public Change load(LineNumberReader reader, Pool pool)
                throws Exception {
            WikibaseSchema oldSchema = null;
            WikibaseSchema newSchema = null;
            
            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);
                
                if ("oldSchema".equals(field) && value.length() > 0) {
                    oldSchema = WikibaseSchema.reconstruct(ParsingUtilities
                            .evaluateJsonStringToObject(value));
                } else if ("newSchema".equals(field) && value.length() > 0) {
                    newSchema = WikibaseSchema.reconstruct(ParsingUtilities
                            .evaluateJsonStringToObject(value));
                }
            }
            
            WikibaseSchemaChange change = new WikibaseSchemaChange(newSchema);
            change._oldSchema = oldSchema;
            
            return change;
        }
        
        static protected void writeWikibaseSchema(WikibaseSchema s, Writer writer)
                throws IOException {
            if (s != null) {
                JSONWriter jsonWriter = new JSONWriter(writer);
                try {
                    s.write(jsonWriter, new Properties());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
