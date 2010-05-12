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
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeberry.jdatapath.DataPath;
import com.codeberry.jdatapath.JDataPathSystem;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.JSONUtilities;

public class ProjectManager {
    
    // last n expressions used across all projects
    static protected final int s_expressionHistoryMax = 100;
    
    protected File                       _workspaceDir;
    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected List<String>               _expressions;
    
    final static Logger logger = LoggerFactory.getLogger("project_manager");
    
    /**
     *  While each project's metadata is loaded completely at start-up, each project's raw data 
     *  is loaded only when the project is accessed by the user. This is because project
     *  metadata is tiny compared to raw project data. This hash map from project ID to project
     *  is more like a last accessed-last out cache.
     */
    transient protected Map<Long, Project> _projects;
    
    /**
     *  What caches the joins between projects.
     */
    transient protected InterProjectModel _interProjectModel = new InterProjectModel();
    
    /**
     *  Flags
     */
    transient protected int _busy = 0; // heavy operations like creating or importing projects are going on 
    
    static public ProjectManager singleton;
    
    static public synchronized void initialize() {
        if (singleton == null) {
            File dir = getProjectLocation();
            logger.info("Using workspace directory: {}", dir.getAbsolutePath());
            
            singleton = new ProjectManager(dir);
        }
    }
    
    static protected File getProjectLocation() {
        String data_dir = Configurations.get("gridworks.data_dir");
        if (data_dir != null) {
            return new File(data_dir);
        }
        
        String os = Configurations.get("os.name").toLowerCase();
        if (os.contains("windows")) {
            try {
                // NOTE(SM): finding the "local data app" in windows from java is actually a PITA
                // see http://stackoverflow.com/questions/1198911/how-to-get-local-application-data-folder-in-java
                // so we're using a library that uses JNI to ask directly the win32 APIs, 
                // it's not elegant but it's the safest bet.
                
                DataPath localDataPath = JDataPathSystem.getLocalSystem().getLocalDataPath("Gridworks");
                File data = new File(fixWindowsUnicodePath(localDataPath.getPath()));
                data.mkdirs();
                return data;
            } catch (Error e) {
                /*
                 *  The above trick can fail, particularly on a 64-bit OS as the jdatapath.dll
                 *  we include is compiled for 32-bit. In this case, we just have to dig up
                 *  environment variables and try our best to find a user-specific path.
                 */
                
                logger.warn("Failed to use jdatapath to detect user data path: resorting to environment variables");
                
                File parentDir = null;
                {
                    String appData = System.getenv("APPDATA"); 
                    if (appData != null && appData.length() > 0) {
                        // e.g., C:\Users\[userid]\AppData\Roaming
                        parentDir = new File(appData);
                    } else {
                        String userProfile = System.getenv("USERPROFILE");
                        if (userProfile != null && userProfile.length() > 0) {
                            // e.g., C:\Users\[userid]
                            parentDir = new File(userProfile);
                        }
                    }
                }
                if (parentDir == null) {
                    parentDir = new File(".");
                }
                
                File data = new File(parentDir, "Gridworks");
                data.mkdirs();
                
                return data;
            }
        } else if (os.contains("mac os x")) {
            // on macosx, use "~/Library/Application Support"
            String home = System.getProperty("user.home");
            String data_home = (home != null) ? home + "/Library/Application Support/Gridworks" : ".gridworks"; 
            File data = new File(data_home);
            data.mkdirs();
            return data;
        } else { // most likely a UNIX flavor
            // start with the XDG environment
            // see http://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
            String data_home = System.getenv("XDG_DATA_HOME");
            if (data_home == null) { // if not found, default back to ~/.local/share
                String home = System.getProperty("user.home");
                if (home == null) home = ".";
                data_home = home + "/.local/share";
            }
            File data = new File(data_home + "/gridworks");
            data.mkdirs();
            return data;
        }
    }
    
    /**
     * For Windows file paths that contain user IDs with non ASCII characters,
     * those characters might get replaced with ?. We need to use the environment
     * APPDATA value to substitute back the original user ID.
     */
    static protected String fixWindowsUnicodePath(String path) {
        int q = path.indexOf('?');
        if (q < 0) {
            return path;
        }
        int pathSep = path.indexOf(File.separatorChar, q);
        
        String goodPath = System.getenv("APPDATA");
        if (goodPath == null || goodPath.length() == 0) {
            goodPath = System.getenv("USERPROFILE");
            if (!goodPath.endsWith(File.separator)) {
                goodPath = goodPath + File.separator;
            }
        }
        
        int goodPathSep = goodPath.indexOf(File.separatorChar, q);
        
        return path.substring(0, q) + goodPath.substring(q, goodPathSep) + path.substring(pathSep);
    }
    
    private ProjectManager(File dir) {
        _workspaceDir = dir;
        _workspaceDir.mkdirs();
        
        _projectsMetadata = new HashMap<Long, ProjectMetadata>();
        _expressions = new LinkedList<String>();
        _projects = new HashMap<Long, Project>();
        
        load();
    }
    
    public InterProjectModel getInterProjectModel() {
        return _interProjectModel;
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
    
    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        synchronized (this) {
            _projects.put(project.id, project);
            _projectsMetadata.put(project.id, projectMetadata);
        }
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
    
    public void deleteProject(Project project) {
        deleteProject(project.id);
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
