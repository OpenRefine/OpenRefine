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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.refine.expr.HasFields;
import com.google.refine.util.JsonViews;
import com.google.refine.util.ParsingUtilities;

@JsonFilter("reconCandidateFilter")
public class Recon implements HasFields {

    /**
     * Freebase schema URLs kept for compatibility with legacy reconciliation results
     */
    private static final String FREEBASE_SCHEMA_SPACE = "http://rdf.freebase.com/ns/type.object.id";
    private static final String FREEBASE_IDENTIFIER_SPACE = "http://rdf.freebase.com/ns/type.object.mid";

    private static final String WIKIDATA_SCHEMA_SPACE = "http://www.wikidata.org/prop/direct/";
    private static final String WIKIDATA_IDENTIFIER_SPACE = "http://www.wikidata.org/entity/";
    private static final Random idGenerator = new Random();

    static public enum Judgment {
        @JsonProperty("none")
        None, @JsonProperty("matched")
        Matched, @JsonProperty("new")
        New
    }

    @Deprecated
    static public String judgmentToString(Judgment judgment) {
        if (judgment == Judgment.Matched) {
            return "matched";
        } else if (judgment == Judgment.New) {
            return "new";
        } else {
            return "none";
        }
    }

    @Deprecated
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

    @JsonIgnore
    final public long id;
    @JsonIgnore
    public String service = "unknown";
    @JsonIgnore
    public String identifierSpace = null;
    @JsonIgnore
    public String schemaSpace = null;

    @JsonIgnore
    public Object[] features = new Object[Feature_max];
    @JsonIgnore
    public List<ReconCandidate> candidates;

    @JsonIgnore
    public Judgment judgment = Judgment.None;
    @JsonIgnore
    public String judgmentAction = "unknown";
    @JsonIgnore
    public long judgmentHistoryEntry;
    @JsonIgnore
    public int judgmentBatchSize = 0;

    @JsonIgnore
    public ReconCandidate match = null;
    @JsonIgnore
    public int matchRank = -1;

    @Deprecated
    static public Recon makeFreebaseRecon(long judgmentHistoryEntry) {
        return new Recon(
                judgmentHistoryEntry,
                FREEBASE_IDENTIFIER_SPACE,
                FREEBASE_SCHEMA_SPACE);
    }

    static public Recon makeWikidataRecon(long judgmentHistoryEntry) {
        return new Recon(
                judgmentHistoryEntry,
                WIKIDATA_IDENTIFIER_SPACE,
                WIKIDATA_SCHEMA_SPACE);
    }

    public Recon(long judgmentHistoryEntry, String identifierSpace, String schemaSpace) {
        id = idGenerator.nextLong();
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

    @JsonIgnore
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

    @Deprecated
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

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("judgmentHistoryEntry")
    @JsonView(JsonViews.SaveMode.class)
    public long getJudgmentHistoryEntry() {
        return judgmentHistoryEntry;
    }

    @JsonProperty("service")
    public String getServiceURI() {
        return service;
    }

    @JsonProperty("identifierSpace")
    public String getIdentifierSpace() {
        return identifierSpace;
    }

    @JsonProperty("schemaSpace")
    public String getSchemaSpace() {
        return schemaSpace;
    }

    @JsonProperty("j")
    public Judgment getJudgment() {
        return judgment;
    }

    @JsonProperty("m")
    @JsonInclude(Include.NON_NULL)
    public ReconCandidate getMatch() {
        return match;
    }

    @JsonProperty("c")
    // @JsonView(JsonViews.SaveMode.class)
    public List<ReconCandidate> getCandidates() {
        if (candidates != null) {
            return candidates;
        }
        return Collections.emptyList();
    }

    @JsonProperty("f")
    @JsonView(JsonViews.SaveMode.class)
    public Object[] getfeatures() {
        return features;
    }

    @JsonProperty("judgmentAction")
    @JsonView(JsonViews.SaveMode.class)
    public String getJudgmentAction() {
        return judgmentAction;
    }

    @JsonProperty("judgmentBatchSize")
    @JsonView(JsonViews.SaveMode.class)
    public int getJudgmentBatchSize() {
        return judgmentBatchSize;
    }

    @JsonProperty("matchRank")
    @JsonView(JsonViews.SaveMode.class)
    @JsonInclude(Include.NON_NULL)
    public Integer getMatchRank() {
        if (match != null) {
            return matchRank;
        }
        return null;
    }

    static public Recon loadStreaming(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, Recon.class);
    }

    public Recon(
            @JsonProperty("id") long id,
            @JsonProperty("judgmentHistoryEntry") long judgmentHistoryEntry,
            @JsonProperty("j") Judgment judgment,
            @JsonProperty("m") ReconCandidate match,
            @JsonProperty("f") Object[] features,
            @JsonProperty("c") List<ReconCandidate> candidates,
            @JsonProperty("service") String service,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace,
            @JsonProperty("judgmentAction") String judgmentAction,
            @JsonProperty("judgmentBatchSize") Integer judgmentBatchSize,
            @JsonProperty("matchRank") Integer matchRank) {
        this.id = id;
        this.judgmentHistoryEntry = judgmentHistoryEntry;
        this.judgment = judgment != null ? judgment : Judgment.None;
        this.match = match;
        this.features = features != null ? features : new Object[Feature_max];
        this.candidates = candidates != null ? candidates : new ArrayList<>();
        this.service = service != null ? service : "unknown";
        this.identifierSpace = identifierSpace;
        this.schemaSpace = schemaSpace;
        this.judgmentAction = judgmentAction != null ? judgmentAction : "unknown";
        this.judgmentBatchSize = judgmentBatchSize != null ? judgmentBatchSize : 0;
        this.matchRank = matchRank != null ? matchRank : -1;
    }
}
