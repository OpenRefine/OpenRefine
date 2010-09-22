package com.google.refine.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.expr.HasFields;

public class ReconCandidate implements HasFields, Jsonizable {
    final public String     id;
    final public String     name;
    final public String[]   types;
    final public double     score;
    
    public ReconCandidate(String topicID, String topicName, String[] typeIDs, double score) {
        this.id = topicID;
        this.name = topicName;
        this.types = typeIDs;
        this.score = score;
    }
    
    public Object getField(String name, Properties bindings) {
        if ("id".equals(name)) {
            return id;
        } else if ("name".equals(name)) {
            return this.name;
        } else if ("type".equals(name)) {
            return types;
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
        writer.key("id"); writer.value(id);
        writer.key("name"); writer.value(name);
        writer.key("score"); writer.value(score);
        
        /* if (!options.containsKey("reconCandidateOmitTypes")) */ {
            writer.key("types"); writer.array();
            for (String typeID : types) {
                writer.value(typeID);
            }
            writer.endArray();
        }
        
        writer.endObject();
    }
    
    static public ReconCandidate loadStreaming(String s) throws Exception {
        JsonFactory jsonFactory = new JsonFactory(); 
        JsonParser jp = jsonFactory.createJsonParser(s);
        
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            return null;
        }
        return loadStreaming(jp);
    }
    
    static public ReconCandidate loadStreaming(JsonParser jp) throws Exception {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL || t != JsonToken.START_OBJECT) {
            return null;
        }
        
        String id = null;
        String name = null;
        List<String> types = null;
        double score = 0;
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            
            if ("id".equals(fieldName)) {
                id = jp.getText();
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
            name,
            typesA, 
            score
        );
    }
}