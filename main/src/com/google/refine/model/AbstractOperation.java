package com.google.refine.model;

import java.util.Properties;

import com.google.refine.Jsonizable;
import com.google.refine.history.HistoryEntry;
import com.google.refine.process.Process;
import com.google.refine.process.QuickHistoryEntryProcess;

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
