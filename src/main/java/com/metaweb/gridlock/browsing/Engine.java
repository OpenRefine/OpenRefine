package com.metaweb.gridlock.browsing;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.browsing.facets.Facet;
import com.metaweb.gridlock.browsing.facets.ListFacet;
import com.metaweb.gridlock.browsing.filters.RowFilter;
import com.metaweb.gridlock.model.Project;

public class Engine {
	protected Project 		_project;
	protected List<Facet> 	_facets = new LinkedList<Facet>();
	
	public Engine(Project project) {
		_project  = project;
	}
	
	public FilteredRows getAllFilteredRows() {
		return getFilteredRows(null);
	}

	public FilteredRows getFilteredRows(Facet except) {
		ConjunctiveFilteredRows cfr = new ConjunctiveFilteredRows();
		for (Facet facet : _facets) {
			if (facet != except) {
				RowFilter rowFilter = facet.getRowFilter();
				if (rowFilter != null) {
					cfr.add(rowFilter);
				}
			}
		}
		return cfr;
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(_facets.size());
		for (Facet facet : _facets) {
			a.add(facet.getJSON(options));
		}
		o.put("facets", a);
		
		return o;
	}

	public void initializeFromJSON(JSONObject o) throws Exception {
		JSONArray a = o.getJSONArray("facets");
		int length = a.length();
		
		for (int i = 0; i < length; i++) {
			JSONObject fo = a.getJSONObject(i);
			String type = fo.has("type") ? fo.getString("type") : "list";
			
			Facet facet = null;
			if ("list".equals(type)) {
				facet = new ListFacet();
			}
			
			if (facet != null) {
				facet.initializeFromJSON(fo);
				_facets.add(facet);
			}
		}
	}
	
	public void computeFacets() throws JSONException {
		for (Facet facet : _facets) {
			FilteredRows filteredRows = getFilteredRows(facet);
			
			facet.computeChoices(_project, filteredRows);
		}
	}
}
