package com.metaweb.gridlock.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridlock.Jsonizable;
import com.metaweb.gridlock.expr.HasFields;

public class Recon implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = 8906257833709315762L;
	
	static public enum Judgment {
		None,
		Approve,
		New
	}
	
	public Map<String, Object> 	features = new HashMap<String, Object>();
	public List<ReconCandidate> candidates = new LinkedList<ReconCandidate>();
	public Judgment				judgment = Judgment.None;
	public ReconCandidate		match = null;
	
	@Override
	public Object getField(String name, Properties bindings) {
		if ("best".equals(name)) {
			return candidates.size() > 0 ? candidates.get(0) : null;
		} else if ("judgment".equals(name) || "judgement".equals(name)) {
			return judgmentToString();
		} else if ("approved".equals(name)) {
			return judgment == Judgment.Approve;
		} else if ("new".equals(name)) {
			return judgment == Judgment.New;
		} else if ("match".equals(name)) {
			return match;
		}
		return null;
	}
	
	protected String judgmentToString() {
		if (judgment == Judgment.Approve) {
			return "approve";
		} else if (judgment == Judgment.New) {
			return "new";
		} else {
			return "none";
		}
	}
	
	public class Features implements HasFields {
		@Override
		public Object getField(String name, Properties bindings) {
			return features.get(name);
		}
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("j");
		writer.value(judgmentToString());
		writer.endObject();
	}
}
