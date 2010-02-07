package com.metaweb.gridworks;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

public class ProjectMetadata implements Serializable, Jsonizable {
	private static final long serialVersionUID = 7959027046468240844L;
	
	private final Date 	_created = new Date();
	private String 		_name;
	private String 		_password;
	private Date 		_modified = new Date();
	
	public Date getCreated() {
		return _created;
	}

	public void setName(String name) {
		this._name = name;
	}

	public String getName() {
		return _name;
	}

	public void setPassword(String password) {
		this._password = password;
	}

	public String getPassword() {
		return _password;
	}
	
	public Date getModified() {
		return _modified;
	}
	
	public void updateModified() {
		_modified = new Date();
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		SimpleDateFormat sdf = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
		
		writer.object();
		writer.key("name"); writer.value(getName());
		writer.key("created"); writer.value(sdf.format(getCreated()));
		writer.key("modified"); writer.value(sdf.format(_modified));
		writer.endObject();
	}
}
