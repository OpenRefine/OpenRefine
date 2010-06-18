package com.metaweb.gridworks;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.tools.tar.TarOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.history.HistoryEntryManager;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.preference.PreferenceStore;
import com.metaweb.gridworks.preference.TopList;


public abstract class ProjectManager {
    // last n expressions used across all projects
    static protected final int s_expressionHistoryMax = 100;

    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected PreferenceStore            _preferenceStore;

    final static Logger logger = LoggerFactory.getLogger("project_manager");

    /**
     *  What caches the joins between projects.
     */
    transient protected InterProjectModel _interProjectModel = new InterProjectModel();

    /**
     *  Flags
     */
    transient protected int _busy = 0; // heavy operations like creating or importing projects are going on

    /**
     *  While each project's metadata is loaded completely at start-up, each project's raw data
     *  is loaded only when the project is accessed by the user. This is because project
     *  metadata is tiny compared to raw project data. This hash map from project ID to project
     *  is more like a last accessed-last out cache.
     */
    transient protected Map<Long, Project> _projects;

    static public ProjectManager singleton;

    public InterProjectModel getInterProjectModel() {
        return _interProjectModel;
    }

    /**
     * Registers the project in the memory of the current session
     * @param project
     * @param projectMetadata
     */
    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        synchronized (this) {
            _projects.put(project.id, project);
            _projectsMetadata.put(project.id, projectMetadata);
        }
    }

    /**
     * Load project from data storage
     * @param projectID
     * @return
     */
    public abstract boolean importProject(long projectID);

    /**
     * Import project from a Gridworks archive
     * @param projectID
     * @param inputStream
     * @param gziped
     * @throws IOException
     */
    public abstract void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException;

    /**
     * Export project to a Gridworks archive
     * @param projectId
     * @param tos
     * @throws IOException
     */
    public abstract void exportProject(long projectId, TarOutputStream tos) throws IOException;

    /**
     *
     * @param id
     */
    public abstract void ensureProjectSaved(long id);

    public ProjectMetadata getProjectMetadata(long id) {
        return _projectsMetadata.get(id);
    }

    /**
     *
     * @param name
     * @return
     */
    public ProjectMetadata getProjectMetadata(String name) {
        for (ProjectMetadata pm : _projectsMetadata.values()) {
            if (pm.getName().equals(name)) {
                return pm;
            }
        }
        return null;
    }

    /**
     * Tries to find the project id when given a project name
     * @param name
     *     The name of the project
     * @return
     *     The id of the project, or -1 if it cannot be found
     */
    public long getProjectID(String name) {
        for (Entry<Long, ProjectMetadata> entry : _projectsMetadata.entrySet()) {
            if (entry.getValue().getName().equals(name)) {
                return entry.getKey();
            }
        }
        return -1;
    }


    public Map<Long, ProjectMetadata> getAllProjectMetadata() {
        return _projectsMetadata;
    }

    /**
     * Gets the required project from the data store
     * @param id
     *     the id of the project
     * @return
     *     the project with the matching id, or null if it can't be found
     */
    public Project getProject(long id) {
        synchronized (this) {
            if (_projects.containsKey(id)) {
                return _projects.get(id);
            } else {
                Project project = loadProject(id);

                _projects.put(id, project);

                return project;
            }
        }
    }

    protected abstract Project loadProject(long id);

    /**
     * Sets the flag for long running operations
     * @param busy
     */
    public void setBusy(boolean busy) {
        synchronized (this) {
            if (busy) {
                _busy++;
            } else {
                _busy--;
            }
        }
    }

    public PreferenceStore getPreferenceStore() {
        return _preferenceStore;
    }

    public void addLatestExpression(String s) {
        synchronized (this) {
            ((TopList) _preferenceStore.get("expressions")).add(s);
        }
    }

    public List<String> getExpressions() {
        return ((TopList) _preferenceStore.get("expressions")).getList();
    }

    /**
     * Save project to data store
     * @param b
     */
    public abstract void save(boolean b);

    /**
     * Remove the project from the data store
     * @param project
     */
    public void deleteProject(Project project) {
        deleteProject(project.id);
    }

    /**
     * Remove project from data store
     * @param projectID
     */
    public abstract void deleteProject(long projectID);

    protected void removeProject(long projectID){
        if (_projectsMetadata.containsKey(projectID)) {
            _projectsMetadata.remove(projectID);
        }
        if (_projects.containsKey(projectID)) {
            _projects.remove(projectID);
        }
    }

    /**
     * The history entry manager deals with changes
     * @return manager for handling history
     */
    public abstract HistoryEntryManager getHistoryEntryManager();

    static protected void preparePreferenceStore(PreferenceStore ps) {
        ps.put("expressions", new TopList(s_expressionHistoryMax));
    }
}
