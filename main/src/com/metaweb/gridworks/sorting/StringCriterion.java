package com.metaweb.gridworks.sorting;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Project;

public class StringCriterion extends Criterion {
	public boolean caseSensitive;
	
	@Override
	public void initializeFromJSON(Project project, JSONObject obj) throws JSONException {
		super.initializeFromJSON(project, obj);
		
		if (obj.has("caseSensitive") && !obj.isNull("caseSensitive")) {
			caseSensitive = obj.getBoolean("caseSensitive");
		}
	}
	
	@Override
	public KeyMaker createKeyMaker() {
		return new KeyMaker() {
			@Override
			protected Object makeKey(Object value) {
				return (ExpressionUtils.isNonBlankData(value) && !(value instanceof String)) ?
						value.toString() : value;
			}
			
			@Override
			public int compareKeys(Object key1, Object key2) {
				if (StringCriterion.this.caseSensitive) {
					return ((String) key1).compareTo((String) key2);
				} else {
					return ((String) key1).compareToIgnoreCase((String) key2);
				}
			}
		};
	}
}
