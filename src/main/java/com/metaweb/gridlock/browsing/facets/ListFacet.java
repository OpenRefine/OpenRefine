package com.metaweb.gridlock.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.filters.RowFilter;

public class ListFacet implements Facet {
	final protected List<Object> _choices = new LinkedList<Object>();

	@Override
	public JSONObject getJSON(Properties options) throws JSONException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initializeFromJSON(JSONObject o) throws JSONException {
		JSONArray a = o.getJSONArray("choices");
		int length = a.length();
		
		for (int i = 0; i < length; i++) {
			
		}
	}

	@Override
	public RowFilter getRowFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void computeChoices(FilteredRows filteredRows) {
		// TODO Auto-generated method stub
		
	}

}
