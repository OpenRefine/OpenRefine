package com.metaweb.gridworks.browsing;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class DecoratedValue implements Jsonizable {
	final public Object value;
	final public String	label;
	
	public DecoratedValue(Object value, String label) {
		this.value = value;
		this.label = label;
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		writer.object();
		writer.key("v"); writer.value(value);
		writer.key("l"); writer.value(label);
		writer.endObject();
	}
}
