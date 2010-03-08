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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import com.codeberry.jdatapath.DataPath;
import com.codeberry.jdatapath.JDataPathSystem;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.JSONUtilities;

public class ProjectManager {
    
    private static final int s_expressionHistoryMax = 100; // last n expressions used across all projects
    
    protected File                       _workspaceDir;
    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected List<String> 				 _expressions;
    
    transient protected Map<Long, Project>  _projects;
    
    static public ProjectManager singleton;
    
    static public void initialize() {
        if (singleton == null) {
            File dir = getProjectLocation();
            Gridworks.log("Using workspace directory: " + dir.getAbsolutePath());
            
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
            // NOTE(SM): finding the "local data app" in windows from java is actually a PITA
            // see http://stackoverflow.com/questions/1198911/how-to-get-local-application-data-folder-in-java
            // so we're using a library that uses JNI to ask directly the win32 APIs, 
            // it's not elegant but it's the safest bet
            DataPath localDataPath = JDataPathSystem.getLocalSystem().getLocalDataPath("Gridworks");
            File data = new File(localDataPath.getPath());
            data.mkdirs();
            return data;           
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
    
    private ProjectManager(File dir) {
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
    
    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        synchronized (this) {
            _projects.put(project.id, project);
            _projectsMetadata.put(project.id, projectMetadata);
        }
    }
    
    public void importProject(long projectID) {
        synchronized (this) {
            ProjectMetadata metadata = ProjectMetadata.load(getProjectDir(projectID));
        
            _projectsMetadata.put(projectID, metadata);
        }
    }
    
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
    
    public Map<Long, ProjectMetadata> getAllProjectMetadata() {
        return _projectsMetadata;
    }
    
    public Project getProject(long id) {
        if (_projects.containsKey(id)) {
            return _projects.get(id);
        } else {
            Project project = Project.load(getProjectDir(id), id);
            
            _projects.put(id, project);
            
            return project;
        }
    }
    
    public void addLatestExpression(String s) {
    	_expressions.remove(s);
    	_expressions.add(0, s);
    	while (_expressions.size() > s_expressionHistoryMax) {
    		_expressions.remove(_expressions.size() - 1);
    	}
    }
    
    public List<String> getExpressions() {
    	return _expressions;
    }
    
    public void save(boolean allModified) {
        saveProjects(allModified);
        saveWorkspace();
    }
    
    protected void saveWorkspace() {
        synchronized (this) {
        	File tempFile = new File(_workspaceDir, "workspace.temp.json");
        	try {
                saveToFile(tempFile);
            } catch (Exception e) {
                e.printStackTrace();
                
                Gridworks.log("Failed to save workspace.");
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
            
            Gridworks.log("Saved workspace.");
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
                    jsonWriter.value(id);
                    
                    ProjectMetadata metadata = _projectsMetadata.get(id);
                    try {
                        metadata.save(getProjectDir(id));
                    } catch (Exception e) {
                        e.printStackTrace();
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
    
    static protected class SaveRecord {
        final Project project;
        final long overdue;
        
        SaveRecord(Project project, long overdue) {
            this.project = project;
            this.overdue = overdue;
        }
    }
    
    protected void saveProjects(boolean allModified) {
        List<SaveRecord> records = new ArrayList<SaveRecord>();
        Date now = new Date();
        
        synchronized (this) {
            for (long id : _projectsMetadata.keySet()) {
                ProjectMetadata metadata = _projectsMetadata.get(id);
                Project project = _projects.get(id);
                
                if (project != null) {
                    boolean hasUnsavedChanges = metadata.getModified().getTime() > project.lastSave.getTime();
                    
                    if (hasUnsavedChanges) {
                        long msecsOverdue = now.getTime() - project.lastSave.getTime();
                        
                        records.add(new SaveRecord(project, msecsOverdue));
                        
                    } else if (now.getTime() - project.lastSave.getTime() > 1000 * 60 * 60) {
                        /*
                         *  It's been 1 hour since the project was last saved and it hasn't
                         *  been modified. We can safely remove it from the cache to save some memory.
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
            
            Gridworks.log(allModified ?
                "Saving all modified projects ..." :
                "Saving some modified projects ..."
            );
            
            for (int i = 0; 
                 i < records.size() && 
                    (allModified || (new Date().getTime() - now.getTime() < 30000)); i++) {
                
                try {
                    records.get(i).project.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void deleteProject(Project project) {
        synchronized (this) {
            if (_projectsMetadata.containsKey(project.id)) {
                _projectsMetadata.remove(project.id);
            }
            if (_projects.containsKey(project.id)) {
                _projects.remove(project.id);
            }
            
            File dir = getProjectDir(project.id);
            if (dir.exists()) {
                dir.delete();
            }
        }
        
        saveWorkspace();
    }
    
    protected void load() {
        try {
            loadFromFile(new File(_workspaceDir, "workspace.json"));
            return;
        } catch (Exception e) {
        }
        
        try {
            loadFromFile(new File(_workspaceDir, "workspace.temp.json"));
            return;
        } catch (Exception e) {
        }
        
        try {
            loadFromFile(new File(_workspaceDir, "workspace.old.json"));
            return;
        } catch (Exception e) {
        }
    }
    
    protected void loadFromFile(File file) throws IOException, JSONException {
        Gridworks.log("Loading workspace from " + file.getAbsolutePath());
        
        _projectsMetadata.clear();
        _expressions.clear();
        
        FileReader reader = new FileReader(file);
        try {
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
        } finally {
            reader.close();
        }
    }
}
