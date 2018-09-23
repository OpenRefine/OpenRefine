package com.google.refine.browsing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.facets.FacetConfig;
import com.google.refine.browsing.facets.ListFacet.ListFacetConfig;
import com.google.refine.browsing.facets.RangeFacet.RangeFacetConfig;
import com.google.refine.browsing.facets.ScatterplotFacet.ScatterplotFacetConfig;
import com.google.refine.browsing.facets.TextSearchFacet.TextSearchFacetConfig;
import com.google.refine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;


public class EngineConfig implements Jsonizable {
    
    protected final List<FacetConfig> _facets;
    protected final Mode _mode;
    
    public EngineConfig(List<FacetConfig> facets, Mode mode) {
        _facets = facets;
        _mode = mode;
    }
    
    @JsonProperty("mode")
    public Mode getMode() {
        return _mode;
    }
    
    @JsonProperty("facets")
    public List<FacetConfig> getFacetConfigs() {
        return _facets;
    }
    
    public static EngineConfig reconstruct(JSONObject o) {
        if (o == null) {
            return new EngineConfig(Collections.emptyList(), Mode.RowBased);
        }

        List<FacetConfig> facets = new LinkedList<>();
        if (o.has("facets") && !o.isNull("facets")) {
            JSONArray a = o.getJSONArray("facets");
            int length = a.length();

            for (int i = 0; i < length; i++) {
                JSONObject fo = a.getJSONObject(i);
                String type = fo.has("type") ? fo.getString("type") : "list";

                FacetConfig facet = null;
                if ("list".equals(type)) {
                    facet = new ListFacetConfig();
                } else if ("range".equals(type)) {
                    facet = new RangeFacetConfig();
                } else if ("timerange".equals(type)) {
                    facet = new TimeRangeFacetConfig();
                } else if ("scatterplot".equals(type)) {
                    facet = new ScatterplotFacetConfig();
                } else if ("text".equals(type)) {
                    facet = new TextSearchFacetConfig();
                }

                if (facet != null) {
                    facet.initializeFromJSON(fo);
                    facets.add(facet);
                }
            }
        }

        Mode mode = Mode.RowBased;
        // for backward compatibility
        if (o.has(Engine.INCLUDE_DEPENDENT) && !o.isNull(Engine.INCLUDE_DEPENDENT)) {
            mode = o.getBoolean(Engine.INCLUDE_DEPENDENT) ? Mode.RecordBased : Mode.RowBased;
        }

        if (o.has(Engine.MODE) && !o.isNull(Engine.MODE)) {
            mode = Engine.MODE_ROW_BASED.equals(o.getString(Engine.MODE)) ? Mode.RowBased : Mode.RecordBased;
        }
        
        return new EngineConfig(facets, mode);
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("facets");
        writer.array();
        for (FacetConfig facet : _facets) {
            facet.write(writer, options);
        }
        writer.endArray();
        writer.key(Engine.MODE); writer.value(_mode == Mode.RowBased ? Engine.MODE_ROW_BASED : Engine.MODE_RECORD_BASED);
        writer.endObject();
    }

}
