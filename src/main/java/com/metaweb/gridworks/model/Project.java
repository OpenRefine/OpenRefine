package com.metaweb.gridworks.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.History;
import com.metaweb.gridworks.process.ProcessManager;
import com.metaweb.gridworks.protograph.Protograph;

public class Project implements Serializable {
	private static final long serialVersionUID = -5089046824819472570L;
	
	final public long 			id;
	
	final public ColumnModel 	columnModel = new ColumnModel();
	final public List<Row> 		rows = new ArrayList<Row>();
	final public History 		history;
	public Protograph			protograph;
	
	transient public ProcessManager processManager;
	
	public Project() {
		id = Math.round(Math.random() * 1000000) + System.currentTimeMillis();
		history = new History(this);
		
		internalInitialize();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		internalInitialize();
	}
	
	protected void internalInitialize() {
		processManager = new ProcessManager();
		
		recomputeRowContextDependencies();
	}
	
	static protected class Group {
		int[] 	cellIndices;
		int 	keyCellIndex;
	}
	
	public void recomputeRowContextDependencies() {
		List<Group> keyedGroups = new ArrayList<Group>();
		
		addRootKeyedGroup(keyedGroups);
		
		for (ColumnGroup group : columnModel.columnGroups) {
			if (group.keyColumnIndex >= 0) {
				Group keyedGroup = new Group();
				keyedGroup.keyCellIndex = columnModel.columns.get(group.keyColumnIndex).getCellIndex();
				keyedGroup.cellIndices = new int[group.columnSpan - 1];
				
				int c = 0;
				for (int i = 0; i < group.columnSpan; i++) {
					int columnIndex = group.startColumnIndex + i;
					if (columnIndex != group.keyColumnIndex) {
						int cellIndex = columnModel.columns.get(columnIndex).getCellIndex();
						keyedGroup.cellIndices[c++] = cellIndex;
					}
				}
				
				keyedGroups.add(keyedGroup);
			}
		}
		
		Collections.sort(keyedGroups, new Comparator<Group>() {
			public int compare(Group o1, Group o2) {
				return o2.cellIndices.length - o1.cellIndices.length; // larger groups first
			}
		});
		
		int[] lastNonBlankRowsByGroup = new int[keyedGroups.size()];
		for (int i = 0; i < lastNonBlankRowsByGroup.length; i++) {
			lastNonBlankRowsByGroup[i] = -1;
		}
		
		int recordIndex = 0;
		for (int r = 0; r < rows.size(); r++) {
			Row row = rows.get(r);
			row.contextRowSlots = null;
			row.contextCellSlots = null;
			
			for (int g = 0; g < keyedGroups.size(); g++) {
				Group group = keyedGroups.get(g);
				
				if (!ExpressionUtils.isNonBlankData(row.getCellValue(group.keyCellIndex))) {
					int contextRowIndex = lastNonBlankRowsByGroup[g];
					if (contextRowIndex >= 0) {
						for (int dependentCellIndex : group.cellIndices) {
							if (ExpressionUtils.isNonBlankData(row.getCellValue(dependentCellIndex))) {
								setRowDependency(
									row, 
									dependentCellIndex, 
									contextRowIndex, 
									group.keyCellIndex
								);
							}
						}
					}
				} else {
					lastNonBlankRowsByGroup[g] = r;
				}
			}
			
			if (row.contextRowSlots != null) {
				row.recordIndex = -1;
				row.contextRows = new ArrayList<Integer>();
				for (int index : row.contextRowSlots) {
					if (index >= 0) {
						row.contextRows.add(index);
					}
				}
				Collections.sort(row.contextRows);
			} else {
				row.recordIndex = recordIndex++;
			}
		}
	}
	
	protected void addRootKeyedGroup(List<Group> keyedGroups) {
		int count = columnModel.getMaxCellIndex() + 1;
		if (count > 0 && columnModel.getKeyColumnIndex() < columnModel.columns.size()) {
			Group rootKeyedGroup = new Group();
			
			rootKeyedGroup.cellIndices = new int[count - 1];
			rootKeyedGroup.keyCellIndex = columnModel.columns.get(columnModel.getKeyColumnIndex()).getCellIndex();
			for (int i = 0; i < count; i++) {
				if (i < rootKeyedGroup.keyCellIndex) {
					rootKeyedGroup.cellIndices[i] = i;
				} else if (i > rootKeyedGroup.keyCellIndex) {
					rootKeyedGroup.cellIndices[i - 1] = i;
				}
			}
			keyedGroups.add(rootKeyedGroup);
		}
	}
	
	protected void setRowDependency(Row row, int cellIndex, int contextRowIndex, int contextCellIndex) {
		int count = columnModel.getMaxCellIndex() + 1;
		if (row.contextRowSlots == null || row.contextCellSlots == null) {
			row.contextRowSlots = new int[count];
			row.contextCellSlots = new int[count];
			
			for (int i = 0; i < count; i++) {
				row.contextRowSlots[i] = -1;
				row.contextCellSlots[i] = -1;
			}
		}
		
		row.contextRowSlots[cellIndex] = contextRowIndex;
		row.contextCellSlots[cellIndex] = contextCellIndex;
	}
}
