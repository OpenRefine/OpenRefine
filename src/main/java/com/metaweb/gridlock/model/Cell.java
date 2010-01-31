package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.expr.HasFields;

public class Cell implements Serializable, HasFields {
	private static final long serialVersionUID = -5891067829205458102L;
	
	public Object value;
	public Recon  recon;
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("v", value);
		if (recon != null && options.containsKey("cell-recon")) {
			o.put("recon", recon.getJSON(options));
		}
		
		return o;
	}

	@Override
	public Object getField(String name, Properties bindings) {
		if ("value".equals(name)) {
			return value;
		} else if ("recon".equals(name)) {
			return recon;
		}
		return null;
	}
}
