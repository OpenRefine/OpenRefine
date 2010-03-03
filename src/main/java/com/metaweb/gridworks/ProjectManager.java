package com.metaweb.gridworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.codeberry.jdatapath.DataPath;
import com.codeberry.jdatapath.JDataPathSystem;
import com.metaweb.gridworks.model.Project;

public class ProjectManager implements Serializable {
    
    private static final long serialVersionUID = -2967415873336723962L;
    private static final int s_expressionHistoryMax = 100; // last n expressions used across all projects
    
    protected File _dir;
    protected Map<Long, ProjectMetadata> _projectsMetadata;
    protected List<String> 				 _expressions;
    
    transient protected Map<Long, Project> _projects;
    
    static public ProjectManager singleton;
    
    static public void initialize() {
        if (singleton == null) {
            File dir = getProjectLocation();
            Gridworks.log("Using data directory: " + dir.getAbsolutePath());
            File file = new File(dir, "projects");
            if (file.exists()) {
                singleton = load(file);
            } else {
                singleton = new ProjectManager(dir);
            }
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
    
    static protected ProjectManager load(File file) {
    	Gridworks.log("Loading project metadata from " + file.getAbsolutePath());
    	
        ProjectManager pm = null;
        FileInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = new FileInputStream(file);
            in = new ObjectInputStream(fis);
            
            pm = (ProjectManager) in.readObject();
        } catch(IOException e) {
            e.printStackTrace();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
        return pm;
    }
    
    private ProjectManager(File dir) {
        _dir = dir;
        _dir.mkdirs();
        
        _projectsMetadata = new HashMap<Long, ProjectMetadata>();
        _expressions = new LinkedList<String>();
        
        internalInitialize();
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        internalInitialize();
    }
    
    private void internalInitialize() {
        _projects = new HashMap<Long, Project>();
    }
    
    public File getDataDir() {
        return _dir;
    }
    
    public void registerProject(Project project, ProjectMetadata projectMetadata) {
        _projects.put(project.id, project);
        _projectsMetadata.put(project.id, projectMetadata);
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
            File file = new File(_dir, id + ".project");
            
            Project project = null;
            FileInputStream fis = null;
            ObjectInputStream in = null;
            try {
                fis = new FileInputStream(file);
                in = new ObjectInputStream(fis);
                
                project = (Project) in.readObject();
            } catch(IOException e) {
                e.printStackTrace();
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                    }
                }
            }
            
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
    
    public void save() {
    	Gridworks.log("Saving project metadata ...");
    	
        File file = new File(_dir, "projects");
        
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(file);
            out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
    
    public void saveAllProjects() {
    	Gridworks.log("Saving all projects ...");
        for (Project project : _projects.values()) {
        	try {
        		saveProject(project);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
    }
    
    protected void saveProject(Project project) {
        File file = new File(_dir, project.id + ".project");
        
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new FileOutputStream(file);
            out = new ObjectOutputStream(fos);
            out.writeObject(project);
            out.flush();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
