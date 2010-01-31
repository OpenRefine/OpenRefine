package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.expr.HasFields;

public class Row implements Serializable, HasFields {
	private static final long serialVersionUID = -689264211730915507L;
	
	public boolean		flagged;
	public boolean		starred;
	public List<Cell> 	cells;
	
	public Row(int cellCount) {
		cells = new ArrayList<Cell>(cellCount);
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(cells.size());
		for (Cell cell : cells) {
			a.add(cell.getJSON(options));
		}
		o.put("cells", a);
		o.put("flagged", flagged);
		o.put("starred", starred);
		
		return o;
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
				return cells.get(column.cellIndex);
			}
			return null;
		}
		
	}
}
