package com.metaweb.gridworks.history;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.ProcessManager;

/**
 * The process for undoing or redoing. This involves calling apply() and revert()
 * on changes.
 */
public class HistoryProcess extends Process {
    final protected Project _project;
    final protected long    _lastDoneID;
    final protected String  _description;
    
    protected boolean _done = false;

    private final static String WARN = "Not a long-running process";
    
    public HistoryProcess(Project project, long lastDoneID) {
        _project = project;
        _lastDoneID = lastDoneID;
        
        if (_lastDoneID == 0) {
            _description = "Undo all";
        } else {
            HistoryEntry entry = _project.history.getEntry(_lastDoneID);
            _description = "Undo/redo until after " + entry.description;
        }
    }
    
    public void cancel() {
        throw new RuntimeException(WARN);
    }

    public boolean isImmediate() {
        return true;
    }

    public HistoryEntry performImmediate() {
        _project.history.undoRedo(_lastDoneID);
        _done = true;
        
        return null;
    }

    public void startPerforming(ProcessManager manager) {
        throw new RuntimeException(WARN);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("description"); writer.value(_description);
        writer.key("immediate"); writer.value(true);
        writer.key("status"); writer.value(_done ? "done" : "pending");
        writer.endObject();
    }

    public boolean isDone() {
        throw new RuntimeException(WARN);
    }

    public boolean isRunning() {
        throw new RuntimeException(WARN);
    }
}
