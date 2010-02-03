package com.metaweb.gridworks.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class ColumnModel implements Serializable, Jsonizable {
	private static final long serialVersionUID = 7679639795211544511L;
	
	final public List<Column> 		columns = new LinkedList<Column>();
	final public List<ColumnGroup> 	columnGroups = new LinkedList<ColumnGroup>();
	
	private int					_maxCellIndex;
	private int					_keyCellIndex;
	
	transient protected Map<String, Column> 	_nameToColumn;
	transient protected Map<Integer, Column> 	_cellIndexToColumn;
	transient protected List<ColumnGroup>		_rootColumnGroups;
	
	public ColumnModel() {
		internalInitialize();
	}
	
	public void setMaxCellIndex(int maxCellIndex) {
		this._maxCellIndex = Math.max(this._maxCellIndex, maxCellIndex);
	}

	public int getMaxCellIndex() {
		return _maxCellIndex;
	}

	public void setKeyCellIndex(int keyCellIndex) {
		// TODO: check validity of new cell index, e.g., it's not in any group
		this._keyCellIndex = keyCellIndex;
	}

	public int getKeyCellIndex() {
		return _keyCellIndex;
	}

	public void update() {
		generateMaps();
	}
	
	public Column getColumnByName(String name) {
		return _nameToColumn.get(name);
	}
	
	public Column getColumnByCellIndex(int cellIndex) {
		return _cellIndexToColumn.get(cellIndex);
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
		
		writer.key("keyCellIndex"); writer.value(getKeyCellIndex());
		writer.key("columnGroups");
		writer.array();
		for (ColumnGroup g : _rootColumnGroups) {
			g.write(writer, options);
		}
		writer.endArray();
		
		writer.endObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		internalInitialize();
	}

	protected void internalInitialize() {
		generateMaps();
		
		// Turn the flat list of column groups into a tree
		
		_rootColumnGroups = new LinkedList<ColumnGroup>(columnGroups);
		Collections.sort(_rootColumnGroups, new Comparator<ColumnGroup>() {
			@Override
			public int compare(ColumnGroup o1, ColumnGroup o2) {
				int firstDiff = o1.cellIndices[0] - o2.cellIndices[0];
				return firstDiff != 0 ?
					firstDiff : // whichever group that starts first goes first 
					(o2.cellIndices.length - o1.cellIndices.length); // otherwise, the larger group goes first
			}
		});
		
		for (int i = _rootColumnGroups.size() - 1; i >= 0; i--) {
			ColumnGroup g = _rootColumnGroups.get(i);
			
			for (int j = i + 1; j < _rootColumnGroups.size(); j++) {
				ColumnGroup g2 = _rootColumnGroups.get(j);
				if (g2.parentGroup == null && g.contains(g2)) {
					g2.parentGroup = g;
					g.subgroups.add(g2);
				}
			}
		}
		
		for (int i = _rootColumnGroups.size() - 1; i >= 0; i--) {
			if (_rootColumnGroups.get(i).parentGroup != null) {
				_rootColumnGroups.remove(i);
			}
		}
	}
	
	protected void generateMaps() {
		_nameToColumn = new HashMap<String, Column>();
		_cellIndexToColumn = new HashMap<Integer, Column>();
		
		for (Column column : columns) {
			_nameToColumn.put(column.getHeaderLabel(), column);
			_cellIndexToColumn.put(column.getCellIndex(), column);
		}
	}
}
