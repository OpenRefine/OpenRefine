package com.google.refine.tests.process;

import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.process.QuickHistoryEntryProcess;
import com.google.refine.tests.util.TestUtils;

public class QuickHistoryEntryProcessTests {
    public static class QuickHistoryEntryProcessStub extends QuickHistoryEntryProcess {

        public QuickHistoryEntryProcessStub(Project project, String briefDescription) {
            super(project, briefDescription);
            
        }

        @Override
        protected HistoryEntry createHistoryEntry(long historyEntryID)
                throws Exception {
            return null;
        }
        
    }
    
    @Test
    public void serializeQuickHistoryEntryProcess() {
        Project project = mock(Project.class);
        Process process = new QuickHistoryEntryProcessStub(project, "quick description");
        int hashCode = process.hashCode();
        TestUtils.isSerializedTo(process, "{"
                + "\"id\":"+hashCode+","
                + "\"description\":"
                + "\"quick description\","
                + "\"immediate\":true,"
                + "\"status\":\"pending\"}");
    }
}
