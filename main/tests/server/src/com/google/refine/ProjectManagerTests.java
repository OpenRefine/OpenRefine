/*

Copyright 2010, 2023 Google Inc. & OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;

import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.Project;
import com.google.refine.model.ProjectStub;
import com.google.refine.model.Row;
import com.google.refine.process.ProcessManager;

public class ProjectManagerTests extends RefineTest {

    private static final Instant BASE_DATE = Instant.parse("1970-01-02T00:30:00Z");
    private static final int ROW_COUNT = 3;
    private static final String[] TAGS = { "testtag1", "testtag2" };
    ProjectManagerStub pm;
    ProjectManagerStub SUT;
    Project project;
    ProjectMetadata metadata;
    ProcessManager procmgr;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() throws NoSuchFieldException, IllegalAccessException {
        pm = new ProjectManagerStub();
        SUT = spy(pm);

        project = mock(Project.class);
        // Hack to override final declaration of "rows" field so that we can initialize
        Field field = Project.class.getDeclaredField("rows");
        field.setAccessible(true);
        field.set(project, new ArrayList<>());
        addRows(project);

        metadata = mock(ProjectMetadata.class);
        when(metadata.getTags()).thenReturn(TAGS);
        procmgr = mock(ProcessManager.class);
        when(project.getProcessManager()).thenReturn(procmgr);
        when(procmgr.hasPending()).thenReturn(false); // always false for now, but should test separately
    }

    private void addRows(Project p) {
        for (int i = 0; i < ROW_COUNT; i++) {
            p.rows.add(new Row(3));
        }
    }

    @AfterMethod
    public void TearDown() {
        metadata = null;
        project = null;
        SUT = null;
        pm = null;
    }

    @Test
    public void canRegisterProject() {

        SUT.registerProject(project, metadata);

        AssertProjectRegistered();
        verify(metadata, times(1)).getTags();
        verify(metadata).setRowCount(ROW_COUNT);

        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
        assertEquals(SUT.getAllProjectsTags().keySet(), Set.of(TAGS));
    }

    // TODO test registerProject in race condition

    @Test
    public void canEnsureProjectSave() throws Exception {
        whenGetSaveTimes(project, metadata);
        registerProject();

        // run test
        SUT.ensureProjectSaved(project.id);

        // assert and verify
        AssertProjectRegistered();
        verify(SUT, times(1)).saveMetadata(metadata, project.id);
        this.verifySaveTimeCompared(1);
        verify(SUT, times(1)).saveProject(project);
        verify(metadata, times(1)).getTags();
        verify(metadata).setRowCount(ROW_COUNT);

        // ensure end
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    // TODO test ensureProjectSave in race condition

    @Test
    public void canSaveAllModified() {
        whenGetSaveTimes(project, metadata); // 5-minute difference
        registerProject(project, metadata);

        // add a second project to the cache
        Project project2 = spy(new ProjectStub(2));
        addRows(project2);
        ProjectMetadata metadata2 = mock(ProjectMetadata.class);
        whenGetSaveTimes(project2, metadata2, 10); // not modified since the last save but within 30 seconds flush limit
        registerProject(project2, metadata2);

        // check that the two projects are not the same
        Assert.assertNotEquals(project2.id, project.id);

        SUT.save(true);

        verifySaved(project, metadata);

        verifySaved(project2, metadata2);

        verify(SUT, times(1)).saveWorkspace();
    }

    @Test
    public void canFlushFromCache() {

        whenGetSaveTimes(project, metadata, -10);// already saved (10 seconds before)
        registerProject(project, metadata);
        Assert.assertSame(SUT.getProject(0), project);

        SUT.save(true);

        verify(metadata, atLeastOnce()).getModified();
        verify(metadata, atLeastOnce()).getTags();
        verify(metadata).setRowCount(ROW_COUNT);
        verify(project, atLeastOnce()).getProcessManager();
        verify(project, atLeastOnce()).getLastSave();
        verify(project, times(1)).dispose();
        verify(SUT, never()).saveProject(project);
        Assert.assertNull(SUT.getProject(0));
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);

        verify(SUT, times(1)).saveWorkspace();
    }

    @Test
    public void cannotSaveWhenBusy() {
        registerProject();
        SUT.setBusy(true);

        SUT.save(false);

        verify(SUT, never()).saveProjects(Mockito.anyBoolean());
        verify(SUT, never()).saveWorkspace();
        verify(metadata, times(1)).getTags();
        verify(metadata).setRowCount(ROW_COUNT);
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    // TODO test canSaveAllModifiedWithRaceCondition

    @Test
    public void canSaveSomeModified() {
        registerProject();
        whenGetSaveTimes(project, metadata);

        SUT.save(false); // not busy

        verifySaved(project, metadata);
        verify(SUT, times(1)).saveWorkspace();

    }
    // TODO test canSaveAllModifiedWithRaceCondition

    // -------------helpers-------------

    protected void registerProject() {
        this.registerProject(project, metadata);
    }

    protected void registerProject(Project proj, ProjectMetadata meta) {
        SUT.registerProject(proj, meta);
    }

    protected void AssertProjectRegistered() {
        assertEquals(SUT.getProject(project.id), project);
        assertEquals(SUT.getProjectMetadata(project.id), metadata);
    }

    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta) {
        whenGetSaveTimes(proj, meta, 5);
    }

    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta, int secondsDifference) {
        whenProjectGetLastSave(proj);
        whenMetadataGetModified(meta, secondsDifference);
    }

    protected void whenProjectGetLastSave(Project proj) {
        when(proj.getLastSave()).thenReturn(BASE_DATE);
    }

    protected void whenMetadataGetModified(ProjectMetadata meta, int secondsDifference) {
        when(meta.getModified()).thenReturn(BASE_DATE.plusSeconds(secondsDifference));
    }

    protected void verifySaveTimeCompared(int times) {
        verifySaveTimeCompared(project, metadata, times);
    }

    protected void verifySaveTimeCompared(Project project, ProjectMetadata metadata, int times) {
        verify(metadata, times(times)).getModified();
        verify(project, times(times)).getLastSave();
    }

    protected void verifySaved(Project proj, ProjectMetadata meta) {
        verify(meta, times(1)).getModified();
        verify(proj, times(2)).getLastSave();
        verify(SUT, times(1)).saveProject(proj);
        verify(meta, times(1)).getTags();
        verify(meta).setRowCount(ROW_COUNT);

        verifyNoMoreInteractions(proj);
        verifyNoMoreInteractions(meta);
    }
}
