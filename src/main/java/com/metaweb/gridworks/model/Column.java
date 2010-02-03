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
	
	final private int		_cellIndex;
	final private String	_originalHeaderLabel;
	private String			_headerLabel;
	
	transient protected Map<String, Object> _precomputes;
	
	public Column(int cellIndex, String headerLabel) {
		_cellIndex = cellIndex;
		_originalHeaderLabel = _headerLabel = headerLabel;
	}
	
	public int getCellIndex() {
		return _cellIndex;
	}

	public String getOriginalHeaderLabel() {
		return _originalHeaderLabel;
	}
	
	public void setHeaderLabel(String headerLabel) {
		this._headerLabel = headerLabel;
	}

	public String getHeaderLabel() {
		return _headerLabel;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("cellIndex"); writer.value(getCellIndex());
		writer.key("headerLabel"); writer.value(getHeaderLabel());
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
