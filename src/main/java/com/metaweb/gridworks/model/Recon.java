package com.metaweb.gridworks.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
    
    final public long            id;
    public Object[]              features = new Object[Feature_max];
    public List<ReconCandidate>  candidates;
    public Judgment              judgment = Judgment.None;
    public ReconCandidate        match = null;
    
    public Recon() {
        id = System.currentTimeMillis() * 1000000 + Math.round(Math.random() * 1000000);
    }
    
    protected Recon(long id) {
        this.id = id;
    }
    
    public Recon dup() {
        Recon r = new Recon();
        
        System.arraycopy(features, 0, r.features, 0, features.length);
        
        if (candidates != null) {
            r.candidates = new ArrayList<ReconCandidate>(candidates);
        }
        
        r.judgment = judgment;
        r.match = match;
        
        return r;
    }
    
    public void addCandidate(ReconCandidate candidate) {
        if (candidates == null) {
            candidates = new ArrayList<ReconCandidate>(3);
        }
        candidates.add(candidate);
    }
    
    public ReconCandidate getBestCandidate() {
        if (candidates != null && candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
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
            return candidates != null && candidates.size() > 0 ? candidates.get(0) : null;
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
        writer.key("id"); writer.value(id);
        writer.key("j"); writer.value(judgmentToString());
        
        if (match != null) {
            writer.key("m");
            match.write(writer, options);
        } else {
            writer.key("c"); writer.array();
            if (candidates != null) {
                for (ReconCandidate c : candidates) {
                    c.write(writer, options);
                }
            }
            writer.endArray();
        }
        
        if ("save".equals(options.getProperty("mode"))) {
            writer.key("f");
                writer.array();
                for (Object o : features) {
                    writer.value(o);
                }
                writer.endArray();
        }
        
        writer.endObject();
    }
    
    static public Recon load(JSONObject obj) throws Exception {
        if (obj == null) {
            return null;
        }
        
        Recon recon = new Recon(obj.getLong("id"));
        
        if (obj.has("j")) {
            recon.judgment = stringToJudgment(obj.getString("j"));
        }
        if (obj.has("m")) {
            recon.match = ReconCandidate.load(obj.getJSONObject("m"));
        }
        if (obj.has("c")) {
            JSONArray a = obj.getJSONArray("c");
            int count = a.length();
            
            for (int i = 0; i < count; i++) {
                recon.addCandidate(ReconCandidate.load(a.getJSONObject(i)));
            }
        }
        if (obj.has("f")) {
            JSONArray a = obj.getJSONArray("f");
            int count = a.length();
            
            for (int i = 0; i < count && i < Feature_max; i++) {
                if (!a.isNull(i)) {
                    recon.features[i] = a.get(i);
                }
            }
        }
        
        return recon;
    }

}
