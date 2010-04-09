package com.metaweb.gridworks.history;

import java.io.File; 
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.operations.OperationRegistry;
import com.metaweb.gridworks.util.ParsingUtilities;

/**
 * This is the metadata of a Change. It's small, so we can load it in order to
 * obtain information about a change without actually loading the change.
 */
public class HistoryEntry implements Jsonizable {
    final public long   id;
    final public long   projectID;
    final public String description;
    final public Date   time;
    
    // the abstract operation, if any, that results in the change
    final public AbstractOperation operation; 
    
    // the actual change, loaded on demand
    transient protected Change _change;
    
    private final static String OPERATION = "operation";
    
    public HistoryEntry(Project project, String description, AbstractOperation operation, Change change) {
        this.id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
        this.projectID = project.id;
        this.description = description;
        this.operation = operation;
        this.time = new Date();
        
        _change = change;
    }
    
    protected HistoryEntry(long id, long projectID, String description, AbstractOperation operation, Date time) {
        this.id = id;
        this.projectID = projectID;
        this.description = description;
        this.operation = operation;
        this.time = time;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(id);
        writer.key("description"); writer.value(description);
        writer.key("time"); writer.value(ParsingUtilities.dateToString(time));
        if ("save".equals(options.getProperty("mode")) && operation != null) {
            writer.key(OPERATION); operation.write(writer, options);
        }
        writer.endObject();
    }
    
    public void apply(Project project) {
        if (_change == null) {
            loadChange();
        }
        
        synchronized (project) {
            _change.apply(project);
            
            // When a change is applied, it can hang on to old data (in order to be able
            // to revert later). Hence, we need to save the change out.
            
            try {
                saveChange();
            } catch (Exception e) {
                e.printStackTrace();
                
                _change.revert(project);
                
                throw new RuntimeException("Failed to apply change", e);
            }
        }
    }
    
    public void revert(Project project) {
        if (_change == null) {
            loadChange();
        }
        _change.revert(project);
    }
    
    public void delete() {
        File file = getChangeFile();
        if (file.exists()) {
            file.delete();
        }
    }
    
    public void save(Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public HistoryEntry load(Project project, String s) throws Exception {
        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(s);
        
        AbstractOperation operation = null;
        if (obj.has(OPERATION) && !obj.isNull(OPERATION)) {
            operation = OperationRegistry.reconstruct(project, obj.getJSONObject(OPERATION));
        }
        
        return new HistoryEntry(
            obj.getLong("id"),
            project.id,
            obj.getString("description"),
            operation,
            ParsingUtilities.stringToDate(obj.getString("time"))
        );
    }

    
    protected void loadChange() {
        File changeFile = getChangeFile();
        
        try {
            loadChange(changeFile);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load change file " + changeFile.getAbsolutePath(), e);
        }
    }
    
    protected void loadChange(File file) throws Exception {
        ZipInputStream in = new ZipInputStream(new FileInputStream(file));
        try {
            ZipEntry entry = in.getNextEntry();
            
            assert "change.txt".equals(entry.getName());
            
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
            try {
                _change = History.readOneChange(reader);
            } finally {
                reader.close();
            }
        } finally {
            in.close();
        }
    }
    
    protected void saveChange() throws Exception {
        File changeFile = getChangeFile();
        if (!(changeFile.exists())) {
            saveChange(changeFile);
        }
    }
    
    protected void saveChange(File file) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            out.putNextEntry(new ZipEntry("change.txt"));
            try {
                Writer writer = new OutputStreamWriter(out);
                try {
                    History.writeOneChange(writer, _change);
                } finally {
                    writer.flush();
                }
            } finally {
                out.closeEntry();
            }
        } finally {
            out.close();
        }
    }
    
    protected File getChangeFile() {
        return new File(getHistoryDir(), id + ".change.zip");
    }
    
    protected File getHistoryDir() {
        File dir = new File(ProjectManager.singleton.getProjectDir(projectID), "history");
        dir.mkdirs();
        
        return dir;
    }
}
