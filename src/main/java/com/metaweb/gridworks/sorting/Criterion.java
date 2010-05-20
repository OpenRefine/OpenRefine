package com.metaweb.gridworks.sorting;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.model.Row;

abstract public class Criterion {
	public String columnName;
	protected int cellIndex;
	
	// These take on positive and negative values to indicate where blanks and errors
	// go relative to non-blank values. They are also relative to each another.
	// Blanks and errors are not affected by the reverse flag.
	public int blankPosition = 1;
	public int errorPosition = 2;
	
	public boolean reverse;
	
	public void initializeFromJSON(Project project, JSONObject obj) throws JSONException {
		if (obj.has("column") && !obj.isNull("column")) {
			columnName = obj.getString("column");
			
			Column column = project.columnModel.getColumnByName(columnName);
			cellIndex = column != null ? column.getCellIndex() : -1;
		}
		
		if (obj.has("blankPosition") && !obj.isNull("blankPosition")) {
			blankPosition = obj.getInt("blankPosition");
		}
		if (obj.has("errorPosition") && !obj.isNull("errorPosition")) {
			blankPosition = obj.getInt("errorPosition");
		}
		
		if (obj.has("reverse") && !obj.isNull("reverse")) {
			reverse = obj.getBoolean("reverse");
		}
	}
	
	abstract public class KeyMaker {
		public Object makeKey(Project project, Record record) {
			Object error = null;
			Object finalKey = null;
			
			for (int r = record.fromRowIndex; r < record.toRowIndex; r++) {
				Object key = makeKey(project, project.rows.get(r), r);
				if (ExpressionUtils.isError(key)) {
					error = key;
				} else if (ExpressionUtils.isNonBlankData(key)) {
					if (finalKey == null) {
						finalKey = key;
					} else {
						int c = compareKeys(finalKey, key);
						if (reverse) {
							if (c < 0) { // key > finalKey
								finalKey = key;
							}
						} else {
							if (c > 0) { // key < finalKey
								finalKey = key;
							}
						}
					}
				}
			}
			
			if (finalKey != null) {
				return finalKey;
			} else if (error != null) {
				return error;
			} else {
				return null;
			}
		}
		
		public Object makeKey(Project project, Row row, int rowIndex) {
			if (cellIndex < 0) {
				return null;
			} else {
				Object value = row.getCellValue(cellIndex);
				return makeKey(value);
			}
		}
		
		abstract public int compareKeys(Object key1, Object key2);
		
		abstract protected Object makeKey(Object value);
	}
	abstract public KeyMaker createKeyMaker();
}
