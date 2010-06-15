package com.metaweb.gridworks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.JSONUtilities;

public class FileProjectManager extends ProjectManager{

    protected File                       _workspaceDir;

    final static Logger logger = LoggerFactory.getLogger("file_project_manager");

    static public synchronized void initialize(File dir) {
        if (singleton == null) {
            logger.info("Using workspace directory: {}", dir.getAbsolutePath());
            singleton = new FileProjectManager(dir);
        }
    }

    private FileProjectManager(File dir) {
        _workspaceDir = dir;
        _workspaceDir.mkdirs();

        _projectsMetadata = new HashMap<Long, ProjectMetadata>();
        _expressions = new LinkedList<String>();
        _projects = new HashMap<Long, Project>();

        load();
    }

    public File getWorkspaceDir() {
        return _workspaceDir;
    }

    static public File getProjectDir(File workspaceDir, long projectID) {
        File dir = new File(workspaceDir, projectID + ".project");
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    public File getProjectDir(long projectID) {
        return getProjectDir(_workspaceDir, projectID);
    }



    /**
     * Import an external project that has been received as a .tar file, expanded, and
     * copied into our workspace directory.
     *
     * @param projectID
     */
    public boolean importProject(long projectID) {
        synchronized (this) {
            ProjectMetadata metadata = ProjectMetadata.load(getProjectDir(projectID));
            if (metadata != null) {
                _projectsMetadata.put(projectID, metadata);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Make sure that a project's metadata and data are saved to file. For example,
     * this method is called before the project is exported to a .tar file.
     *
     * @param id
     */
    public void ensureProjectSaved(long id) {
        synchronized (this) {
            File projectDir = getProjectDir(id);

            ProjectMetadata metadata = _projectsMetadata.get(id);
            if (metadata != null) {
                try {
                    metadata.save(projectDir);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Project project = _projects.get(id);
            if (project != null && metadata.getModified().after(project.lastSave)) {
                try {
                    project.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Project getProject(long id) {
        synchronized (this) {
            if (_projects.containsKey(id)) {
                return _projects.get(id);
            } else {
                Project project = Project.load(getProjectDir(id), id);

                _projects.put(id, project);

                return project;
            }
        }
    }

    public void save(boolean allModified) {
        if (allModified || _busy == 0) {
            saveProjects(allModified);
            saveWorkspace();
        }
    }

    /**
     * Save the workspace's data out to file in a safe way: save to a temporary file first
     * and rename it to the real file.
     */
    protected void saveWorkspace() {
        synchronized (this) {
            File tempFile = new File(_workspaceDir, "workspace.temp.json");
            try {
                saveToFile(tempFile);
            } catch (Exception e) {
                e.printStackTrace();

                logger.warn("Failed to save workspace");
                return;
            }

            File file = new File(_workspaceDir, "workspace.json");
            File oldFile = new File(_workspaceDir, "workspace.old.json");

            if (file.exists()) {
                file.renameTo(oldFile);
            }

            tempFile.renameTo(file);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            logger.info("Saved workspace");
        }
    }

    protected void saveToFile(File file) throws IOException, JSONException {
        FileWriter writer = new FileWriter(file);
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object();
            jsonWriter.key("projectIDs");
                jsonWriter.array();
                for (Long id : _projectsMetadata.keySet()) {
                    ProjectMetadata metadata = _projectsMetadata.get(id);
                    if (metadata != null) {
                        jsonWriter.value(id);

                        try {
                            metadata.save(getProjectDir(id));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                jsonWriter.endArray();
                writer.write('\n');

            jsonWriter.key("expressions"); JSONUtilities.writeStringList(jsonWriter, _expressions);
            jsonWriter.endObject();
        } finally {
            writer.close();
        }
    }

    /**
     * A utility class to prioritize projects for saving, depending on how long ago
     * they have been changed but have not been saved.
     */
    static protected class SaveRecord {
        final Project project;
        final long overdue;

        SaveRecord(Project project, long overdue) {
            this.project = project;
            this.overdue = overdue;
        }
    }

    static protected final int s_projectFlushDelay = 1000 * 60 * 60; // 1 hour
    static protected final int s_quickSaveTimeout = 1000 * 30; // 30 secs

    protected void saveProjects(boolean allModified) {
        List<SaveRecord> records = new ArrayList<SaveRecord>();
        Date now = new Date();

        synchronized (this) {
            for (long id : _projectsMetadata.keySet()) {
                ProjectMetadata metadata = _projectsMetadata.get(id);
                Project project = _projects.get(id);

                if (project != null) {
                    boolean hasUnsavedChanges =
                        metadata.getModified().getTime() > project.lastSave.getTime();

                    if (hasUnsavedChanges) {
                        long msecsOverdue = now.getTime() - project.lastSave.getTime();

                        records.add(new SaveRecord(project, msecsOverdue));

                    } else if (now.getTime() - project.lastSave.getTime() > s_projectFlushDelay) {
                        /*
                         *  It's been a while since the project was last saved and it hasn't been
                         *  modified. We can safely remove it from the cache to save some memory.
                         */
                        _projects.remove(id);
                    }
                }
            }
        }

        if (records.size() > 0) {
            Collections.sort(records, new Comparator<SaveRecord>() {
                public int compare(SaveRecord o1, SaveRecord o2) {
                    if (o1.overdue < o2.overdue) {
                        return 1;
                    } else if (o1.overdue > o2.overdue) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            });

            logger.info(allModified ?
                "Saving all modified projects ..." :
                "Saving some modified projects ..."
            );

            for (int i = 0;
                 i < records.size() &&
                    (allModified || (new Date().getTime() - now.getTime() < s_quickSaveTimeout));
                 i++) {

                try {
                    records.get(i).project.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void deleteProject(long projectID) {
        synchronized (this) {
            if (_projectsMetadata.containsKey(projectID)) {
                _projectsMetadata.remove(projectID);
            }
            if (_projects.containsKey(projectID)) {
                _projects.remove(projectID);
            }

            File dir = getProjectDir(projectID);
            if (dir.exists()) {
                deleteDir(dir);
            }
        }

        saveWorkspace();
    }

    static protected void deleteDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    protected void load() {
        if (loadFromFile(new File(_workspaceDir, "workspace.json"))) return;
        if (loadFromFile(new File(_workspaceDir, "workspace.temp.json"))) return;
        if (loadFromFile(new File(_workspaceDir, "workspace.old.json"))) return;
    }

    protected boolean loadFromFile(File file) {
        logger.info("Loading workspace: {}", file.getAbsolutePath());

        _projectsMetadata.clear();
        _expressions.clear();

        boolean found = false;

        if (file.exists() || file.canRead()) {
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                JSONTokener tokener = new JSONTokener(reader);
                JSONObject obj = (JSONObject) tokener.nextValue();

                JSONArray a = obj.getJSONArray("projectIDs");
                int count = a.length();
                for (int i = 0; i < count; i++) {
                    long id = a.getLong(i);

                    File projectDir = getProjectDir(id);
                    ProjectMetadata metadata = ProjectMetadata.load(projectDir);

                    _projectsMetadata.put(id, metadata);
                }

                JSONUtilities.getStringList(obj, "expressions", _expressions);
                found = true;
            } catch (JSONException e) {
                logger.warn("Error reading file", e);
            } catch (IOException e) {
                logger.warn("Error reading file", e);
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Exception closing file",e);
                }
            }
        }

        return found;
    }
}
