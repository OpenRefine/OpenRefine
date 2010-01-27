package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class ReconCandidate implements Serializable {
	private static final long serialVersionUID = -8013997214978715606L;
	
	public String 	topicID;
	public String 	topicGUID;
	public String	topicName;
	public String[] typeIDs;
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		o.put("id", topicID);
		o.put("guid", topicGUID);
		o.put("name", topicName);
		o.put("types", typeIDs);
		
		return o;
	}
}
