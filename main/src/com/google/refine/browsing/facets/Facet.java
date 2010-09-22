package com.google.refine.browsing.facets;

import org.json.JSONObject;

import com.google.refine.Jsonizable;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.model.Project;

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
