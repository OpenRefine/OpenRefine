/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.openrefine.browsing;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stores the configuration of facets and whether we are using row or
 * record mode.
 * In addition it also stores an optional limit on the number of rows (or records)
 * to aggregate when computing facets.
 * 
 * @author Antonin Delpeuch
 *
 */
public class EngineConfig  {
    
    protected final List<FacetConfig> _facets;
    protected final Mode _mode;
    protected final Long _aggregationLimit;
    
    public static final EngineConfig ALL_ROWS = new EngineConfig(Collections.emptyList(), Mode.RowBased);
    public static final EngineConfig ALL_RECORDS = new EngineConfig(Collections.emptyList(), Mode.RecordBased);
    
    /**
     * Creates a new EngineConfig from a list of facet
     * configurations and an engine mode. No limit on facet
     * aggregation.
     * 
     * @param facets
     * @param mode
     */
    public EngineConfig(List<FacetConfig> facets, Mode mode) {
        this(facets, mode, null);
    }
    
    /**
     * Creates a new EngineConfig from a list of facet
     * configurations, an engine mode and an aggregation limit.
     * 
     * A null aggregationLimit of means no limit (which
     * happens when the field is not specified in the JSON
     * serialization).
     * A zero aggregationLimit means no aggregation at all.
     * 
     * @param facets
     * @param mode
     * @param aggregationLimit 
     */
    @JsonCreator
    public EngineConfig(
            @JsonProperty("facets")
            List<FacetConfig> facets,
            @JsonProperty("mode")
            Mode mode,
            @JsonProperty("aggregationLimit")
            Long aggregationLimit) {
        _facets = facets == null ? Collections.emptyList() : facets;
        _mode = mode == null ? Mode.RowBased : mode;
        _aggregationLimit = aggregationLimit;
    }
    
    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }
    
    @JsonProperty("facets")
    public List<FacetConfig> getFacetConfigs() {
        return _facets;
    }
    
    @JsonProperty("aggregationLimit")
    @JsonInclude(Include.NON_NULL)
    public Long getAggregationLimit() {
        return _aggregationLimit;
    }
    
    /**
     * Computes the set of columns the facets depend on.
     * If the extraction of dependencies fails for some facet,
     * or if the engine uses the records mode, this returns null.
     * 
     * @return
     *    the set of column dependencies, or null
     */
    @JsonIgnore
    public Set<String> getColumnDependencies() {
        if (Mode.RecordBased.equals(_mode)) {
            return null;
        }
        Set<String> dependencies = new HashSet<>();
        for(FacetConfig facet : _facets) {
            Set<String> facetDependencies = facet.getColumnDependencies();
            if (facetDependencies == null) {
                return null;
            // only add the facet dependencies if the facet is actually used
            // for filtering.
            } else if (!facet.isNeutral()) {
                dependencies.addAll(facetDependencies);
            }
        }
        return dependencies;
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
