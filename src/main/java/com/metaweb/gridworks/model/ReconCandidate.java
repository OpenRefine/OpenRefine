package com.metaweb.gridworks.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.JSONUtilities;

public class ReconCandidate implements HasFields, Jsonizable {
    final public String     topicID;
    final public String     topicGUID;
    final public String     topicName;
    final public String[]   typeIDs;
    final public double     score;
    
    public ReconCandidate(String topicID, String topicGUID, String topicName, String[] typeIDs, double score) {
        this.topicID = topicID;
        this.topicGUID = topicGUID;
        this.topicName = topicName;
        this.typeIDs = typeIDs;
        this.score = score;
    }
    
    public Object getField(String name, Properties bindings) {
        if ("id".equals(name)) {
            return topicID;
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
    
    public boolean fieldAlsoHasFields(String name) {
        return false;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("id"); writer.value(topicID);
        writer.key("guid"); writer.value(topicGUID);
        writer.key("name"); writer.value(topicName);
        writer.key("score"); writer.value(score);
        
        /* if (!options.containsKey("reconCandidateOmitTypes")) */ {
	        writer.key("types"); writer.array();
	        for (String typeID : typeIDs) {
	            writer.value(typeID);
	        }
	        writer.endArray();
        }
        
        writer.endObject();
    }
    
    static public ReconCandidate load(JSONObject obj) throws Exception {
        if (obj == null) {
            return null;
        }
        
        ReconCandidate candidate = new ReconCandidate(
            obj.getString("id"),
            obj.getString("guid"),
            obj.getString("name"),
            JSONUtilities.getStringArray(obj, "types"),
            obj.getDouble("score")
        );
        return candidate;
    }
    
    static public ReconCandidate loadStreaming(JsonParser jp, Map<Long, Recon> reconCache) throws Exception {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL || t != JsonToken.START_OBJECT) {
            return null;
        }
        
        String id = null;
        String guid = null;
        String name = null;
        List<String> types = null;
        double score = 0;
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            
            if ("id".equals(fieldName)) {
                id = jp.getText();
            } else if ("guid".equals(fieldName)) {
                guid = jp.getText();
            } else if ("name".equals(fieldName)) {
                name = jp.getText();
            } else if ("score".equals(fieldName)) {
                score = jp.getDoubleValue();
            } else if ("types".equals(fieldName)) {
                if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                    return null;
                }
                
                types = new ArrayList<String>();
                
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    types.add(jp.getText());
                }
            }
        }
        
        String[] typesA;
        if (types != null) {
            typesA = new String[types.size()];
            types.toArray(typesA);
        } else {
            typesA = new String[0];
        }
        
        return new ReconCandidate(
            id,
            guid,
            name,
            typesA, 
            score
        );
    }
}