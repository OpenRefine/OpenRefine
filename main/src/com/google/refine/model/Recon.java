/*

Copyright 2010,2012. Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.expr.HasFields;
import com.google.refine.util.Pool;
 
public class Recon implements HasFields, Jsonizable {
    
    private static final String FREEBASE_SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private static final String FREEBASE_IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/type.object.mid";

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
    public String                service = "unknown";
    public String                identifierSpace = null;
    public String                schemaSpace = null;
    
    public Object[]              features = new Object[Feature_max];
    public List<ReconCandidate>  candidates;
    
    public Judgment              judgment = Judgment.None;
    public String                judgmentAction = "unknown";
    public long                  judgmentHistoryEntry;
    public int                   judgmentBatchSize = 0;
    
    public ReconCandidate        match = null;
    public int                   matchRank = -1;
    
    static public Recon makeFreebaseRecon(long judgmentHistoryEntry) {
        return new Recon(
            judgmentHistoryEntry,
            FREEBASE_IDENTIFIER_SPACE,
            FREEBASE_SCHEMA_SPACE);
    }
    
    public Recon(long judgmentHistoryEntry, String identifierSpace, String schemaSpace) {
        id = System.currentTimeMillis() * 1000000 + Math.round(Math.random() * 1000000);
        this.judgmentHistoryEntry = judgmentHistoryEntry;
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
    }
    
    protected Recon(long id, long judgmentHistoryEntry) {
        this.id = id;
        this.judgmentHistoryEntry = judgmentHistoryEntry;
    }
    
    public Recon dup() {
        Recon r = new Recon(id, judgmentHistoryEntry);
        r.identifierSpace = identifierSpace;
        r.schemaSpace = schemaSpace;
        
        copyTo(r);
        
        return r;
    }
    
    public Recon dup(long judgmentHistoryEntry) {
        Recon r = new Recon(judgmentHistoryEntry, identifierSpace, schemaSpace);
        
        copyTo(r);
        
        return r;
    }
    
    protected void copyTo(Recon r) {
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
    
    @Override
    public Object getField(String name, Properties bindings) {
        if ("id".equals(name)) {
            return id;
        } else if ("best".equals(name)) {
            return candidates != null && candidates.size() > 0 ? candidates.get(0) : null;
        } else if ("candidates".equals(name)) {
            return candidates;
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
        } else if ("service".equals(name)) {
            return service;
        } else if ("identifierSpace".equals(name)) {
            return identifierSpace;
        } else if ("schemaSpace".equals(name)) {
            return schemaSpace;
        }
        return null;
    }
    
    @Override
    public boolean fieldAlsoHasFields(String name) {
        return "match".equals(name) || "best".equals(name);
    }
    
    protected String judgmentToString() {
        return judgmentToString(judgment);
    }
    
    public class Features implements HasFields {
        @Override
        public Object getField(String name, Properties bindings) {
            int index = s_featureMap.containsKey(name) ? s_featureMap.get(name) : -1;
            return (index >= 0 && index < features.length) ? features[index] : null;
        }

        @Override
        public boolean fieldAlsoHasFields(String name) {
            return false;
        }
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        boolean saveMode = "save".equals(options.getProperty("mode"));
        
        writer.object();
        writer.key("id"); writer.value(id);
        if (saveMode) {
            writer.key("judgmentHistoryEntry"); writer.value(judgmentHistoryEntry);
        }
        
        writer.key("service"); writer.value(service);
        writer.key("identifierSpace"); writer.value(identifierSpace);
        writer.key("schemaSpace"); writer.value(schemaSpace);
        
        writer.key("j"); writer.value(judgmentToString());
        if (match != null) {
            writer.key("m");
            match.write(writer, options);
        }
        if (match == null || saveMode) {
            writer.key("c"); writer.array();
            if (candidates != null) {
                for (ReconCandidate c : candidates) {
                    c.write(writer, options);
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
                if (recon != null) {
                    recon.judgmentHistoryEntry = judgmentHistoryEntry;
                }
            } else {
                if (recon == null) {
                    recon = new Recon(id, judgmentHistoryEntry);
                }
                
                if ("j".equals(fieldName)) {
                    recon.judgment = stringToJudgment(jp.getText());
                } else if ("m".equals(fieldName)) {
                    if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
                        // legacy case
                        String candidateID = jp.getText();
                        recon.match = pool.getReconCandidate(candidateID);
                    } else {
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
                            // legacy case
                            String candidateID = jp.getText();
                            recon.addCandidate(pool.getReconCandidate(candidateID));
                        } else {
                            recon.addCandidate(ReconCandidate.loadStreaming(jp));
                        }
                    }
                } else if ("service".equals(fieldName)) {
                    recon.service = jp.getText();
                } else if ("identifierSpace".equals(fieldName)) {
                    recon.identifierSpace = jp.getText();
                    if ("null".equals(recon.identifierSpace)) {
                        recon.identifierSpace = FREEBASE_IDENTIFIER_SPACE;
                    }
                } else if ("schemaSpace".equals(fieldName)) {
                    recon.schemaSpace = jp.getText();
                    if ("null".equals(recon.schemaSpace)) {
                        recon.schemaSpace = FREEBASE_SCHEMA_SPACE;
                    }
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
