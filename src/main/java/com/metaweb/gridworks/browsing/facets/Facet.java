package com.metaweb.gridworks.browsing.facets;

import org.json.JSONObject;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.FilteredRecords;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RecordFilter;
import com.metaweb.gridworks.browsing.RowFilter;
import com.metaweb.gridworks.model.Project;

/**
 * Interface of facets.
 */
public interface Facet extends Jsonizable {
    public RowFilter getRowFilter();
    
    public RecordFilter getRecordFilter();
    
    public void computeChoices(Project project, FilteredRows filteredRows);
    
    public void computeChoices(Project project, FilteredRecords filteredRecords);
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception;
}
