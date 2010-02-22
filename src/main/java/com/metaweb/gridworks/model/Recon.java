package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;

public class Recon implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = 8906257833709315762L;
	
	static public enum Judgment {
		None,
		Matched,
		New
	}
	
	static public String judgmentToString(Judgment judgment) {
		if (judgment == Judgment.Matched) {
			return "matched";
		} else if (judgment == Judgment.New) {
			return "new";
		} else {
			return "none";
		}
	}
	
	static public Judgment stringToJudgment(String s) {
		if ("matched".equals(s)) {
			return Judgment.Matched;
		} else if ("new".equals(s)) {
			return Judgment.New;
		} else {
			return Judgment.None;
		}
	}
	
	
	static public int Feature_typeMatch = 0;
	static public int Feature_nameMatch = 1;
	static public int Feature_nameLevenshtein = 2;
	static public int Feature_nameWordDistance = 3;
	static public int Feature_max = 4;

	static protected Map<String, Integer> s_featureMap;
	static {
		s_featureMap = new HashMap<String, Integer>();
		s_featureMap.put("typeMatch", Feature_typeMatch);
		s_featureMap.put("nameMatch", Feature_nameMatch);
		s_featureMap.put("nameLevenshtein", Feature_nameLevenshtein);
		s_featureMap.put("nameWordDistance", Feature_nameWordDistance);
	}
	
	public Object[] 			features = new Object[Feature_max];
	public List<ReconCandidate> candidates = new LinkedList<ReconCandidate>();
	public Judgment				judgment = Judgment.None;
	public ReconCandidate		match = null;
	
	public Recon dup() {
		Recon r = new Recon();
		
		System.arraycopy(features, 0, r.features, 0, features.length);
		
		r.candidates.addAll(candidates);
		r.judgment = judgment;
		r.match = match;
		return r;
	}
	
	public Object getFeature(int feature) {
		return feature < features.length ? features[feature] : null;
	}
	
	public void setFeature(int feature, Object v) {
		if (feature >= features.length) {
			if (feature >= Feature_max) {
				return;
			}
			
			// We deserialized this object from an older version of the class
			// that had fewer features, so we can just try to extend it
			
			Object[] newFeatures = new Object[Feature_max];
			
			System.arraycopy(features, 0, newFeatures, 0, features.length);
			
			features = newFeatures;
		}
		
		features[feature] = v;
	}
	
	public Object getField(String name, Properties bindings) {
		if ("best".equals(name)) {
			return candidates.size() > 0 ? candidates.get(0) : null;
		} else if ("judgment".equals(name) || "judgement".equals(name)) {
			return judgmentToString();
		} else if ("matched".equals(name)) {
			return judgment == Judgment.Matched;
		} else if ("new".equals(name)) {
			return judgment == Judgment.New;
		} else if ("match".equals(name)) {
			return match;
		} else if ("features".equals(name)) {
			return new Features();
		}
		return null;
	}
	
	protected String judgmentToString() {
		return judgmentToString(judgment);
	}
	
	public class Features implements HasFields {
		public Object getField(String name, Properties bindings) {
			int index = s_featureMap.get(name);
			return index < features.length ? features[index] : null;
		}
	}

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("j");
		writer.value(judgmentToString());
		
		if (match != null) {
			writer.key("m");
			match.write(writer, options);
		} else {
			writer.key("c"); writer.array();
			for (ReconCandidate c : candidates) {
				c.write(writer, options);
			}
			writer.endArray();
		}
		
		writer.endObject();
	}
}
