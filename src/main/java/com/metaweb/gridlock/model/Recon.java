package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

public class Recon implements Serializable {
	private static final long serialVersionUID = 8906257833709315762L;
	
	static public enum Judgment {
		None,
		Approve,
		New
	}
	
	public Map<String, Object> 	features = new HashMap<String, Object>();
	public List<ReconCandidate> candidates = new LinkedList<ReconCandidate>();
	public Judgment				judgment = Judgment.None;
	
	public JSONObject getJSON(Properties options) throws JSONException {
		JSONObject o = new JSONObject();
		
		if (judgment == Judgment.None) {
			o.put("j", "none");
		} else if (judgment == Judgment.Approve) {
			o.put("j", "approve");
		} else if (judgment == Judgment.New) {
			o.put("j", "new");
		}
		
		return o;
	}
}
