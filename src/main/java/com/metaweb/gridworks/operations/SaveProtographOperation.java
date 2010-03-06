package com.metaweb.gridworks.operations;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.protograph.Protograph;
import com.metaweb.gridworks.util.ParsingUtilities;

public class SaveProtographOperation extends AbstractOperation {
    private static final long serialVersionUID = 3134524625206033285L;
    
    final protected Protograph _protograph;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new SaveProtographOperation(
            Protograph.reconstruct(obj.getJSONObject("protograph"))
        );
    }
    
    public SaveProtographOperation(
        Protograph protograph
    ) {
        _protograph = protograph;
    }

    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value("Save protograph");
        writer.key("protograph"); _protograph.write(writer, options);
        writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Save schema skeleton";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project) throws Exception {
        String description = "Save schema-alignment protograph";
        
        Change change = new ProtographChange(_protograph);
        
        return new HistoryEntry(project, description, SaveProtographOperation.this, change);
    }

    static public class ProtographChange implements Change {
        final protected Protograph     _newProtograph;
        protected Protograph        _oldProtograph;
        
        public ProtographChange(Protograph protograph) {
            _newProtograph = protograph;
        }
        
        public void apply(Project project) {
            synchronized (project) {
                _oldProtograph = project.protograph;
                project.protograph = _newProtograph;
            }
        }

        public void revert(Project project) {
            synchronized (project) {
                project.protograph = _oldProtograph;
            }
        }
        
        public void save(Writer writer, Properties options) throws IOException {
            writer.write("newProtograph="); writeProtograph(_newProtograph, writer); writer.write('\n');
            writer.write("oldProtograph="); writeProtograph(_oldProtograph, writer); writer.write('\n');
            writer.write("/ec/\n"); // end of change marker
        }
        
        static public Change load(LineNumberReader reader) throws Exception {
            Protograph oldProtograph = null;
            Protograph newProtograph = null;
            
            String line;
            while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
                int equal = line.indexOf('=');
                CharSequence field = line.subSequence(0, equal);
                String value = line.substring(equal + 1);
                
                if ("oldProtograph".equals(field)) {
                    if (value.length() > 0) {
                        oldProtograph = Protograph.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                    }
                } else if ("newProtograph".equals(field)) {
                    if (value.length() > 0) {
                        newProtograph = Protograph.reconstruct(ParsingUtilities.evaluateJsonStringToObject(value));
                    }
                }

            }
            
            ProtographChange change = new ProtographChange(newProtograph);
            change._oldProtograph = oldProtograph;
            
            return change;
        }
        
        static protected void writeProtograph(Protograph p, Writer writer) throws IOException {
            if (p != null) {
                JSONWriter jsonWriter = new JSONWriter(writer);
                try {
                    p.write(jsonWriter, new Properties());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    } 
}
