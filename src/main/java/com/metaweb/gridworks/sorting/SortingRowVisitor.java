package com.metaweb.gridworks.sorting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.sorting.Criterion.KeyMaker;

public class SortingRowVisitor extends BaseSorter implements RowVisitor {
	final protected RowVisitor 	_visitor;
	protected List<IndexedRow> 	_indexedRows;
	
	static protected class IndexedRow {
		final int index;
		final Row row;
		
		IndexedRow(int index, Row row) {
			this.index = index;
			this.row = row;
		}
	}
	
	public SortingRowVisitor(RowVisitor visitor) {
		_visitor = visitor;
	}

	@Override
	public void start(Project project) {
		int count = project.rows.size();
		_indexedRows = new ArrayList<IndexedRow>(count);
		_keys = new ArrayList<Object[]>(count);
	}

	@Override
	public void end(Project project) {
		_visitor.start(project);
		
		Collections.sort(_indexedRows, new Comparator<IndexedRow>() {
			Project project;
			
			Comparator<IndexedRow> init(Project project) {
				this.project = project;
				return this;
			}
			
			@Override
			public int compare(IndexedRow o1, IndexedRow o2) {
				return SortingRowVisitor.this.compare(project, o1.row, o1.index, o2.row, o2.index);
			}
		}.init(project));
		
		for (IndexedRow indexedRow : _indexedRows) {
			_visitor.visit(project, indexedRow.index, indexedRow.row);
		}
		
		_visitor.end(project);
	}

	@Override
	public boolean visit(Project project, int rowIndex, Row row) {
		_indexedRows.add(new IndexedRow(rowIndex, row));
		return false;
	}

	@Override
	protected Object makeKey(
			Project project, KeyMaker keyMaker, Criterion c, Object o, int index) {
		
		return keyMaker.makeKey(project, (Row) o, index);
	}
}
