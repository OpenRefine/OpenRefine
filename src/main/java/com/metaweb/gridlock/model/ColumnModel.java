package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class ColumnModel implements Serializable {
	private static final long serialVersionUID = 7679639795211544511L;
	
	public List<Column> columns = new LinkedList<Column>();
	
	transient protected Map<String, Column> _nameToColumn;
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(columns.size());
		for (Column column : columns) {
			a.add(column.getJSON(options));
		}
		o.put("columns", a);
		
		return o;
	}
	
	public Column getColumnByName(String name) {
		if (_nameToColumn == null) {
			for (Column column : columns) {
				_nameToColumn.put(column.headerLabel, column);
			}
		}
		return _nameToColumn.get(name);
	}
}
