package com.metaweb.gridlock.history;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.ProjectManager;
import com.metaweb.gridlock.model.Project;

public class HistoryEntry implements Serializable {
	private static final long serialVersionUID = 532766467813930262L;
	
	public long 	id;
	public long 	projectID;
	public String	description;
	public Date		time;
	
	transient protected Change _change;
	
	public HistoryEntry(Project project, String description, Change change) {
		this.id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
		this.projectID = project.id;
		this.description = description;
		this.time = new Date();
		
		_change = change;
		
		saveChange();
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		
		o.put("id", id);
		o.put("description", description);
		o.put("time", sdf.format(time));
		
		return o;
	}
	
	public void apply(Project project) {
		if (_change == null) {
			loadChange();
		}
		_change.apply(project);
	}
	
	public void revert(Project project) {
		if (_change == null) {
			loadChange();
		}
		_change.revert(project);
	}
	
	public void delete() {
		File file = getFile();
		if (file.exists()) {
			file.delete();
		}
	}
	
	protected void loadChange() {
		File file = getFile();
		
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			
			_change = (Change) in.readObject();
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
	}
	
	protected void saveChange() {
		File file = getFile();
		
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(file);
			out = new ObjectOutputStream(fos);
			
			out.writeObject(_change);
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
	
	protected File getFile() {
		File dir = new File(ProjectManager.singleton.getDataDir(), projectID + ".history");
		dir.mkdirs();
		
		return new File(dir, id + ".entry");
	}
}
