package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;

public class Cell implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = -5891067829205458102L;
	
	final public Object value;
	final public Recon  recon;
	
	public Cell(Object value, Recon recon) {
		this.value = value;
		this.recon = recon;
	}
	
	public Object getField(String name, Properties bindings) {
		if ("value".equals(name)) {
			return value;
		} else if ("recon".equals(name)) {
			return recon;
		}
		return null;
	}

	public void write(JSONWriter writer, Properties options) throws JSONException {
		writer.object();
		writer.key("v");
		writer.value(value);
		
		if (recon != null) {
			writer.key("r");
			recon.write(writer, options);
		}
		writer.endObject();
	}
}
