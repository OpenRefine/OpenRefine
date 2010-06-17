package com.metaweb.gridworks.tests.history;

import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.tests.GridworksTest;


public class HistoryTests extends GridworksTest {
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    //System Under Test
    History SUT;

    //dependencies
    Project proj;
    ProjectManager projectManager;

    @BeforeMethod
    public void SetUp(){
        projectManager = mock(ProjectManager.class);
        ProjectManager.singleton = projectManager;
        proj = new Project();
        SUT = new History(proj);
    }

    @AfterMethod
    public void TearDown(){
        SUT = null;
        proj = null;
    }

    @Test
    public void canAddEntry(){
        //local dependencies
        HistoryEntry entry = mock(HistoryEntry.class);
        Project project = mock(Project.class);
        ProjectMetadata projectMetadata = mock(ProjectMetadata.class);

        when(projectManager.getProject(Mockito.anyLong())).thenReturn(project);
        when(projectManager.getProjectMetadata(Mockito.anyLong())).thenReturn(projectMetadata);

        SUT.addEntry(entry);

        verify(projectManager, times(1)).getProject(Mockito.anyLong());
        verify(entry, times(1)).apply(project);
        verify(projectMetadata, times(1)).updateModified();
        Assert.assertEquals(SUT.getLastPastEntries(1).get(0), entry);
    }
}
