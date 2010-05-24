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
import com.metaweb.gridworks.browsing.util.ConjunctiveFilteredRecords;
import com.metaweb.gridworks.browsing.util.ConjunctiveFilteredRows;
import com.metaweb.gridworks.browsing.util.FilteredRecordsAsFilteredRows;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

/**
 * Faceted browsing engine.
 */
public class Engine implements Jsonizable {
	static public enum Mode {
		RowBased,
		RecordBased
	}
	
    public final static String INCLUDE_DEPENDENT = "includeDependent";
    public final static String MODE = "mode";
    public final static String MODE_ROW_BASED = "row-based";
    public final static String MODE_RECORD_BASED = "record-based";
    
    protected Project         _project;
    protected List<Facet>     _facets = new LinkedList<Facet>();
    protected Mode			  _mode = Mode.RowBased;
    
    static public String modeToString(Mode mode) {
    	return mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED;
    }
    static public Mode stringToMode(String s) {
    	return MODE_ROW_BASED.equals(s) ? Mode.RowBased : Mode.RecordBased;
    }
    
    public Engine(Project project) {
        _project  = project;
    }
    
    public Mode getMode() {
    	return _mode;
    }
    public void setMode(Mode mode) {
    	_mode = mode;
    }
    
    public FilteredRows getAllRows() {
        return new FilteredRows() {
			@Override
			public void accept(Project project, RowVisitor visitor) {
		    	try {
		    		visitor.start(project);
		    		
			        int c = project.rows.size();
			        for (int rowIndex = 0; rowIndex < c; rowIndex++) {
			            Row row = project.rows.get(rowIndex);
			            visitor.visit(project, rowIndex, row);
			        }
		    	} finally {
		    		visitor.end(project);
		    	}
			}
        };
    }
    
    public FilteredRows getAllFilteredRows() {
        return getFilteredRows(null);
    }

    public FilteredRows getFilteredRows(Facet except) {
    	if (_mode == Mode.RecordBased) {
    		return new FilteredRecordsAsFilteredRows(getFilteredRecords(except));
    	} else if (_mode == Mode.RowBased) {
	        ConjunctiveFilteredRows cfr = new ConjunctiveFilteredRows();
	        for (Facet facet : _facets) {
	            if (facet != except) {
	                RowFilter rowFilter = facet.getRowFilter(_project);
	                if (rowFilter != null) {
	                    cfr.add(rowFilter);
	                }
	            }
	        }
	        return cfr;
    	}
    	throw new InternalError("Unknown mode.");
    }
    
    public FilteredRecords getAllRecords() {
        return new FilteredRecords() {
			@Override
			public void accept(Project project, RecordVisitor visitor) {
		    	try {
		    		visitor.start(project);
		    		
			        int c = project.recordModel.getRecordCount();
			        for (int r = 0; r < c; r++) {
			            visitor.visit(project, project.recordModel.getRecord(r));
			        }
		    	} finally {
		    		visitor.end(project);
		    	}
			}
        };
    }
    
    public FilteredRecords getFilteredRecords() {
    	return getFilteredRecords(null);
    }
    
    public FilteredRecords getFilteredRecords(Facet except) {
    	if (_mode == Mode.RecordBased) {
    		ConjunctiveFilteredRecords cfr = new ConjunctiveFilteredRecords();
            for (Facet facet : _facets) {
                if (facet != except) {
                    RecordFilter recordFilter = facet.getRecordFilter(_project);
                    if (recordFilter != null) {
                        cfr.add(recordFilter);
                    }
                }
            }
            return cfr;
    	}
    	throw new InternalError("This method should not be called when the engine is not in record mode.");
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
        
        // for backward compatibility
        if (o.has(INCLUDE_DEPENDENT) && !o.isNull(INCLUDE_DEPENDENT)) {
            _mode = o.getBoolean(INCLUDE_DEPENDENT) ? Mode.RecordBased : Mode.RowBased;
        }
        
        if (o.has(MODE) && !o.isNull(MODE)) {
        	_mode = MODE_ROW_BASED.equals(o.getString(MODE)) ? Mode.RowBased : Mode.RecordBased;
        }
    }
        
    public void computeFacets() throws JSONException {
    	if (_mode == Mode.RowBased) {
	        for (Facet facet : _facets) {
	            FilteredRows filteredRows = getFilteredRows(facet);
	            
	            facet.computeChoices(_project, filteredRows);
	        }
    	} else if (_mode == Mode.RecordBased) {
	        for (Facet facet : _facets) {
	            FilteredRecords filteredRecords = getFilteredRecords(facet);
	            
	            facet.computeChoices(_project, filteredRecords);
	        }
    	} else {
        	throw new InternalError("Unknown mode.");
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
        writer.key(MODE); writer.value(_mode == Mode.RowBased ? MODE_ROW_BASED : MODE_RECORD_BASED);
        writer.endObject();
    }
}
