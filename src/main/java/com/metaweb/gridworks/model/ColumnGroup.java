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

	final public int 	startColumnIndex;
	final public int	columnSpan;
	final public int	keyColumnIndex; // could be -1 if there is no key cell 
	
	transient public ColumnGroup		parentGroup;
	transient public List<ColumnGroup> 	subgroups;
	
	public ColumnGroup(int startColumnIndex, int columnSpan, int keyColumnIndex) {
		this.startColumnIndex = startColumnIndex;
		this.columnSpan = columnSpan;
		this.keyColumnIndex = keyColumnIndex;
		internalInitialize();
	}
	
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		
		writer.key("startColumnIndex"); writer.value(startColumnIndex);
		writer.key("columnSpan"); writer.value(columnSpan);
		writer.key("keyColumnIndex"); writer.value(keyColumnIndex);
		
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
		return (g.startColumnIndex >= startColumnIndex &&
			g.startColumnIndex < startColumnIndex + columnSpan);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		internalInitialize();
	}
	
	protected void internalInitialize() {
		subgroups = new LinkedList<ColumnGroup>();
	}
}
