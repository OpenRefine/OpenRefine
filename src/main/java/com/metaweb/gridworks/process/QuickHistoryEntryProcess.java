package com.metaweb.gridworks.process;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;

abstract public class QuickHistoryEntryProcess extends Process {
    final protected Project _project;
    final protected String _briefDescription;
    protected HistoryEntry _historyEntry;
    boolean _done = false;
    
    public QuickHistoryEntryProcess(Project project, String briefDescription) {
        _project = project;
        _briefDescription = briefDescription;
    }
    
    public void cancel() {
        throw new RuntimeException("Not a long-running process");
    }

    public boolean isImmediate() {
        return true;
    }
    
    public boolean isRunning() {
        throw new RuntimeException("Not a long-running process");
    }

    public void performImmediate() throws Exception {
        if (_historyEntry == null) {
            _historyEntry = createHistoryEntry();
        }
        _project.history.addEntry(_historyEntry);
        _done = true;
    }

    public void startPerforming(ProcessManager manager) {
        throw new RuntimeException("Not a long-running process");
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("description"); writer.value(_historyEntry != null ? _historyEntry.description : _briefDescription);
        writer.key("immediate"); writer.value(true);
        writer.key("status"); writer.value(_done ? "done" : "pending");
        writer.endObject();
    }


    @Override
    public boolean isDone() {
        return _done;
    }
    
    abstract protected HistoryEntry createHistoryEntry() throws Exception;
}
