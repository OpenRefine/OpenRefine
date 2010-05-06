package com.metaweb.gridworks.browsing;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.facets.Facet;
import com.metaweb.gridworks.browsing.facets.ListFacet;
import com.metaweb.gridworks.browsing.facets.RangeFacet;
import com.metaweb.gridworks.browsing.facets.ScatterplotFacet;
import com.metaweb.gridworks.browsing.facets.TextSearchFacet;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;

/**
 * Faceted browsing engine.
 */
public class Engine implements Jsonizable {
    protected Project         _project;
    protected List<Facet>     _facets = new LinkedList<Facet>();
    protected boolean         _includeDependent;
    
    public final static String INCLUDE_DEPENDENT = "includeDependent";
    
    public Engine(Project project) {
        _project  = project;
    }
    
    public FilteredRows getAllRows() {
        return new ConjunctiveFilteredRows(false, false);
    }
    
    public FilteredRows getAllFilteredRows(boolean includeContextual) {
        return getFilteredRows(null, includeContextual);
    }

    public FilteredRows getFilteredRows(Facet except, boolean includeContextual) {
        ConjunctiveFilteredRows cfr = new ConjunctiveFilteredRows(includeContextual, _includeDependent);
        for (Facet facet : _facets) {
            if (facet != except) {
                RowFilter rowFilter = facet.getRowFilter();
                if (rowFilter != null) {
                    cfr.add(rowFilter);
                }
            }
        }
        return cfr;
    }
    
    public void initializeFromJSON(JSONObject o) throws Exception {
        if (o == null) {
            return;
        }
        
        if (o.has("facets") && !o.isNull("facets")) {
            JSONArray a = o.getJSONArray("facets");
            int length = a.length();
            
            for (int i = 0; i < length; i++) {
                JSONObject fo = a.getJSONObject(i);
                String type = fo.has("type") ? fo.getString("type") : "list";
                
                Facet facet = null;
                if ("list".equals(type)) {
                    facet = new ListFacet();
                } else if ("range".equals(type)) {
                    facet = new RangeFacet();
                } else if ("scatterplot".equals(type)) {
                    facet = new ScatterplotFacet();
                } else if ("text".equals(type)) {
                    facet = new TextSearchFacet();
                }
                
                if (facet != null) {
                    facet.initializeFromJSON(_project, fo);
                    _facets.add(facet);
                }
            }
        }
        
        if (o.has(INCLUDE_DEPENDENT) && !o.isNull(INCLUDE_DEPENDENT)) {
            _includeDependent = o.getBoolean(INCLUDE_DEPENDENT);
        }
    }
        
    public void computeFacets() throws JSONException {
        for (Facet facet : _facets) {
            FilteredRows filteredRows = getFilteredRows(facet, false);
            
            facet.computeChoices(_project, filteredRows);
        }
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("facets");
            writer.array();
            for (Facet facet : _facets) {
                facet.write(writer, options);
            }
            writer.endArray();
        writer.key(INCLUDE_DEPENDENT); writer.value(_includeDependent);
        writer.endObject();
    }
}
