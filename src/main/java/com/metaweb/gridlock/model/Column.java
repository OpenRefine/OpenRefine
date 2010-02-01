package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;

public class Column implements Serializable, Jsonizable {
	private static final long serialVersionUID = -1063342490951563563L;
	
	public int		cellIndex;
	public String	headerLabel;
	public Class 	valueType;
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("cellIndex"); writer.value(cellIndex);
		writer.key("headerLabel"); writer.value(headerLabel);
		writer.key("valueType"); writer.value(valueType == null ? null : valueType.getSimpleName());
		writer.endObject();
	}
}
