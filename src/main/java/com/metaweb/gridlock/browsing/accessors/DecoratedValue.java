package com.metaweb.gridlock.browsing.accessors;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class DecoratedValue {
	final public Object value;
	final public String	label;
	
	public DecoratedValue(Object value, String label) {
		this.value = value;
		this.label = label;
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("v", value);
		o.put("l", label);
		
		return o;
	}
}
