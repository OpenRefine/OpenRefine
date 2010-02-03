package com.metaweb.gridworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.metaweb.gridworks.model.Project;

public class ProjectManager implements Serializable {
	private static final long serialVersionUID = -2967415873336723962L;
	
	protected File _dir;
	protected Map<Long, ProjectMetadata> _projectsMetadata;
	
	transient protected Map<Long, Project> _projects;
	
	static public ProjectManager singleton;
	
	static public void initialize(File dir) {
		if (singleton == null) {
			File file = new File(dir, "projects");
			if (file.exists()) {
				singleton = load(file);
			}
			if (singleton == null) {
				singleton = new ProjectManager(dir);
			}
		}
	}
	
	static protected ProjectManager load(File file) {
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
	
	public Project createProject(ProjectMetadata projectMetadata) {
		Project project = new Project();
		
		_projects.put(project.id, project);
		_projectsMetadata.put(project.id, projectMetadata);
		
		return project;
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
	
	public void save() {
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
		for (Project project : _projects.values()) {
			saveProject(project);
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
