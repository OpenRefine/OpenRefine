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

public class Engine {
	protected List<Facet> facets = new LinkedList<Facet>();
	
	public FilteredRows getAllFilteredRows() {
		return getFilteredRows(null);
	}

	public FilteredRows getFilteredRows(Facet except) {
		ConjunctiveFilteredRows cfr = new ConjunctiveFilteredRows();
		for (Facet facet : facets) {
			if (facet != except) {
				cfr.add(facet.getRowFilter());
			}
		}
		return cfr;
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		List<JSONObject> a = new ArrayList<JSONObject>(facets.size());
		for (Facet facet : facets) {
			a.add(facet.getJSON(options));
		}
		o.put("facets", a);
		
		return o;
	}

	public void initializeFromJSON(JSONObject o) throws JSONException {
		JSONArray a = o.getJSONArray("facets");
		int length = a.length();
		
		for (int i = 0; i < length; i++) {
			JSONObject fo = a.getJSONObject(i);
			String type = fo.getString("type");
			
			Facet facet = null;
			if ("list".equals(type)) {
				facet = new ListFacet();
			}
			
			if (facet != null) {
				facet.initializeFromJSON(fo);
				facets.add(facet);
			}
		}
	}
	
	public void computeFacets() throws JSONException {
		for (Facet facet : facets) {
			FilteredRows filteredRows = getFilteredRows(facet);
			
			facet.computeChoices(filteredRows);
		}
	}
}
