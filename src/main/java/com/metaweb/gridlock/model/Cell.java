package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;
import com.metaweb.gridlock.expr.HasFields;

public class Cell implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = -5891067829205458102L;
	
	public Object value;
	public Recon  recon;
	
	@Override
	public Object getField(String name, Properties bindings) {
		if ("value".equals(name)) {
			return value;
		} else if ("recon".equals(name)) {
			return recon;
		}
		return null;
	}

	@Override
	public void write(JSONWriter writer, Properties options) throws JSONException {
		writer.object();
		writer.key("v");
		writer.value(value);
		
		if (recon != null && options.containsKey("cell-recon")) {
			writer.key("recon");
			recon.write(writer, options);
		}
		writer.endObject();
	}
}
