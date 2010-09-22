package com.google.refine.process;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryProcess;

public class ProcessManager implements Jsonizable {
    protected List<Process> _processes = new LinkedList<Process>();
    
    public ProcessManager() {
        
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("processes"); writer.array();
        for (Process p : _processes) {
            p.write(writer, options);
        }
        writer.endArray();
        
        writer.endObject();
    }

    public HistoryEntry queueProcess(Process process) throws Exception {
        if (process.isImmediate() && _processes.size() == 0) {
            return process.performImmediate();
        } else {
            _processes.add(process);
            
            update();
        }
        return null;
    }
    
    public boolean queueProcess(HistoryProcess process) throws Exception {
        if (process.isImmediate() && _processes.size() == 0) {
            return process.performImmediate() != null;
        } else {
            _processes.add(process);
            
            update();
        }
        return false;
    }
    
    public boolean hasPending() {
        return _processes.size() > 0;
    }
    
    public void onDoneProcess(Process p) {
        _processes.remove(p);
        update();
    }
    
    public void cancelAll() {
        for (Process p : _processes) {
            if (!p.isImmediate() && p.isRunning()) {
                p.cancel();
            }
        }
        _processes.clear();
    }
    
    protected void update() {
        while (_processes.size() > 0) {
            Process p = _processes.get(0);
            if (p.isImmediate()) {
                try {
                    p.performImmediate();
                } catch (Exception e) {
                    // TODO: Not sure what to do yet
                    e.printStackTrace();
                }
                _processes.remove(0);
            } else if (p.isDone()) {
                _processes.remove(0);
            } else {
                if (!p.isRunning()) {
                    p.startPerforming(this);
                }
                break;
            }
        }
    }
}
