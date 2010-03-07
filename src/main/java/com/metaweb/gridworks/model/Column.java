package com.metaweb.gridworks.model;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.model.recon.ReconConfig;
import com.metaweb.gridworks.util.ParsingUtilities;

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
}
