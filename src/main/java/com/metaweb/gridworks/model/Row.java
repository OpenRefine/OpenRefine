package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;

public class Row implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = -689264211730915507L;
	
	public boolean		flagged;
	public boolean		starred;
	final public List<Cell> 	cells;
	
	transient public	List<Integer> contextRows;
	transient public	List<Integer> contextCells;
	
	public Row(int cellCount) {
		cells = new ArrayList<Cell>(cellCount);
	}
	
	@Override
	public Object getField(String name, Properties bindings) {
		if ("flagged".equals(name)) {
			return flagged;
		} else if ("starred".equals(name)) {
			return starred;
		} else if ("cells".equals(name)) {
			return new Cells();
		}
		return null;
	}
	
	public class Cells implements HasFields {
		private Cells() {};

		@Override
		public Object getField(String name, Properties bindings) {
			Project project = (Project) bindings.get("project");
			Column column = project.columnModel.getColumnByName(name);
			if (column != null) {
				return cells.get(column.getCellIndex());
			}
			return null;
		}
		
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("flagged"); writer.value(flagged);
		writer.key("starred"); writer.value(starred);
		
		writer.key("cells"); writer.array();
		for (Cell cell : cells) {
			cell.write(writer, options);
		}
		writer.endArray();
		
		if (options.containsKey("rowIndex")) {
			writer.key("i"); writer.value(options.get("rowIndex"));
		}
		
		writer.endObject();
	}
}
