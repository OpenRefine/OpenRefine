/*

Copyright 2010, Google Inc.
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

package org.openrefine;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectMetadata;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.Project;
import org.openrefine.model.ProjectStub;
import org.openrefine.process.ProcessManager;

public class ProjectManagerTests {

    ProjectManagerStub pm;
    ProjectManagerStub SUT;
    Project project;
    ProjectMetadata metadata;
    ProcessManager procmgr;

    @BeforeMethod
    public void SetUp() {
        pm = new ProjectManagerStub(mock(DatamodelRunner.class));
        SUT = spy(pm);
        project = mock(Project.class);
        metadata = mock(ProjectMetadata.class);
        procmgr = mock(ProcessManager.class);
        when(project.getProcessManager()).thenReturn(procmgr);
        when(project.getId()).thenReturn(1234L);
        when(procmgr.hasPending()).thenReturn(false); // always false for now, but should test separately
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
        verify(project, atLeast(1)).getId();
        verify(metadata, times(1)).getTags();

        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    // TODO test registerProject in race condition

    @Test
    public void canEnsureProjectSave() {
        whenGetSaveTimes(project, metadata);
        registerProject();

        // run test
        SUT.ensureProjectSaved(project.getId());

        // assert and verify
        AssertProjectRegistered();
        verify(project, atLeast(1)).getId();
        try {
            verify(SUT, times(1)).saveMetadata(metadata, project.getId());
        } catch (Exception e) {
            Assert.fail();
        }
        this.verifySaveTimeCompared(1);
        verify(SUT, times(1)).saveProject(project);
        verify(metadata, times(1)).getTags();
        verify(project, atLeast(1)).getId();

        // ensure end
        verifyNoMoreInteractions(project);
        verifyNoMoreInteractions(metadata);
    }

    // TODO test ensureProjectSave in race condition

    @Test
    public void canSaveAllModified() {
        whenGetSaveTimes(project, metadata); // 5 minute difference
        registerProject(project, metadata);

        // add a second project to the cache
        Project project2 = spy(new ProjectStub(2));
        ProjectMetadata metadata2 = mock(ProjectMetadata.class);
        whenGetSaveTimes(project2, metadata2, 10); // not modified since the last save but within 30 seconds flush limit
        registerProject(project2, metadata2);

        // check that the two projects are not the same
        Assert.assertFalse(project.getId() == project2.getId());

        SUT.save(true);

        verifySaved(project, metadata);

        verifySaved(project2, metadata2);

        verify(SUT, times(1)).saveWorkspace();
    }

    @Test
    public void canFlushFromCache() {

        whenGetSaveTimes(project, metadata, -10);// already saved (10 seconds before)
        registerProject(project, metadata);
        Assert.assertSame(SUT.getProject(1234L), project);

        SUT.save(true);

        verify(metadata, times(1)).getModified();
        verify(metadata, times(1)).getTags();
        verify(project, times(1)).getProcessManager();
        verify(project, times(2)).getLastSave();
        verify(SUT, never()).saveProject(project);
        Assert.assertEquals(SUT.getProject(0), null);
        verify(project, atLeast(1)).getId();
        verify(project, times(1)).dispose();
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
        verify(project, atLeast(1)).getId();
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
        Assert.assertEquals(SUT.getProject(project.getId()), project);
        Assert.assertEquals(SUT.getProjectMetadata(project.getId()), metadata);
    }

    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta) {
        whenGetSaveTimes(proj, meta, 5);
    }

    protected void whenGetSaveTimes(Project proj, ProjectMetadata meta, int secondsDifference) {
        whenProjectGetLastSave(proj);
        whenMetadataGetModified(meta, secondsDifference);
    }

    protected void whenProjectGetLastSave(Project proj) {
        LocalDateTime projectLastSaveDate = LocalDateTime.of(1970, 01, 02, 00, 30, 00);
        when(proj.getLastSave()).thenReturn(projectLastSaveDate);
    }

    protected void whenMetadataGetModified(ProjectMetadata meta) {
        whenMetadataGetModified(meta, 5 * 60);
    }

    protected void whenMetadataGetModified(ProjectMetadata meta, int secondsDifference) {
        LocalDateTime metadataModifiedDate = LocalDateTime.of(1970, 01, 02, 00, 30 + secondsDifference);
        when(meta.getModified()).thenReturn(metadataModifiedDate);
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
        verify(proj, atLeast(1)).getId();

        verifyNoMoreInteractions(proj);
        verifyNoMoreInteractions(meta);
    }
}
