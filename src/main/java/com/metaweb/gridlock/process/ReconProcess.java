package com.metaweb.gridlock.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;

public class ReconProcess extends LongRunningProcess implements Runnable {
	static public class ReconEntry {
		final public int rowIndex;
		final public Cell cell;
		
		public ReconEntry(int rowIndex, Cell cell) {
			this.rowIndex = rowIndex;
			this.cell = cell;
		}
	}
	
	final protected Project				_project;
	final protected int					_cellIndex;
	final protected List<ReconEntry> 	_entries;
	final protected String 				_typeID;
	
	public ReconProcess(Project project, String description, int cellIndex, List<ReconEntry> entries, String typeID) {
		super(description);
		_project = project;
		_cellIndex = cellIndex;
		_entries = entries;
		_typeID = typeID;
	}
	
	@Override
	protected Runnable getRunnable() {
		return this;
	}
	
	@Override
	public void run() {
		Map<String, List<ReconEntry>> valueToEntries = new HashMap<String, List<ReconEntry>>();
		
		for (ReconEntry entry : _entries) {
			Object value = entry.cell.value;
			if (value != null && value instanceof String) {
				List<ReconEntry> entries2;
				if (valueToEntries.containsKey(value)) {
					entries2 = valueToEntries.get(value);
				} else {
					entries2 = new LinkedList<ReconEntry>();
					valueToEntries.put((String) value, entries2);
				}
				entries2.add(entry);
			}
		}
		
		List<String> values = new ArrayList<String>(valueToEntries.keySet());
		for (int i = 0; i < values.size(); i += 20) {
			_progress = i * 100 / values.size();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				if (_canceled) {
					break;
				}
			}
		}
		
		_project.processManager.onDoneProcess(this);
	}
}
