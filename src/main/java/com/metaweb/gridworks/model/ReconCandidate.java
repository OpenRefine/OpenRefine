package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;

public class ReconCandidate implements Serializable, HasFields, Jsonizable {
	private static final long serialVersionUID = -8013997214978715606L;
	
	final public String 	topicID;
	final public String 	topicGUID;
	final public String		topicName;
	final public String[] 	typeIDs;
	final public double		score;
	
	public ReconCandidate(String topicID, String topicGUID, String topicName, String[] typeIDs, double score) {
		this.topicID = topicID;
		this.topicGUID = topicGUID;
		this.topicName = topicName;
		this.typeIDs = typeIDs;
		this.score = score;
	}
	
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

	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("id"); writer.value(topicID);
		writer.key("guid"); writer.value(topicGUID);
		writer.key("name"); writer.value(topicName);
		writer.key("score"); writer.value(score);
		writer.key("types"); writer.array();
		for (String typeID : typeIDs) {
			writer.value(typeID);
		}
		writer.endArray();
		
		writer.endObject();
	}
}
