package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import com.metaweb.gridlock.expr.HasFields;

public class ReconCandidate implements Serializable, HasFields {
	private static final long serialVersionUID = -8013997214978715606L;
	
	public String 	topicID;
	public String 	topicGUID;
	public String	topicName;
	public String[] typeIDs;
	public double	score;
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("id", topicID);
		o.put("guid", topicGUID);
		o.put("name", topicName);
		o.put("types", typeIDs);
		o.put("score", score);
		
		return o;
	}

	@Override
	public Object getField(String name, Properties bindings) {
		if ("id".equals(name)) {
			return topicName;
		} else if ("guid".equals(name)) {
			return topicGUID;
		} else if ("name".equals(name)) {
			return topicName;
		} else if ("type".equals(name)) {
			return typeIDs;
		} else if ("score".equals(name)) {
			return score;
		}
		return null;
	}
}
