/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package org.openrefine.io;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.ProjectMetadata;
import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.model.Project;
import org.openrefine.model.Runner;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.GetProjectIDException;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class FileProjectManagerTests {

    protected File workspaceDir;
    protected File workspaceFile;
    protected Grid mockGrid;

    @BeforeMethod
    public void createDirectory() throws IOException {
        workspaceDir = TestUtils.createTempDirectory("openrefine-test-workspace-dir");
        workspaceFile = new File(workspaceDir, "workspace.json");
    }

    @BeforeMethod
    public void setUpMocks() {
        mockGrid = mock(Grid.class);
        when(mockGrid.rowCount()).thenReturn(14L);
    }

    protected class FileProjectManagerStub extends FileProjectManager {

        protected FileProjectManagerStub(File dir) {
            super(dir, mock(Runner.class));
            _projectsMetadata.put(5555L, mock(ProjectMetadata.class));
        }
    }

    @Test
    public void serializeFileProjectManager() {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        String json = "{\n" +
                "       \"preferences\" : {\n" +
                "         \"entries\" : {\n" +
                "           \"scripting.expressions\" : {\n" +
                "             \"class\" : \"org.openrefine.preference.TopList\",\n" +
                "             \"list\" : [ ],\n" +
                "             \"top\" : 100\n" +
                "           },\n" +
                "           \"scripting.starred-expressions\" : {\n" +
                "             \"class\" : \"org.openrefine.preference.TopList\",\n" +
                "             \"list\" : [ ],\n" +
                "             \"top\" : 2147483647\n" +
                "           }\n" +
                "         }\n" +
                "       },\n" +
                "       \"projectIDs\" : [ 5555 ]\n" +
                "     }";
        TestUtils.isSerializedTo(manager, json, ParsingUtilities.defaultWriter);
    }

    /**
     * Test that we can save and restore non-ASCII characters. For best effectiveness, this should be run with a
     * non-UTF8 default encoding for Java e.g. java -Dfile.encoding=cp1252 to simulate running on a Windows system
     */
    @Test
    public void saveReloadMultinationalCharacter() throws IOException {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        manager.getPreferenceStore().put("testPref", "Refiné");
        manager.saveWorkspace();
        manager = new FileProjectManagerStub(workspaceDir);
        assertEquals(manager.getPreferenceStore().get("testPref"), "Refiné");
    }

    /**
     * Issue fix Issue #1418 Issue #3719 Issue #3277 deleting the only existing project and saving the workspace should
     * remove the projectID from workspace.json
     */
    @Test
    public void deleteProjectAndSaveWorkspace() throws IOException {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        manager.saveWorkspace();
        // TODO: Test tag updating on project deletion
        manager.deleteProject(5555);
        manager.saveWorkspace();

        InputStream inputStream = new FileInputStream(workspaceFile);
        ObjectNode json = (ObjectNode) ParsingUtilities.mapper.readTree(inputStream);
        assertTrue(json.get("projectIDs").isEmpty(), "deleted project still in workspace.json");
    }

    /**
     * Tests whether only meta files of modified projects will be updated locally.
     */
    @Test
    public void metaFileUpdateTest() throws InterruptedException, GetProjectIDException {
        FileProjectManager manager = new FileProjectManager(workspaceDir, mock(Runner.class));
        ProjectMetadata metaA = new ProjectMetadata();
        ProjectMetadata metaB = new ProjectMetadata();
        metaA.setName("A");
        metaB.setName("B");
        Project projA = mock(Project.class);
        when(projA.getId()).thenReturn(1234L);
        when(projA.getCurrentGrid()).thenReturn(mockGrid);
        Project projB = mock(Project.class);
        when(projB.getId()).thenReturn(5678L);
        when(projB.getCurrentGrid()).thenReturn(mockGrid);
        manager.registerProject(projA, metaA);
        manager.registerProject(projB, metaB);
        manager.saveWorkspace();
        long idA = manager.getProjectID("A");
        long idB = manager.getProjectID("B");

        Path pathA = Paths.get(manager.getProjectDir(idA).getAbsolutePath(), ProjectMetadata.DEFAULT_FILE_NAME);
        Path pathB = Paths.get(manager.getProjectDir(idB).getAbsolutePath(), ProjectMetadata.DEFAULT_FILE_NAME);
        File metaAFile = pathA.toFile();
        File metaBFile = pathB.toFile();
        long timeBeforeA = metaAFile.lastModified();
        long timeBeforeB = metaBFile.lastModified();
        // Reload fresh copy of the workspace
        manager = new FileProjectManager(workspaceDir, mock(Runner.class));
        Thread.sleep(1000);
        manager.getProjectMetadata(idA).setName("ModifiedA");
        manager.saveWorkspace();
        long timeAfterA = metaAFile.lastModified();
        long timeAfterB = metaBFile.lastModified();
        assertEquals(timeBeforeB, timeAfterB, "Unmodified project written when it didn't need to be");
        assertNotEquals(timeBeforeA, timeAfterA, "Modified project not written");
    }

    @Test
    public void testUntarZipSlip() throws IOException {
        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);

        File tempDir = TestUtils.createTempDirectory("openrefine-project-import-zip-slip-test");
        try (InputStream stream = FileProjectManagerTests.class.getClassLoader().getResourceAsStream("zip-slip.tar")) {
            File subDir = new File(tempDir, "dest");
            assertThrows(IllegalArgumentException.class, () -> manager.untar(subDir, stream));
        } finally {
            tempDir.delete();
        }
    }

    @Test
    public void testSave() throws IOException {
        Project proj = mock(Project.class);
        when(proj.getId()).thenReturn(1234L);
        when(proj.getCurrentGrid()).thenReturn(mockGrid);
        History history = mock(History.class);
        when(proj.getHistory()).thenReturn(history);
        Grid initialGrid = mock(Grid.class);
        when(history.getEntries()).thenReturn(Collections.emptyList());
        when(history.getPosition()).thenReturn(0);
        when(history.getInitialGrid()).thenReturn(initialGrid);
        VoidFuture future = mock(VoidFuture.class);
        when(initialGrid.saveToFileAsync(any())).thenReturn(future);
        ProjectMetadata meta = new ProjectMetadata();
        meta.setName("some project");

        FileProjectManager manager = new FileProjectManagerStub(workspaceDir);
        manager.registerProject(proj, meta);

        manager.saveProject(proj, null);

        verify(proj, times(1)).setLastSave();
        verify(initialGrid, times(1)).saveToFileAsync(any());
    }

    private static interface VoidFuture extends ProgressingFuture<Void> {

    }
}
