package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class ColumnModel implements Serializable {
	private static final long serialVersionUID = 7679639795211544511L;
	
	public List<Column> columns = new LinkedList<Column>();
	
	public JSONObject getJSON() throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(columns.size());
		for (Column column : columns) {
			a.add(column.getJSON());
		}
		o.put("columns", a);
		
		return o;
	}
}
