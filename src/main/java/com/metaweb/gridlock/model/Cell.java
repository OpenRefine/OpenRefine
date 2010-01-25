package com.metaweb.gridlock.model;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Cell implements Serializable {
	private static final long serialVersionUID = -5891067829205458102L;
	
	public Object value;
	public Recon  recon;
	
	public JSONObject getJSON() throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("v", value);
		
		return o;
	}
}
