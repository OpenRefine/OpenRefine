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

package com.google.refine;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;

/**
 * Stub used to avoid saves and stub HistoryEntryManager
 *
 */
public class ProjectManagerStub extends ProjectManager {

    @Override
    public void deleteProject(long projectID) {
        // empty

    }

    @Override
    public void exportProject(long projectId, TarArchiveOutputStream tos) throws IOException {
        // empty
    }

    @Override
    public HistoryEntryManager getHistoryEntryManager() {
        return new HistoryEntryManagerStub();
    }

    @Override
    public void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException {
        // empty
    }

    @Override
    protected Project loadProject(long id) {
        // empty
        return null;
    }

    @Override
    public boolean loadProjectMetadata(long projectID) {
        // empty
        return false;
    }

    @Override
    public void saveMetadata(ProjectMetadata metadata, long projectId) throws Exception {
        // empty

    }

    @Override
    public void saveProject(Project project) {
        // empty
    }

    // Overridden to make public for testing
    @Override
    public void saveProjects(boolean allModified) {
        super.saveProjects(allModified);
    }

    @Override
    protected void saveWorkspace() {
        // empty
    }

}
