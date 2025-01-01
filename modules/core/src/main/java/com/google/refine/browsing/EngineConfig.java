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

package com.google.refine.browsing;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.facets.FacetConfig;
import com.google.refine.util.ParsingUtilities;

public class EngineConfig {

    protected final List<FacetConfig> _facets;
    protected final Mode _mode;

    private static final EngineConfig _default = new EngineConfig(List.of(), Mode.RowBased);

    @JsonCreator
    public EngineConfig(
            @JsonProperty("facets") List<FacetConfig> facets,
            @JsonProperty("mode") Mode mode) {
        _facets = facets == null ? Collections.emptyList() : facets;
        _mode = mode == null ? Mode.RowBased : mode;
    }

    public static EngineConfig defaultRowBased() {
        return _default;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }

    @JsonProperty("facets")
    public List<FacetConfig> getFacetConfigs() {
        return _facets;
    }

    /**
     * Checks that all facets in this engine config are valid (rely on syntactically correct expressions, don't contain
     * contradictory options).
     * 
     * @throws IllegalArgumentException
     *             if not
     */
    public void validate() {
        _facets.stream().forEach(facetConfig -> facetConfig.validate());
    }

    /**
     * Returns an approximation of the names of the columns this engine depends on. This approximation is designed to be
     * safe: if a set of column names is returned, then the engine does not read any other column than the ones
     * mentioned, regardless of the data it is executed on.
     *
     * @return {@link Optional#empty()} if the columns could not be isolated: in this case, the engine might depend on
     *         all columns in the project. Note that this is different from returning an empty set, which means that the
     *         engine does not depend on any column (for instance, if there are no facets).
     */
    @JsonIgnore
    public Optional<Set<String>> getColumnDependencies() {
        Set<String> result = new HashSet<>();
        for (FacetConfig config : _facets) {
            Optional<Set<String>> dependencies = config.getColumnDependencies();
            if (dependencies.isEmpty()) {
                return Optional.empty();
            } else {
                result.addAll(dependencies.get());
            }
        }
        return Optional.of(result);
    }

    /**
     * @deprecated This method returns null when its argument is invalid, which is bad practice. Use
     *             {@link EngineConfig#deserialize(String)} instead.
     */
    @Deprecated(since = "3.9")
    public static EngineConfig reconstruct(String json) {
        if (json == null) {
            return new EngineConfig(Collections.emptyList(), Mode.RowBased);
        }
        try {
            return ParsingUtilities.mapper.readValue(json, EngineConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize an engine config from its JSON representation (non-null)
     * 
     * @throws IllegalArgumentException
     *             if the JSON format is invalid.
     */
    public static EngineConfig deserialize(String json) {
        try {
            return ParsingUtilities.mapper.readValue(json, EngineConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
