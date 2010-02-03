package com.metaweb.gridworks.browsing;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ConjunctiveFilteredRows implements FilteredRows {
	final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
	
	public void add(RowFilter rowFilter) {
		_rowFilters.add(rowFilter);
	}
	
	@Override
	public void accept(Project project, RowVisitor visitor) {
		for (int i = 0; i < project.rows.size(); i++) {
			Row row = project.rows.get(i);
			
			boolean ok = true;
			for (RowFilter rowFilter : _rowFilters) {
				if (!rowFilter.filterRow(project, i, row)) {
					ok = false;
					break;
				}
			}
			
			if (ok) {
				visitor.visit(project, i, row);
			}
		}
	}
}
