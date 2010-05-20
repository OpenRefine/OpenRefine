package com.metaweb.gridworks.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.ExpressionUtils;

public class RecordModel implements Jsonizable {
	final static public class CellDependency {
		final public int rowIndex;
		final public int cellIndex;
		
		public CellDependency(int rowIndex, int cellIndex) {
			this.rowIndex = rowIndex;
			this.cellIndex = cellIndex;
		}
	}
	
	final static public class RowDependency {
		public int recordIndex;
		public CellDependency[] cellDependencies;
		public List<Integer> contextRows;
	}
	
	protected List<RowDependency> _rowDependencies;
	protected List<Record> _records;
	
	public RowDependency getRowDependency(int rowIndex) {
		return _rowDependencies != null && rowIndex >= 0 && rowIndex < _rowDependencies.size() ?
				_rowDependencies.get(rowIndex) : null;
	}
	
	public int getRecordCount() {
		return _records.size();
	}
	
	public Record getRecord(int recordIndex) {
		return _records != null && recordIndex >= 0 && recordIndex < _records.size() ?
				_records.get(recordIndex) : null;
	}
	
	public Record getRecordOfRow(int rowIndex) {
		RowDependency rd = getRowDependency(rowIndex);
		if (rd != null) {
			if (rd.recordIndex < 0) {
				rd = getRowDependency(rd.contextRows.get(0));
			}
			return getRecord(rd.recordIndex);
		}
		return null;
	}
	
    synchronized public void write(JSONWriter writer, Properties options)
    	throws JSONException {

    	writer.object();
    	writer.key("hasRecords"); writer.value(_records.size() < _rowDependencies.size());
    	writer.endObject();
    }
    
    static protected class KeyedGroup {
        int[]   cellIndices;
        int     keyCellIndex;
    }

    synchronized public void update(Project project) {
    	synchronized (project) {
	    	List<Row> rows = project.rows;
	        int rowCount = rows.size();
	    	
	    	ColumnModel columnModel = project.columnModel;
	    	List<KeyedGroup> keyedGroups = computeKeyedGroups(columnModel);
	        int groupCount = keyedGroups.size();
	    	
	        int[] lastNonBlankRowsByGroup = new int[keyedGroups.size()];
	        for (int i = 0; i < lastNonBlankRowsByGroup.length; i++) {
	            lastNonBlankRowsByGroup[i] = -1;
	        }
	
	    	_rowDependencies = new ArrayList<RowDependency>(rowCount);
	    	
	        int recordIndex = 0;
	        for (int r = 0; r < rowCount; r++) {
	            Row row = rows.get(r);
	            RowDependency rowDependency = new RowDependency();
	
	            for (int g = 0; g < groupCount; g++) {
	                KeyedGroup group = keyedGroups.get(g);
	
	                if (!ExpressionUtils.isNonBlankData(row.getCellValue(group.keyCellIndex))) {
	                    int contextRowIndex = lastNonBlankRowsByGroup[g];
	                    if (contextRowIndex >= 0) {
	                        for (int dependentCellIndex : group.cellIndices) {
	                            if (ExpressionUtils.isNonBlankData(row.getCellValue(dependentCellIndex))) {
	                                setRowDependency(
	                                    project,
	                                    rowDependency,
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
	
	            if (rowDependency.cellDependencies != null && rowDependency.cellDependencies.length > 0) {
	            	rowDependency.recordIndex = -1;
	            	rowDependency.contextRows = new ArrayList<Integer>();
	                for (CellDependency cd : rowDependency.cellDependencies) {
	                    if (cd != null) {
	                    	rowDependency.contextRows.add(cd.rowIndex);
	                    }
	                }
	                Collections.sort(rowDependency.contextRows);
	            } else {
	            	rowDependency.recordIndex = recordIndex++;
	            }
	            
	            _rowDependencies.add(rowDependency);
	        }
	        
	    	_records = new ArrayList<Record>(recordIndex);
	    	if (recordIndex > 0) {
		    	recordIndex = 0;
		    	
		    	int recordRowIndex = 0;
		        for (int r = 1; r < rowCount; r++) {
		            RowDependency rd = _rowDependencies.get(r);
		            if (rd.recordIndex >= 0) {
		            	_records.add(new Record(recordRowIndex, r, recordIndex++));
		            	
		            	recordIndex = rd.recordIndex;
		            	recordRowIndex = r;
		            }
		        }
		        
            	_records.add(new Record(recordRowIndex, rowCount, recordIndex++));
	    	}
    	}
    }
    
    protected List<KeyedGroup> computeKeyedGroups(ColumnModel columnModel) {
        List<KeyedGroup> keyedGroups = new ArrayList<KeyedGroup>();
    	
        addRootKeyedGroup(columnModel, keyedGroups);

        for (ColumnGroup group : columnModel.columnGroups) {
            if (group.keyColumnIndex >= 0) {
                KeyedGroup keyedGroup = new KeyedGroup();
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

        Collections.sort(keyedGroups, new Comparator<KeyedGroup>() {
            public int compare(KeyedGroup o1, KeyedGroup o2) {
                return o2.cellIndices.length - o1.cellIndices.length; // larger groups first
            }
        });
        
        return keyedGroups;
    }

    protected void addRootKeyedGroup(ColumnModel columnModel, List<KeyedGroup> keyedGroups) {
        int count = columnModel.getMaxCellIndex() + 1;
        if (count > 0 && columnModel.getKeyColumnIndex() < columnModel.columns.size()) {
            KeyedGroup rootKeyedGroup = new KeyedGroup();

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

    protected void setRowDependency(
		Project project, 
		RowDependency rowDependency, 
		int cellIndex, 
		int contextRowIndex, 
		int contextCellIndex
	) {
        if (rowDependency.cellDependencies == null) {
            int count = project.columnModel.getMaxCellIndex() + 1;
            
        	rowDependency.cellDependencies = new CellDependency[count];
        }

        rowDependency.cellDependencies[cellIndex] = 
        	new CellDependency(contextRowIndex, contextCellIndex);
    }

}
