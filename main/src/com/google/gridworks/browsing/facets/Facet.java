package com.google.gridworks.browsing.facets;

import org.json.JSONObject;

import com.google.gridworks.Jsonizable;
import com.google.gridworks.browsing.FilteredRecords;
import com.google.gridworks.browsing.FilteredRows;
import com.google.gridworks.browsing.RecordFilter;
import com.google.gridworks.browsing.RowFilter;
import com.google.gridworks.model.Project;

/**
 * Interface of facets.
 */
public interface Facet extends Jsonizable {
    public RowFilter getRowFilter(Project project);
    
    public RecordFilter getRecordFilter(Project project);
    
    public void computeChoices(Project project, FilteredRows filteredRows);
    
    public void computeChoices(Project project, FilteredRecords filteredRecords);
    
    public void initializeFromJSON(Project project, JSONObject o) throws Exception;
}
