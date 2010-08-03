package com.google.gridworks.model;

import java.util.Properties;

import com.google.gridworks.Jsonizable;
import com.google.gridworks.history.HistoryEntry;
import com.google.gridworks.process.Process;
import com.google.gridworks.process.QuickHistoryEntryProcess;

/*
 *  An abstract operation can be applied to different but similar
 *  projects.
 */
abstract public class AbstractOperation implements Jsonizable {
    public Process createProcess(Project project, Properties options) throws Exception {
        return new QuickHistoryEntryProcess(project, getBriefDescription(null)) {
            @Override
            protected HistoryEntry createHistoryEntry(long historyEntryID) throws Exception {
                return AbstractOperation.this.createHistoryEntry(_project, historyEntryID);
            }
        };
    }
    
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        throw new UnsupportedOperationException();
    }
    
    protected String getBriefDescription(Project project) {
        throw new UnsupportedOperationException();
    }
}
