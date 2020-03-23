/*

Copyright 2010, Google Inc.
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

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.util.ParsingUtilities;

public class Column  {
    final private int       _cellIndex;
    final private String    _originalName;
    private String          _name;
    private ReconConfig     _reconConfig;
    private ReconStats      _reconStats;
    
    // from data package metadata Field.java:
    private String type = "";
    private String format = "default";
    private String title = "";
    private String description = "";
    private Map<String, Object> constraints = Collections.emptyMap();
    
    transient protected Map<String, Object> _precomputes;
    
    @JsonCreator
    public Column(
            @JsonProperty("cellIndex")
            int cellIndex,
            @JsonProperty("originalName")
            String originalName) {
        _cellIndex = cellIndex;
        _originalName = _name = originalName;
    }
    
    @JsonProperty("cellIndex")
    public int getCellIndex() {
        return _cellIndex;
    }

    @JsonProperty("originalName")
    public String getOriginalHeaderLabel() {
        return _originalName;
    }
    
    @JsonProperty("name")
    public void setName(String name) {
        this._name = name;
    }

    @JsonProperty("name")
    public String getName() {
        return _name;
    }

    @JsonProperty("reconConfig")
    public void setReconConfig(ReconConfig config) {
        this._reconConfig = config;
    }

    @JsonProperty("reconConfig")
    @JsonInclude(Include.NON_NULL)
    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    @JsonProperty("reconStats")
    public void setReconStats(ReconStats stats) {
        this._reconStats = stats;
    }

    @JsonProperty("reconStats")
    @JsonInclude(Include.NON_NULL)
    public ReconStats getReconStats() {
        return _reconStats;
    }
    
    /**
     * Clear all cached precomputed values.
     * <p>
     * If you are modifying something that requires this to be called, you
     * probably also need to call
     * {@link com.google.refine.LookupCacheManager#flushLookupsInvolvingProjectColumn(long, String)}
     * e.g. ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, column.getName())
     */
    public void clearPrecomputes() {
        if (_precomputes != null) {
            _precomputes.clear();
        }
    }
    
    public Object getPrecompute(String key) {
        if (_precomputes != null) {
            return _precomputes.get(key);
        }
        return null;
    }
    
    public void setPrecompute(String key, Object value) {
        if (_precomputes == null) {
            _precomputes = new HashMap<String, Object>();
        }
        _precomputes.put(key, value);
    }
    
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    
    @JsonProperty("format")
    public String getFormat() {
        return format;
    }

    @JsonProperty("format")
    public void setFormat(String format) {
        this.format = format;
    }

    
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }
    
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("constraints")
    public String getConstraintsString() {
        try {
            return ParsingUtilities.mapper.writeValueAsString(constraints);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }
    
    @JsonProperty("constraints")
    public void setConstraintsJson(String json) {
        try {
            setConstraints(ParsingUtilities.mapper.readValue(json, new TypeReference<Map<String,Object>>() {}));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, Object> getConstraints() {
        return constraints;
    }

    
    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public void save(Writer writer) {
        try {
            ParsingUtilities.defaultWriter.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static public Column load(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, Column.class);
    }
    
    @Override
    public String toString() {
        return _name;
    }
}
