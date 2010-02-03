package com.metaweb.gridworks.browsing.facets;

import org.json.JSONObject;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;

public interface Facet extends Jsonizable {
	public RowFilter getRowFilter();
	
	public void computeChoices(Project project, FilteredRows filteredRows);
	
	public void initializeFromJSON(JSONObject o) throws Exception;
}
