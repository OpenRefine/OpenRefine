/*

Copyright 2011, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.operations.cell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.sorting.SortingConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reshapes a table which contains key and value columns,
 * such that the repeating contents in the key column become new column names,
 * and the contents of the value column are spread in the new columns.
 */
public class KeyValueColumnizeOperation implements Operation {
    final protected String  _keyColumnName;
    final protected String  _valueColumnName;
    final protected String  _noteColumnName;

    @JsonCreator
    public KeyValueColumnizeOperation(
        @JsonProperty("keyColumnName")
        String keyColumnName,
        @JsonProperty("valueColumnName")
        String valueColumnName,
        @JsonProperty("noteColumnName")
        String noteColumnName
    ) {
        _keyColumnName = keyColumnName;
        _valueColumnName = valueColumnName;
        _noteColumnName = noteColumnName;
    }
    
    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
    }
    
    @JsonProperty("valueColumnName")
    public String getValueColumnName() {
        return _valueColumnName;
    }
    
    @JsonProperty("noteColumnName")
    public String getNoteColumnName() {
        return _noteColumnName;
    }

    @Override
    public String getDescription() {
        return "Columnize by key column " +
            _keyColumnName + " and value column " + _valueColumnName +
            (_noteColumnName != null ? (" with note column " + _noteColumnName) : "");
    }
    
    @Override
    public Change createChange() {
    	return new KeyValueColumnizeChange();
    }
    
    public class KeyValueColumnizeChange implements Change {

    	@Override
    	public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
    		ColumnModel columnModel = projectState.getColumnModel();
            int keyColumnIndex = columnModel.getColumnIndexByName(_keyColumnName);
            int valueColumnIndex = columnModel.getColumnIndexByName(_valueColumnName);
            int noteColumnIndex = _noteColumnName == null ? -1 :
                columnModel.getColumnIndexByName(_noteColumnName);
            ColumnMetadata noteColumn = _noteColumnName == null ? null :
                columnModel.getColumnByName(_noteColumnName);
            
            ColumnModel newColumns = new ColumnModel(Collections.emptyList());
            ColumnModel newNoteColumns = new ColumnModel(Collections.emptyList());
            Map<String, Integer> keyValueToColumn = new HashMap<>();
            Map<String, Integer> keyValueToNoteColumn = new HashMap<>();
            Map<String, List<Cell>> groupByCellValuesToRow = new HashMap<>();
            
            List<Integer> unchangedColumns = new ArrayList<>();
            List<ColumnMetadata> oldColumns = columnModel.getColumns();
            for (int i = 0; i < oldColumns.size(); i++) {
                if (i != keyColumnIndex &&
                    i != valueColumnIndex &&
                    i != noteColumnIndex) {
                    unchangedColumns.add(i);
                    newColumns = newColumns.appendUnduplicatedColumn(oldColumns.get(i));
                }
            }
            
            List<List<Cell>> newBaseRows = new ArrayList<>();
            List<List<Cell>> newNotes = new ArrayList<>();
            
            List<Cell> baseRow = null;
            List<Cell> notes = null;
            List<List<Cell>> currentBaseRows = new ArrayList<>();
            List<List<Cell>> currentNotes = new ArrayList<>();
            
            String recordKey = null; // key which indicates the start of a record
            if (unchangedColumns.isEmpty()) {
            	pushCurrentRows(newBaseRows, currentBaseRows, newNotes, currentNotes);
            	baseRow = new ArrayList<>(newColumns.getColumns().size());
                currentBaseRows.add(baseRow);
                notes = new ArrayList<>(1);
                currentNotes.add(notes);
            }

            for (IndexedRow indexedRow : projectState.iterateRows(RowFilter.ANY_ROW, SortingConfig.NO_SORTING)) {
                Row oldRow = indexedRow.getRow();
                
                Object key = oldRow.getCellValue(keyColumnIndex);
                if (!ExpressionUtils.isNonBlankData(key)) {
                    if (unchangedColumns.isEmpty()) {
                        // For degenerate 2 column case (plus optional note column), 
                        // start a new row when we hit a blank line
                    	pushCurrentRows(newBaseRows, currentBaseRows, newNotes, currentNotes);
                    	baseRow = new ArrayList<>(newColumns.getColumns().size());
                        currentBaseRows.add(baseRow);
                        notes = new ArrayList<>(newNoteColumns.getColumns().size());
                        currentNotes.add(notes);
                    } else {
                        // Copy rows with no key
                        newBaseRows.add(buildNewRow(keyColumnIndex, valueColumnIndex, noteColumnIndex, oldRow));
                        newNotes.add(Collections.emptyList());
                    }
                    continue; 
                }
                
                String keyString = key.toString();
                // Start a new row on our beginning of record key
                // TODO: Add support for processing in record mode instead of just by rows
                if (keyString.equals(recordKey) || recordKey == null) {
                	pushCurrentRows(newBaseRows, currentBaseRows, newNotes, currentNotes);
                	baseRow = new ArrayList<>(newColumns.getColumns().size());
                	currentBaseRows.add(baseRow);
                	notes = new ArrayList<>(newNoteColumns.getColumns().size());
                	currentNotes.add(notes);
                }
                Integer newColumn = keyValueToColumn.get(keyString);
                if (newColumn == null) {
                    // Allocate new column
                	String columnName = newColumns.getUnduplicatedColumnName(keyString);
                	int columnIndex = newColumns.getColumns().size();
                	newColumns = newColumns.insertUnduplicatedColumn(columnIndex,
                			new ColumnMetadata(columnName));
                    keyValueToColumn.put(keyString, columnIndex);
                    newColumn = columnIndex;

                    // We assume first key encountered is the beginning of record key
                    // TODO: make customizable?
                    if (recordKey == null) {
                        recordKey = keyString;
                    }
                }
                
                /*
                 * NOTE: If we have additional columns, we currently merge all rows that
                 * have identical values in those columns and then add our new columns.
                 */
                if (unchangedColumns.size() > 0) {
                    StringBuffer sb = new StringBuffer();
                    for (int c = 0; c < unchangedColumns.size(); c++) {
                        Object cellValue = oldRow.getCellValue(c);
                        if (c > 0) {
                            sb.append('\0');
                        }
                        if (cellValue != null) {
                            sb.append(cellValue.toString());
                        }
                    }
                    String unchangedCellValues = sb.toString();

                    baseRow = groupByCellValuesToRow.get(unchangedCellValues);
                    if (baseRow == null ||
                            (newColumn < baseRow.size() && baseRow.get(newColumn) != null)) {
                        baseRow = buildNewRow(keyColumnIndex, valueColumnIndex, noteColumnIndex, oldRow);
                        groupByCellValuesToRow.put(unchangedCellValues, baseRow);
                        currentBaseRows.add(baseRow);
                        currentNotes.add(new ArrayList<>());
                    }
                }
                
                Cell cell = oldRow.getCell(valueColumnIndex);
                if (unchangedColumns.size() == 0) {
                    int index = newColumn;
                    List<Cell> row = getAvailableRow(currentBaseRows, currentNotes, index);
                    if (row == null) {
                        row = new ArrayList<>();
                        currentBaseRows.add(row);
                        currentNotes.add(new ArrayList<>());
                    }
                    setCell(row, index, cell);
                } else {
                    // TODO: support repeating keys in this mode too
                    setCell(baseRow, newColumn, cell);
                }
                
                if (noteColumn != null) {
                    Object noteValue = oldRow.getCellValue(noteColumnIndex);
                    if (ExpressionUtils.isNonBlankData(noteValue)) {
                        Integer newNoteColumn = keyValueToNoteColumn.get(keyString);
                        if (newNoteColumn == null) {
                            // Allocate new column
                        	String name = newNoteColumns.getUnduplicatedColumnName(
                        			noteColumn.getName() + " : " + keyString);
                        	newNoteColumn = newNoteColumns.getColumns().size();
                        	newNoteColumns = newNoteColumns.insertUnduplicatedColumn(newNoteColumn,
                        			new ColumnMetadata(name));
                            keyValueToNoteColumn.put(keyString, newNoteColumn);
                        }
                        
                        int newNoteCellIndex = newNoteColumn;
                        Cell existingNewNoteCell = newNoteCellIndex < notes.size() ? notes.get(newNoteCellIndex) : null;
                        Object existingNewNoteValue = existingNewNoteCell == null ? null : existingNewNoteCell.value;
                        if (ExpressionUtils.isNonBlankData(existingNewNoteValue)) {
                            Cell concatenatedNoteCell = new Cell(
                                existingNewNoteValue.toString() + ";" + noteValue.toString(), null);
                            setCell(notes, newNoteCellIndex, concatenatedNoteCell);
                        } else {
                            setCell(notes, newNoteCellIndex, oldRow.getCell(noteColumnIndex));
                        }
                    }
                }
            }
            
            pushCurrentRows(newBaseRows, currentBaseRows, newNotes, currentNotes);
            
            ColumnModel finalColumnModel = newColumns;
            for(ColumnMetadata notesColumn : newNoteColumns.getColumns()) {
            	finalColumnModel = finalColumnModel.insertUnduplicatedColumn(
            			finalColumnModel.getColumns().size(), notesColumn);
            }
            
            // build the final grid of rows
            List<Row> finalRows = new ArrayList<>(newBaseRows.size());
            for (int i = 0; i != newBaseRows.size(); i++) {
            	padWithNulls(newBaseRows.get(i), newColumns.getColumns().size());
            	padWithNulls(newNotes.get(i), newNoteColumns.getColumns().size());
            	newBaseRows.get(i).addAll(newNotes.get(i));
            	Row row = new Row(newBaseRows.get(i));
            	if (!row.isEmpty()) {
            		finalRows.add(row);
            	}
            }
            
            return projectState.getDatamodelRunner().create(finalColumnModel, finalRows, projectState.getOverlayModels());
    	}
    	
        private List<Cell> getAvailableRow(List<List<Cell>> currentRows, List<List<Cell>> currentNotes, int index) {
            for (List<Cell> row : currentRows) {
                if (index >= row.size() || row.get(index) == null) {
                    return row;
                }
            }
            // If we couldn't find a row with an empty spot
            return null;
        }

        private List<Cell> buildNewRow(int keyColumnIndex, int valueColumnIndex, int noteColumnIndex, Row oldRow) {
        	List<Cell> cells = new ArrayList<>(oldRow.getCells().size());
            for (int c = 0; c < oldRow.getCells().size(); c++) {
            	if (c != keyColumnIndex && c != valueColumnIndex && c != noteColumnIndex) {
            		cells.add(oldRow.getCell(c));
            	}
            }
            return cells;
        }
        
        private void pushCurrentRows(List<List<Cell>> newBaseRows, List<List<Cell>> currentBaseRows, List<List<Cell>> newNotes, List<List<Cell>> currentNotes) {
        	newBaseRows.addAll(currentBaseRows);
        	newNotes.addAll(currentNotes);
        	currentBaseRows.clear();
        	currentNotes.clear();
        }
        
        private void setCell(List<Cell> cells, int index, Cell cell) {
        	while (cells.size() <= index) {
        		cells.add(null);
        	}
        	cells.set(index, cell);
        }
        
        private void padWithNulls(List<Cell> cells, int targetLength) {
        	while (cells.size() < targetLength) {
        		cells.add(null);
        	}
        }

    	@Override
    	public boolean isImmediate() {
    		return true;
    	}

    	@Override
    	public DagSlice getDagSlice() {
    		// TODO Auto-generated method stub
    		return null;
    	}

    }

}
