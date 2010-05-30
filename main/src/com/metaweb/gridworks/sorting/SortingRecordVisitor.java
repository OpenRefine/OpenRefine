package com.metaweb.gridworks.sorting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.metaweb.gridworks.browsing.RecordVisitor;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Record;
import com.metaweb.gridworks.sorting.Criterion.KeyMaker;

public class SortingRecordVisitor extends BaseSorter implements RecordVisitor {
	final protected RecordVisitor 	_visitor;
	protected List<Record>	 		_records;
	
	public SortingRecordVisitor(RecordVisitor visitor) {
		_visitor = visitor;
	}

	@Override
	public void start(Project project) {
		int count = project.recordModel.getRecordCount();
		_records = new ArrayList<Record>(count);
		_keys = new ArrayList<Object[]>(count);
	}

	@Override
	public void end(Project project) {
		_visitor.start(project);
		
		Collections.sort(_records, new Comparator<Record>() {
			Project project;
			
			Comparator<Record> init(Project project) {
				this.project = project;
				return this;
			}
			
			@Override
			public int compare(Record o1, Record o2) {
				return SortingRecordVisitor.this.compare(project, o1, o1.recordIndex, o2, o2.recordIndex);
			}
		}.init(project));
		
		for (Record record : _records) {
			_visitor.visit(project, record);
		}
		
		_visitor.end(project);
	}

	@Override
	public boolean visit(Project project, Record record) {
		_records.add(record);
		return false;
	}

	@Override
	protected Object makeKey(
			Project project, KeyMaker keyMaker, Criterion c, Object o, int index) {
		
		return keyMaker.makeKey(project, (Record) o);
	}
}
