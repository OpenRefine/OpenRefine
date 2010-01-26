package com.metaweb.gridlock;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class ProjectMetadata implements Serializable {
	private static final long serialVersionUID = 7959027046468240844L;
	
	public String name;
	public String password;
	
	public JSONObject getJSON() throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("name", name);
		
		return o;
	}
}
