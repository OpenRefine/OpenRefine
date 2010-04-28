package com.metaweb.gridworks.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.Pool;
 
public class Recon implements HasFields, Jsonizable {
    
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
    
    static final public int Feature_typeMatch = 0;
    static final public int Feature_nameMatch = 1;
    static final public int Feature_nameLevenshtein = 2;
    static final public int Feature_nameWordDistance = 3;
    static final public int Feature_max = 4;

    static final protected Map<String, Integer> s_featureMap = new HashMap<String, Integer>();
    static {
        s_featureMap.put("typeMatch", Feature_typeMatch);
        s_featureMap.put("nameMatch", Feature_nameMatch);
        s_featureMap.put("nameLevenshtein", Feature_nameLevenshtein);
        s_featureMap.put("nameWordDistance", Feature_nameWordDistance);
    }
    
    final public long            id;
    public Object[]              features = new Object[Feature_max];
    public String				 service = "unknown";
    public List<ReconCandidate>  candidates;
    
    public Judgment              judgment = Judgment.None;
    public String				 judgmentAction = "unknown";
    public long			         judgmentHistoryEntry;
    public int					 judgmentBatchSize = 0;
    
    public ReconCandidate        match = null;
    public int					 matchRank = -1;
    
    public Recon(long judgmentHistoryEntry) {
        id = System.currentTimeMillis() * 1000000 + Math.round(Math.random() * 1000000);
        this.judgmentHistoryEntry = judgmentHistoryEntry;
    }
    
    protected Recon(long id, long judgmentHistoryEntry) {
        this.id = id;
        this.judgmentHistoryEntry = judgmentHistoryEntry;
    }
    
    public Recon dup(long judgmentHistoryEntry) {
        Recon r = new Recon(judgmentHistoryEntry);
        
        System.arraycopy(features, 0, r.features, 0, features.length);
        
        if (candidates != null) {
            r.candidates = new ArrayList<ReconCandidate>(candidates);
        }
        
        r.service = service;
        
        r.judgment = judgment;
        
        r.judgmentAction = judgmentAction;
        r.judgmentBatchSize = judgmentBatchSize;
        
        r.match = match;
        r.matchRank = matchRank;
        
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
        } else if ("judgmentAction".equals(name) || "judgementAction".equals(name)) {
            return judgmentAction;
        } else if ("judgmentHistoryEntry".equals(name) || "judgementHistoryEntry".equals(name)) {
            return judgmentHistoryEntry;
        } else if ("judgmentBatchSize".equals(name) || "judgementBatchSize".equals(name)) {
            return judgmentBatchSize;
        } else if ("matched".equals(name)) {
            return judgment == Judgment.Matched;
        } else if ("new".equals(name)) {
            return judgment == Judgment.New;
        } else if ("match".equals(name)) {
            return match;
        } else if ("matchRank".equals(name)) {
            return matchRank;
        } else if ("features".equals(name)) {
            return new Features();
        }
        return null;
    }
    
    public boolean fieldAlsoHasFields(String name) {
        return "match".equals(name) || "best".equals(name);
    }
    
    protected String judgmentToString() {
        return judgmentToString(judgment);
    }
    
    public class Features implements HasFields {
        public Object getField(String name, Properties bindings) {
            int index = s_featureMap.get(name);
            return index < features.length ? features[index] : null;
        }

        public boolean fieldAlsoHasFields(String name) {
            return false;
        }
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
    	boolean saveMode = "save".equals(options.getProperty("mode"));
    	
        writer.object();
        writer.key("id"); writer.value(id);
        if (saveMode) {
            writer.key("judgmentHistoryEntry"); writer.value(judgmentHistoryEntry);
        }
        
        writer.key("j"); writer.value(judgmentToString());
        if (match != null) {
            writer.key("m");
            writer.value(match.topicID);
        }
        if (match == null || saveMode) {
            writer.key("c"); writer.array();
            if (candidates != null) {
                for (ReconCandidate c : candidates) {
                    writer.value(c.topicID);
                }
            }
            writer.endArray();
        }
        
        if (saveMode) {
            writer.key("f");
                writer.array();
                for (Object o : features) {
                    writer.value(o);
                }
                writer.endArray();
                
            writer.key("service"); writer.value(service);
            writer.key("judgmentAction"); writer.value(judgmentAction);
            writer.key("judgmentBatchSize"); writer.value(judgmentBatchSize);
            
            if (match != null) {
            	writer.key("matchRank"); writer.value(matchRank);
            }
        }
        
        writer.endObject();
    }
    
    static public Recon loadStreaming(String s, Pool pool) throws Exception {
        JsonFactory jsonFactory = new JsonFactory(); 
        JsonParser jp = jsonFactory.createJsonParser(s);
        
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            return null;
        }
        return loadStreaming(jp, pool);
    }
    
    static public Recon loadStreaming(JsonParser jp, Pool pool) throws Exception {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL || t != JsonToken.START_OBJECT) {
            return null;
        }
        
        Recon recon = null;
        long id = -1;
        long judgmentHistoryEntry = -1;
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            
            if ("id".equals(fieldName)) {
                id = jp.getLongValue();
            } else if ("judgmentHistoryEntry".equals(fieldName)) {
                judgmentHistoryEntry = jp.getLongValue();
            } else {
                if (recon == null) {
                    recon = new Recon(id, judgmentHistoryEntry);
                }
                
                if ("j".equals(fieldName)) {
                    recon.judgment = stringToJudgment(jp.getText());
                } else if ("m".equals(fieldName)) {
                    if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                        String candidateID = jp.getText();
                        
                        recon.match = pool.getReconCandidate(candidateID);
                    } else {
                        // legacy
                        recon.match = ReconCandidate.loadStreaming(jp);
                    }
                } else if ("f".equals(fieldName)) {
                    if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                        return null;
                    }
                    
                    int feature = 0;
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        if (feature < recon.features.length) {
                            JsonToken token = jp.getCurrentToken();
                            if (token == JsonToken.VALUE_STRING) {
                                recon.features[feature++] = jp.getText();
                            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                                recon.features[feature++] = jp.getLongValue();
                            } else if (token == JsonToken.VALUE_NUMBER_FLOAT) {
                                recon.features[feature++] = jp.getDoubleValue();
                            } else if (token == JsonToken.VALUE_FALSE) {
                                recon.features[feature++] = false;
                            } else if (token == JsonToken.VALUE_TRUE) {
                                recon.features[feature++] = true;
                            }
                        }
                    }
                } else if ("c".equals(fieldName)) {
                    if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                        return null;
                    }
                    
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                            String candidateID = jp.getText();
                        
                            recon.addCandidate(pool.getReconCandidate(candidateID));
                        } else {
                            // legacy
                            recon.addCandidate(ReconCandidate.loadStreaming(jp));
                        }
                    }
                } else if ("service".equals(fieldName)) {
                	recon.service = jp.getText();
                } else if ("judgmentAction".equals(fieldName)) {
                	recon.judgmentAction = jp.getText();
                } else if ("judgmentBatchSize".equals(fieldName)) {
                	recon.judgmentBatchSize = jp.getIntValue();
                } else if ("matchRank".equals(fieldName)) {
                	recon.matchRank = jp.getIntValue();
                }
            }
        }
        
        return recon;
    }
}
