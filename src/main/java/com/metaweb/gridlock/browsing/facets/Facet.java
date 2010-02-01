package com.metaweb.gridlock.browsing.facets;

import org.json.JSONObject;

import com.metaweb.gridlock.Jsonizable;
import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.filters.RowFilter;
import com.metaweb.gridlock.model.Project;

public interface Facet extends Jsonizable {
	public RowFilter getRowFilter();
	
	public void computeChoices(Project project, FilteredRows filteredRows);
	
	public void initializeFromJSON(JSONObject o) throws Exception;
}
