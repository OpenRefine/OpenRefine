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

package org.openrefine.model;

import java.time.Instant;
import java.util.Map;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.history.History;
import org.openrefine.model.changes.GridCache;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.overlay.OverlayModelResolver;
import org.openrefine.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A project is a table with overlay models and full edit history. This is the central concept of the OpenRefine data
 * model - most operations are done within a given project.
 */
public class Project {

    // The project identifier, assigned at creation
    private final long id;
    // The complete history of transformations executed on this project
    private final History history;

    // by default, the project has never been saved so far.
    transient private Instant _lastSave = Instant.parse("1970-01-02T00:30:00Z");

    final static Logger logger = LoggerFactory.getLogger(Project.class);

    /**
     * Creates a new project from an initial grid.
     * 
     * @param initialState
     *            the initial state of the project.
     */
    public Project(Grid initialState, ChangeDataStore dataStore, GridCache gridStore) {
        this(generateID(), initialState, dataStore, gridStore);
    }

    /**
     * Creates a new project from an initial grid and a project id.
     * 
     * @param projectId
     *            the project identifier
     * @param initialState
     *            the initial state of the project
     */
    public Project(long projectId, Grid initialState, ChangeDataStore dataStore, GridCache gridStore) {
        this(projectId, new History(initialState, dataStore, gridStore, projectId));
    }

    /**
     * Restores a project from a saved state.
     */
    public Project(
            long id,
            History history) {
        this.id = id;
        this.history = history;
    }

    public Instant getLastSave() {
        return this._lastSave;
    }

    /**
     * Sets the lastSave time to now
     */
    public void setLastSave() {
        this._lastSave = Instant.now();
    }

    public Instant getLastModified() {
        return history.getLastModified();
    }

    public ProjectMetadata getMetadata() {
        return ProjectManager.singleton.getProjectMetadata(id);
    }

    public ProcessManager getProcessManager() {
        return this.history.getChangeDataStore().getProcessManager();
    }

    public History getHistory() {
        return history;
    }

    static public long generateID() {
        return System.currentTimeMillis() + Math.round(Math.random() * 1000000000000L);
    }

    public long getId() {
        return id;
    }

    public void dispose() {
        history.dispose();
    }

    /**
     * Convenience function to return the current column model from the history.
     */
    public ColumnModel getColumnModel() {
        return history.getCurrentGrid().getColumnModel();
    }

    /**
     * Convenience function to return the current grid.
     */
    public Grid getCurrentGrid() {
        return history.getCurrentGrid();
    }

    /**
     * Convenience function to return the current overlay models
     */
    public Map<String, OverlayModel> getOverlayModels() {
        return history.getCurrentGrid().getOverlayModels();
    }

    /**
     * @deprecated use {@link org.openrefine.overlay.OverlayModelResolver}
     */
    @Deprecated
    public static void registerOverlayModel(String name, Class<? extends OverlayModel> klass) {
        OverlayModelResolver.registerOverlayModel(name, klass);
    }
}
