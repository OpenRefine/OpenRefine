package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;

public class ColumnModel implements Serializable, Jsonizable {
	private static final long serialVersionUID = 7679639795211544511L;
	
	public List<Column> columns = new LinkedList<Column>();
	
	transient protected Map<String, Column> _nameToColumn;
	
	public Column getColumnByName(String name) {
		if (_nameToColumn == null) {
			for (Column column : columns) {
				_nameToColumn.put(column.headerLabel, column);
			}
		}
		return _nameToColumn.get(name);
	}
	
	public Column getColumnByCellIndex(int cellIndex) {
		for (Column column : columns) {
			if (column.cellIndex == cellIndex) {
				return column;
			}
		}
		return null;
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("columns");
		writer.array();
		for (Column column : columns) {
			column.write(writer, options);
		}
		writer.endArray();
		writer.endObject();
	}
}
