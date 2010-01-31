package com.metaweb.gridlock.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.browsing.DecoratedValue;

public class NominalFacetChoice {
	final public DecoratedValue	decoratedValue;
	public int					count;
	public boolean				selected;
	
	public NominalFacetChoice(DecoratedValue decoratedValue) {
		this.decoratedValue = decoratedValue;
	}
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("v", decoratedValue.getJSON(options));
		o.put("c", count);
		o.put("s", selected);
		
		return o;
	}
}
