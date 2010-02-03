package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class Column implements Serializable, Jsonizable {
	private static final long serialVersionUID = -1063342490951563563L;
	
	public int		cellIndex;
	public String	headerLabel;
	public Class 	valueType;
	
	transient protected Map<String, Object> _precomputes;
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("cellIndex"); writer.value(cellIndex);
		writer.key("headerLabel"); writer.value(headerLabel);
		writer.key("valueType"); writer.value(valueType == null ? null : valueType.getSimpleName());
		writer.endObject();
	}
	
	public void clearPrecomputes() {
		if (_precomputes != null) {
			_precomputes.clear();
		}
	}
	
	public Object getPrecompute(String key) {
		if (_precomputes != null) {
			return _precomputes.get(key);
		}
		return null;
	}
	
	public void setPrecompute(String key, Object value) {
		if (_precomputes == null) {
			_precomputes = new HashMap<String, Object>();
		}
		_precomputes.put(key, value);
	}
}
