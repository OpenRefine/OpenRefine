package com.google.refine.browsing;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.facets.FacetConfig;
import com.google.refine.util.ParsingUtilities;


public class EngineConfig  {
    
    protected final List<FacetConfig> _facets;
    protected final Mode _mode;
    
    @JsonCreator
    public EngineConfig(
            @JsonProperty("facets")
            List<FacetConfig> facets,
            @JsonProperty("mode")
            Mode mode) {
        _facets = facets == null ? Collections.emptyList() : facets;
        _mode = mode == null ? Mode.RowBased : mode;
    }
    
    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }
    
    @JsonProperty("facets")
    public List<FacetConfig> getFacetConfigs() {
        return _facets;
    }
    
    public static EngineConfig reconstruct(String json) {
        if(json == null) {
            return new EngineConfig(Collections.emptyList(), Mode.RowBased);
        }
        try {
            return ParsingUtilities.mapper.readValue(json, EngineConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
