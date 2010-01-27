package com.metaweb.gridlock.browsing;

import java.util.LinkedList;
import java.util.List;

import com.metaweb.gridlock.browsing.filters.RowFilter;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public class ConjunctiveFilteredRows implements FilteredRows {
	final protected List<RowFilter> _rowFilters = new LinkedList<RowFilter>();
	
	public void add(RowFilter rowFilter) {
		_rowFilters.add(rowFilter);
	}
	
	@Override
	public void accept(Project project, RowVisitor visitor) {
		for (Row row : project.rows) {
			boolean ok = true;
			for (RowFilter rowFilter : _rowFilters) {
				if (!rowFilter.filterRow(row)) {
					ok = false;
					break;
				}
			}
			
			if (ok) {
				visitor.visit(row);
			}
		}
	}
}
