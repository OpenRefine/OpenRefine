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


public abstract class ProjectManager {
    // last n expressions used across all projects
    static protected final int s_expressionHistoryMax = 100;

    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected List<String>               _expressions;

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

    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        synchronized (this) {
            _projects.put(project.id, project);
            _projectsMetadata.put(project.id, projectMetadata);
        }
    }

    public abstract boolean importProject(long projectID);

    public abstract void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException;

    public abstract void exportProject(long projectId, TarOutputStream tos) throws IOException;

    public abstract void ensureProjectSaved(long id);

    public ProjectMetadata getProjectMetadata(long id) {
        return _projectsMetadata.get(id);
    }

    public ProjectMetadata getProjectMetadata(String name) {
        for (ProjectMetadata pm : _projectsMetadata.values()) {
            if (pm.getName().equals(name)) {
                return pm;
            }
        }
        return null;
    }

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

    public abstract Project getProject(long parseLong);

    public void setBusy(boolean busy) {
        synchronized (this) {
            if (busy) {
                _busy++;
            } else {
                _busy--;
            }
        }
    }

    public void addLatestExpression(String s) {
        synchronized (this) {
            _expressions.remove(s);
            _expressions.add(0, s);
            while (_expressions.size() > s_expressionHistoryMax) {
                _expressions.remove(_expressions.size() - 1);
            }
        }
    }

    public List<String> getExpressions() {
        return _expressions;
    }

    public abstract void save(boolean b);

    public void deleteProject(Project project) {
        deleteProject(project.id);
    }

    public abstract void deleteProject(long projectID) ;

    public abstract HistoryEntryManager getHistoryEntryManager();
}
