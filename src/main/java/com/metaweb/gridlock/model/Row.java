package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Row implements Serializable {
	private static final long serialVersionUID = -689264211730915507L;
	
	public boolean		flagged;
	public boolean		starred;
	public List<Cell> 	cells;
	
	public Row(int cellCount) {
		cells = new ArrayList<Cell>(cellCount);
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(cells.size());
		for (Cell cell : cells) {
			a.add(cell.getJSON());
		}
		o.put("cells", a);
		o.put("flagged", flagged);
		o.put("starred", starred);
		
		return o;
	}
}
