package com.google.refine.tests.process;

import static org.mockito.Mockito.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryProcess;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.ProjectMetadata;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class HistoryProcessTests extends RefineTest {
    
    private Project project;
    private ProjectMetadata projectMetadata;

    @BeforeMethod
    public void setUp() {
        project = new Project();
        projectMetadata = mock(ProjectMetadata.class);
        ProjectManager.singleton.registerProject(project, projectMetadata);
        AbstractOperation op = mock(AbstractOperation.class);
        Change ch = mock(Change.class);
        HistoryEntry entry = new HistoryEntry(1234L, project, "first operation", op, ch);
        project.history.addEntry(entry);
        entry = new HistoryEntry(5678L, project, "second operation", op, ch);
        project.history.addEntry(entry);
    }
    
    @Test
    public void serializeHistoryProcess() {
        HistoryProcess process = new HistoryProcess(project, 1234L);
        TestUtils.isSerializedTo(process, "{"
                + "\"description\":\"Undo/redo until after first operation\","
                + "\"immediate\":true,"
                + "\"status\":\"pending\"}");
    }
}
