package com.metaweb.gridworks.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class ColumnGroup implements Serializable, Jsonizable {
	private static final long serialVersionUID = 2161780779920066118L;

	final public int[] 	cellIndices;  // must be in order from smallest to largest
	final public int	keyCellIndex; // could be -1 if there is no key cell 
	
	transient public ColumnGroup		parentGroup;
	transient public List<ColumnGroup> 	subgroups;
	
	public ColumnGroup(int[] cellIndices, int keyCellIndex) {
		this.cellIndices = cellIndices;
		this.keyCellIndex = keyCellIndex;
		internalInitialize();
	}
	
	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		
		writer.key("cellIndices"); writer.array();
		for (int i : cellIndices) {
			writer.value(i);
		}
		writer.endArray();
		
		if (subgroups != null && subgroups.size() > 0) {
			writer.key("subgroups"); writer.array();
			for (ColumnGroup g : subgroups) {
				g.write(writer, options);
			}
			writer.endArray();
		}
		
		writer.endObject();
	}
	
	public boolean contains(ColumnGroup g) {
		for (int c : g.cellIndices) {
			boolean has = false;
			for (int d : cellIndices) {
				if (c == d) {
					has = true;
					break;
				}
			}
			if (!has) {
				return false;
			}
		}
		return true;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		internalInitialize();
	}
	
	protected void internalInitialize() {
		subgroups = new LinkedList<ColumnGroup>();
	}
}
