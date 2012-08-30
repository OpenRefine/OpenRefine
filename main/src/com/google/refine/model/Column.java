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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.InterProjectModel;
import com.google.refine.Jsonizable;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.util.ParsingUtilities;

public class Column implements Jsonizable {
    final private int       _cellIndex;
    final private String    _originalName;
    private String          _name;
    private ReconConfig     _reconConfig;
    private ReconStats      _reconStats;
    
    transient protected Map<String, Object> _precomputes;
    
    public Column(int cellIndex, String originalName) {
        _cellIndex = cellIndex;
        _originalName = _name = originalName;
    }
    
    public int getCellIndex() {
        return _cellIndex;
    }

    public String getOriginalHeaderLabel() {
        return _originalName;
    }
    
    public void setName(String name) {
        this._name = name;
    }

    public String getName() {
        return _name;
    }

    public void setReconConfig(ReconConfig config) {
        this._reconConfig = config;
    }

    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    public void setReconStats(ReconStats stats) {
        this._reconStats = stats;
    }

    public ReconStats getReconStats() {
        return _reconStats;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("cellIndex"); writer.value(_cellIndex);
        writer.key("originalName"); writer.value(_originalName);
        writer.key("name"); writer.value(_name);
        if (_reconConfig != null) {
            writer.key("reconConfig");
            _reconConfig.write(writer, options);
        }
        if (_reconStats != null) {
            writer.key("reconStats");
            _reconStats.write(writer, options);
        }
        writer.endObject();
    }
    
    /**
     * Clear all cached precomputed values.
     * <p>
     * If you are modifying something that requires this to be called, you
     * probably also need to call
     * {@link InterProjectModel#flushJoinsInvolvingProjectColumn(long, String)}.
     * e.g. ProjectManager.singleton.getInterProjectModel().flushJoinsInvolvingProjectColumn(project.id, column.getName())
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
    
    public void save(Writer writer) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, new Properties());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public Column load(String s) throws Exception {
        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(s);
        Column column = new Column(obj.getInt("cellIndex"), obj.getString("originalName"));
        
        column._name = obj.getString("name");
        if (obj.has("reconConfig")) {
            column._reconConfig = ReconConfig.reconstruct(obj.getJSONObject("reconConfig"));
        }
        if (obj.has("reconStats")) {
            column._reconStats = ReconStats.load(obj.getJSONObject("reconStats"));
        }
        
        return column;
    }
    
    @Override
    public String toString() {
        return _name;
    }
}
