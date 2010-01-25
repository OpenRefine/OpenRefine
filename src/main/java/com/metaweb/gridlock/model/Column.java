package com.metaweb.gridlock.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Column implements Serializable {
	private static final long serialVersionUID = -1063342490951563563L;
	
	public int		cellIndex;
	public String	headerLabel;
	public Class 	valueType;
	
	public JSONObject getJSON() throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("cellIndex", cellIndex);
		o.put("headerLabel", headerLabel);
		o.put("valueType", valueType == null ? null : valueType.getSimpleName());
		
		return o;
	}
}
