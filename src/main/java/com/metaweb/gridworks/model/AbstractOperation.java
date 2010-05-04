package com.metaweb.gridworks.model;

import java.util.Properties;

import org.apache.commons.lang.NotImplementedException;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.process.QuickHistoryEntryProcess;

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
        throw new NotImplementedException();
    }
    
    protected String getBriefDescription(Project project) {
        throw new NotImplementedException();
    }
}
