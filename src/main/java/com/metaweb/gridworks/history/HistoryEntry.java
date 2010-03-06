package com.metaweb.gridworks.history;

import java.io.File; 
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Project;

public class HistoryEntry implements Serializable, Jsonizable {
    private static final long serialVersionUID = 532766467813930262L;
    
    final public long                 id;
    final public long                 projectID;
    final public String                description;
    final public AbstractOperation    operation;
    final public Date                time;
    
    transient protected Change _change;
    
    public HistoryEntry(Project project, String description, AbstractOperation operation, Change change) {
        this.id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
        this.projectID = project.id;
        this.description = description;
        this.operation = operation;
        this.time = new Date();
        
        _change = change;
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
        
        writer.object();
        writer.key("id"); writer.value(id);
        writer.key("description"); writer.value(description);
        writer.key("time"); writer.value(sdf.format(time));
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
            } catch (IOException e) {
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
    
    protected void saveChange() throws IOException {
        File changeFile = getChangeFile();
        if (!(changeFile.exists())) {
            saveChange(changeFile);
        }
    }
    
    protected void saveChange(File file) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
        try {
            out.putNextEntry(new ZipEntry("change.txt"));
            try {
                Writer writer = new OutputStreamWriter(out);
                try {
                    Properties options = new Properties();
                    options.setProperty("mode", "save");
                    
                    writer.write(_change.getClass().getName()); writer.write('\n');
                        
                    _change.save(writer, options);
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
