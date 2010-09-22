package com.google.refine.tests;

import java.util.Date;
import java.util.GregorianCalendar;

import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.model.Project;
import com.google.refine.tests.model.ProjectStub;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;

public class ProjectManagerTests extends GridworksTest {
    ProjectManagerStub pm;
    ProjectManagerStub SUT;
    Project project;
    ProjectMetadata metadata;

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp(){
        pm = new ProjectManagerStub();
        SUT = spy(pm);
        project = mock(Project.class);
        metadata = mock(ProjectMetadata.class);
    }

    @AfterMethod
    public void TearDown(){
        metadata = null;
        project = null;
        SUT = null;
        pm = null;
    }

    @Test
    public void canRegisterProject(){

        SUT.registerProject(project, metadata);

        AssertProjectRegistered();

        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    //TODO test registerProject in race condition

    @Test
    public void canEnsureProjectSave(){
        whenGetSaveTimes(project, metadata);
        registerProject();

        //run test
        SUT.ensureProjectSaved(project.id);

        //assert and verify
        AssertProjectRegistered();
        try {
            verify(SUT, times(1)).saveMetadata(metadata, project.id);
        } catch (Exception e) {
            Assert.fail();
        }
        this.verifySaveTimeCompared(1);
        verify(SUT, times(1)).saveProject(project);

        //ensure end
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    //TODO test ensureProjectSave in race condition

    @Test
    public void canSaveAllModified(){
        whenGetSaveTimes(project, metadata); //5 minute difference
        registerProject(project, metadata);

        //add a second project to the cache
        Project project2 = spy(new ProjectStub(2));
        ProjectMetadata metadata2 = mock(ProjectMetadata.class);
        whenGetSaveTimes(project2, metadata2, 10); //not modified since the last save but within 30 seconds flush limit
        registerProject(project2, metadata2);

        //check that the two projects are not the same
        Assert.assertFalse(project.id == project2.id);

        SUT.save(true);

        verifySaved(project, metadata);

        verifySaved(project2, metadata2);

        verify(SUT, times(1)).saveWorkspace();
    }

    @Test
    public void canFlushFromCache(){

        whenGetSaveTimes(project, metadata, -10 );//already saved (10 seconds before)
        registerProject(project, metadata);
        Assert.assertSame(SUT.getProject(0), project);

        SUT.save(true);

        verify(metadata, times(1)).getModified();
        verify(project, times(2)).getLastSave();
        verify(SUT, never()).saveProject(project);
        Assert.assertEquals(SUT.getProject(0), null);
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);

        verify(SUT, times(1)).saveWorkspace();
    }

    @Test
    public void cannotSaveWhenBusy(){
        registerProject();
        SUT.setBusy(true);

        SUT.save(false);

        verify(SUT, never()).saveProjects(Mockito.anyBoolean());
        verify(SUT, never()).saveWorkspace();
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    //TODO test canSaveAllModifiedWithRaceCondition

    @Test
    public void canSaveSomeModified(){
        registerProject();
        whenGetSaveTimes(project, metadata );

        SUT.save(false); //not busy

        verifySaved(project, metadata);
        verify(SUT, times(1)).saveWorkspace();

    }
    //TODO test canSaveAllModifiedWithRaceCondition

    //-------------helpers-------------

    protected void registerProject(){
        this.registerProject(project, metadata);
    }
    protected void registerProject(Project proj, ProjectMetadata meta){
        SUT.registerProject(proj, meta);
    }

    protected void AssertProjectRegistered(){
        Assert.assertEquals(SUT.getProject(project.id), project);
        Assert.assertEquals(SUT.getProjectMetadata(project.id), metadata);
    }

    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta){
        whenGetSaveTimes(proj, meta, 5);
    }
    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta, int secondsDifference){
        whenProjectGetLastSave(proj);
        whenMetadataGetModified(meta, secondsDifference);
    }

    protected void whenProjectGetLastSave(Project proj){
        Date projectLastSaveDate = new GregorianCalendar(1970,01,02,00,30,00).getTime();
        when(proj.getLastSave()).thenReturn(projectLastSaveDate);
    }

    protected void whenMetadataGetModified(ProjectMetadata meta){
        whenMetadataGetModified(meta, 5*60);
    }
    protected void whenMetadataGetModified(ProjectMetadata meta, int secondsDifference){
        Date metadataModifiedDate = new GregorianCalendar(1970,01,02,00, 30, secondsDifference).getTime();
        when(meta.getModified()).thenReturn(metadataModifiedDate);
    }

    protected void verifySaveTimeCompared(int times){
        verifySaveTimeCompared(project, metadata, times);
    }
    protected void verifySaveTimeCompared(Project project, ProjectMetadata metadata, int times){
        verify(metadata, times(times)).getModified();
        verify(project, times(times)).getLastSave();
    }

    protected void verifySaved(Project proj, ProjectMetadata meta){
        verify(meta, times(1)).getModified();
        verify(proj, times(2)).getLastSave();
        verify(SUT, times(1)).saveProject(proj);

        verifyNoMoreInteractions(proj);
        verifyNoMoreInteractions(meta);
    }
}
