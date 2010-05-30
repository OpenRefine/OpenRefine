package com.metaweb.gridworks.history;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.Pool;

/**
 * Track done and undone changes. Done changes can be undone; undone changes can be redone.
 * Each change is actually not tracked directly but through a history entry. The history
 * entry stores only the metadata, while the change object stores the actual data. Thus
 * the history entries are much smaller and can be kept in memory, while the change objects
 * are only loaded into memory on demand.
 */
public class History implements Jsonizable {
    static public Change readOneChange(InputStream in, Pool pool) throws Exception {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
        try {
            return readOneChange(reader, pool);
        } finally {
            reader.close();
        }
    }
    
    static public Change readOneChange(LineNumberReader reader, Pool pool) throws Exception {
        /* String version = */ reader.readLine();
        
        String className = reader.readLine();
        Class<? extends Change> klass = getChangeClass(className);
        
        Method load = klass.getMethod("load", LineNumberReader.class, Pool.class);
        
        return (Change) load.invoke(null, reader, pool);
    }
    
    static public void writeOneChange(OutputStream out, Change change, Pool pool) throws IOException {
        Writer writer = new OutputStreamWriter(out);
        try {
            History.writeOneChange(writer, change, pool);
        } finally {
            writer.flush();
        }
    }
    
    static public void writeOneChange(Writer writer, Change change, Pool pool) throws IOException {
        Properties options = new Properties();
        options.setProperty("mode", "save");
        options.put("pool", pool);
        
        writeOneChange(writer, change, options);
    }
    
    static public void writeOneChange(Writer writer, Change change, Properties options) throws IOException {
        writer.write(Gridworks.getVersion()); writer.write('\n');
        writer.write(change.getClass().getName()); writer.write('\n');
            
        change.save(writer, options);
    }
    
    @SuppressWarnings("unchecked")
    static public Class<? extends Change> getChangeClass(String className) throws ClassNotFoundException {
        return (Class<? extends Change>) Class.forName(className);
    }
    
    protected long               _projectID;
    protected List<HistoryEntry> _pastEntries;   // done changes, can be undone
    protected List<HistoryEntry> _futureEntries; // undone changes, can be redone
    
    public History(Project project) {
        _projectID = project.id;
        _pastEntries = new ArrayList<HistoryEntry>();
        _futureEntries = new ArrayList<HistoryEntry>();
    }
    
    synchronized public void addEntry(HistoryEntry entry) {
        entry.apply(ProjectManager.singleton.getProject(_projectID));
        _pastEntries.add(entry);
        
        setModified();
        
        // Any new change will clear all future entries.
        List<HistoryEntry> futureEntries = _futureEntries;
        _futureEntries = new ArrayList<HistoryEntry>();
        
        for (HistoryEntry entry2 : futureEntries) {
            try {
                // remove residual data on disk
                entry2.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void setModified() {
        ProjectManager.singleton.getProjectMetadata(_projectID).updateModified();
    }
    
    synchronized public List<HistoryEntry> getLastPastEntries(int count) {
        if (count <= 0) {
            return new LinkedList<HistoryEntry>(_pastEntries);
        } else {
            return _pastEntries.subList(Math.max(_pastEntries.size() - count, 0), _pastEntries.size());
        }
    }
    
    synchronized public void undoRedo(long lastDoneEntryID) {
        if (lastDoneEntryID == 0) {
            // undo all the way back to the start of the project
            undo(_pastEntries.size());
        } else {
            for (int i = 0; i < _pastEntries.size(); i++) {
                if (_pastEntries.get(i).id == lastDoneEntryID) {
                    undo(_pastEntries.size() - i - 1);
                    return;
                }
            }
            
            for (int i = 0; i < _futureEntries.size(); i++) {
                if (_futureEntries.get(i).id == lastDoneEntryID) {
                    redo(i + 1);
                    return;
                }
            }
        }
    }
    
    synchronized public long getPrecedingEntryID(long entryID) {
        if (entryID == 0) {
            return -1;
        } else {
            for (int i = 0; i < _pastEntries.size(); i++) {
                if (_pastEntries.get(i).id == entryID) {
                    return i == 0 ? 0 : _pastEntries.get(i - 1).id;
                }
            }
            
            for (int i = 0; i < _futureEntries.size(); i++) {
                if (_futureEntries.get(i).id == entryID) {
                    if (i > 0) {
                        return _futureEntries.get(i - 1).id;
                    } else if (_pastEntries.size() > 0) {
                        return _pastEntries.get(_pastEntries.size() - 1).id;
                    } else {
                        return 0;
                    }
                }
            }
        }
        return -1;
    }
    
    protected HistoryEntry getEntry(long entryID) {
        for (int i = 0; i < _pastEntries.size(); i++) {
            if (_pastEntries.get(i).id == entryID) {
                return _pastEntries.get(i);
            }
        }
        
        for (int i = 0; i < _futureEntries.size(); i++) {
            if (_futureEntries.get(i).id == entryID) {
                return _futureEntries.get(i);
            }
        }
        return null;
    }
    
    protected void undo(int times) {
        Project project = ProjectManager.singleton.getProject(_projectID);
        
        while (times > 0 && _pastEntries.size() > 0) {
            HistoryEntry entry = _pastEntries.get(_pastEntries.size() - 1);
            
            entry.revert(project);
            
            setModified();
            times--;
            
            _pastEntries.remove(_pastEntries.size() - 1);
            _futureEntries.add(0, entry);
        }
    }
    
    protected void redo(int times) {
        Project project = ProjectManager.singleton.getProject(_projectID);
        
        while (times > 0 && _futureEntries.size() > 0) {
            HistoryEntry entry = _futureEntries.get(0);
            
            entry.apply(project);
            
            setModified();
            times--;
            
            _pastEntries.add(entry);
            _futureEntries.remove(0);
        }
    }
    
    synchronized public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        
        writer.key("past"); writer.array();
        for (HistoryEntry entry : _pastEntries) {
            entry.write(writer, options);
        }
        writer.endArray();
        
        writer.key("future"); writer.array();
        for (HistoryEntry entry : _futureEntries) {
            entry.write(writer, options);
        }
        writer.endArray();
        
        writer.endObject();
    }
    
    synchronized public void save(Writer writer, Properties options) throws IOException {
        writer.write("pastEntryCount="); writer.write(Integer.toString(_pastEntries.size())); writer.write('\n');
        for (HistoryEntry entry : _pastEntries) {
            entry.save(writer, options); writer.write('\n');
        }
        
        writer.write("futureEntryCount="); writer.write(Integer.toString(_futureEntries.size())); writer.write('\n');
        for (HistoryEntry entry : _futureEntries) {
            entry.save(writer, options); writer.write('\n');
        }
        
        writer.write("/e/\n");
    }
    
    synchronized public void load(Project project, LineNumberReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null && !"/e/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("pastEntryCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    _pastEntries.add(HistoryEntry.load(project, reader.readLine()));
                }
            } else if ("futureEntryCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    _futureEntries.add(HistoryEntry.load(project, reader.readLine()));
                }
            }
        }
    }
}
