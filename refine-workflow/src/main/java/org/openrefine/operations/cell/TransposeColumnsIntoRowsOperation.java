/*

Copyright 2010, Google Inc.
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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.openrefine.history.dag.DagSlice;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowBuilder;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.sorting.SortingConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransposeColumnsIntoRowsOperation implements Operation {
    @JsonProperty("startColumnName")
    final protected String  _startColumnName;
    @JsonProperty("columnCount")
    final protected int     _columnCount;
    @JsonProperty("ignoreBlankCells")
    final protected boolean _ignoreBlankCells;
    @JsonProperty("fillDown")
    final protected boolean _fillDown;
    
    @JsonProperty("combinedColumnName")
    @JsonInclude(Include.NON_NULL)
    final protected String  _combinedColumnName;
    @JsonIgnore
    final protected boolean _prependColumnName;
    @JsonProperty("separator")
    final protected String  _separator;
    
    @JsonProperty("keyColumnName")
    final protected String  _keyColumnName;
    @JsonProperty("valueColumnName")
    final protected String  _valueColumnName;
    
    @JsonProperty("prependColumnName")
    @JsonInclude(Include.NON_NULL)
    public Boolean getPrependColumnName() {
        return _combinedColumnName == null ? null : _prependColumnName; 
    }

    @JsonCreator
    static public TransposeColumnsIntoRowsOperation deserialize(
            @JsonProperty("combinedColumnName")
            String combinedColumnName,
            @JsonProperty("startColumnName")
            String startColumnName,
            @JsonProperty("columnCount")
            int columnCount,
            @JsonProperty("ignoreBlankCells")
            Boolean ignoreBlankCells,
            @JsonProperty("fillDown")
            Boolean fillDown,
            @JsonProperty("prependColumnName")
            boolean prependColumnName,
            @JsonProperty("separator")
            String separator,
            @JsonProperty("keyColumnName")
            String keyColumnName,
            @JsonProperty("valueColumnName")
            String valueColumnName) {
        ignoreBlankCells = ignoreBlankCells == null ? true : ignoreBlankCells;
        fillDown = fillDown == null ? false : fillDown;
        if (combinedColumnName != null) {
            return new TransposeColumnsIntoRowsOperation(
                    startColumnName,
                    columnCount,
                    ignoreBlankCells,
                    fillDown,
                    combinedColumnName,
                    prependColumnName,
                    separator);
        } else {
            return new TransposeColumnsIntoRowsOperation(
                    startColumnName,
                    columnCount,
                    ignoreBlankCells,
                    fillDown,
                    keyColumnName,
                    valueColumnName);
        }
    }
    
    public TransposeColumnsIntoRowsOperation(
        String  startColumnName,
        int     columnCount,
        boolean ignoreBlankCells,
        boolean fillDown,
        String  combinedColumnName,
        boolean prependColumnName,
        String  separator
    ) {
    	Validate.notNull(startColumnName);
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _ignoreBlankCells = ignoreBlankCells;
        _fillDown = fillDown;
        
        _combinedColumnName = combinedColumnName;
        _prependColumnName = prependColumnName;
        _separator = separator;
        
        _keyColumnName = null;
        _valueColumnName = null;
    }
    
    public TransposeColumnsIntoRowsOperation(
        String  startColumnName,
        int     columnCount,
        boolean ignoreBlankCells,
        boolean fillDown,
        String  keyColumnName,
        String  valueColumnName
    ) {
    	Validate.notNull(startColumnName);
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _ignoreBlankCells = ignoreBlankCells;
        _fillDown = fillDown;
        
        _combinedColumnName = null;
        _prependColumnName = false;
        _separator = null;
        
        _keyColumnName = keyColumnName;
        _valueColumnName = valueColumnName;
    }

    @Override
    public String getDescription() {
        if (_combinedColumnName != null) {
            if (_columnCount > 0) {
                return "Transpose cells in " + _columnCount +
                    " column(s) starting with " + _startColumnName +
                    " into rows in one new column named " + _combinedColumnName;
            } else {
                return "Transpose cells in columns starting with " +
                    _startColumnName +
                    " into rows in one new column named " + _combinedColumnName;
            }
        } else {
            if (_columnCount > 0) {
                return "Transpose cells in " + _columnCount +
                    " column(s) starting with " + _startColumnName +
                    " into rows in two new columns named " +
                    _keyColumnName + " and " + _valueColumnName;
            } else {
                return "Transpose cells in columns starting with " +
                    _startColumnName +
                    " into rows in two new columns named " +
                    _keyColumnName + " and " + _valueColumnName;
            }
        }
    }
    
    @Override
    public Change createChange() {
    	return new TransposeColumnsIntoRowsChange();
    }
    
    public class TransposeColumnsIntoRowsChange implements Change {

    	@Override
    	public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
    		ColumnModel columnModel = projectState.getColumnModel();
            if (_combinedColumnName != null) {
                if (columnModel.getColumnByName(_combinedColumnName) != null) {
                    throw new DoesNotApplyException("Another column already named " + _combinedColumnName);
                }
            } else {
                if (columnModel.getColumnByName(_keyColumnName) != null) {
                    throw new DoesNotApplyException("Another column already named " + _keyColumnName);
                }
                if (columnModel.getColumnByName(_valueColumnName) != null) {
                    throw new DoesNotApplyException("Another column already named " + _valueColumnName);
                }
            }
            
            List<ColumnMetadata> newColumns = new ArrayList<>();
            List<ColumnMetadata> oldColumns = columnModel.getColumns();
            
            int startColumnIndex = oldColumns.size();
            int columnCount = _columnCount;
            if (_columnCount > 0) {
                int columnsLeftToTranspose = _columnCount;
                for (int c = 0; c < oldColumns.size(); c++) {
                    ColumnMetadata column = oldColumns.get(c);
                    if (columnsLeftToTranspose == 0) {
                        // This column is beyond the columns to transpose
                        newColumns.add(column);
                    } else if (columnsLeftToTranspose < _columnCount) {
                        // This column is a column to transpose, but not the first
                        // nothing to do
                        
                        columnsLeftToTranspose--;
                    } else if (_startColumnName.equals(column.getName())) {
                        // This is the first column to transpose
                        
                        startColumnIndex = c;
                        
                        if (_combinedColumnName != null) {
                            newColumns.add(new ColumnMetadata(_combinedColumnName));
                        } else {
                            newColumns.add(new ColumnMetadata(_keyColumnName));
                            newColumns.add(new ColumnMetadata(_valueColumnName));
                        }
                        
                        columnsLeftToTranspose--;
                    } else {
                        // This column is before all columns to transpose
                        newColumns.add(column);
                    }
                }
            } else {
                for (int c = 0; c < oldColumns.size(); c++) {
                    ColumnMetadata column = oldColumns.get(c);
                    if (_startColumnName.equals(column.getName())) {
                        // This is the first column to transpose
                        
                        startColumnIndex = c;
                        
                        if (_combinedColumnName != null) {
                            newColumns.add(new ColumnMetadata( _combinedColumnName));
                        } else {
                            newColumns.add(new ColumnMetadata(_keyColumnName));
                            newColumns.add(new ColumnMetadata(_valueColumnName));
                        }
                        break;
                    } else {
                        // This column is before all columns to transpose
                        newColumns.add(column);
                    }
                }
                columnCount = oldColumns.size() - startColumnIndex;
            }
            
            List<RowBuilder> newRows = new ArrayList<>();
            for (IndexedRow indexedRow : projectState.iterateRows(RowFilter.ANY_ROW, SortingConfig.NO_SORTING)) {
                Row oldRow = indexedRow.getRow();
                RowBuilder firstNewRow = RowBuilder.create(newColumns.size());
                int firstNewRowIndex = newRows.size();
                
                newRows.add(firstNewRow);
                
                int transposedCells = 0;
                for (int c = 0; c < oldColumns.size(); c++) {
                    ColumnMetadata column = oldColumns.get(c);
                    Cell cell = oldRow.getCell(c);
                    
                    if (c < startColumnIndex) {
                        firstNewRow.withCell(c, cell);
                    } else if (c == startColumnIndex || c < startColumnIndex + columnCount) {
                        if (_combinedColumnName != null) {
                            Cell newCell;
                            if (cell == null || cell.value == null) {
                                if (_prependColumnName && !_ignoreBlankCells) {
                                    newCell = new Cell(column.getName() + _separator, null);
                                } else {
                                    continue;
                                }
                            } else if (_prependColumnName) {
                                newCell = new Cell(column.getName() + _separator + cell.value, null);
                            } else {
                                newCell = cell;
                            }
                            
                            RowBuilder rowToModify;
                            if (transposedCells == 0) {
                                rowToModify = firstNewRow;
                            } else {
                                rowToModify = RowBuilder.create(newColumns.size());
                                newRows.add(rowToModify);
                            }
                            rowToModify.withCell(startColumnIndex, newCell);
                            
                            transposedCells++;
                        } else {
                            if (_ignoreBlankCells && (cell == null || cell.value == null)) {
                                continue;
                            }
                            
                            RowBuilder rowToModify;
                            if (transposedCells == 0) {
                                rowToModify = firstNewRow;
                            } else {
                                rowToModify = RowBuilder.create(newColumns.size());
                                newRows.add(rowToModify);
                            }
                            rowToModify.withCell(startColumnIndex, new Cell(column.getName(), null));
                            rowToModify.withCell(startColumnIndex + 1, cell);
                            
                            transposedCells++;
                        }
                        
                    } else {
                        firstNewRow.withCell(
                            c - columnCount + (_combinedColumnName != null ? 1 : 2),
                            cell);
                    }
                }
                
                if (_fillDown) {
                    for (int r2 = firstNewRowIndex + 1; r2 < newRows.size(); r2++) {
                        RowBuilder newRow = newRows.get(r2);
                        for (int c = 0; c < newColumns.size(); c++) {
                            if (c < startColumnIndex ||
                                (_combinedColumnName != null ?
                                    c > startColumnIndex :
                                    c > startColumnIndex + 1)) {
                                int cellIndex = c;
                                
                                Cell cellToCopy = firstNewRow.getCell(cellIndex);
                                if (cellToCopy != null && newRow.getCell(cellIndex) == null) {
                                    newRow.withCell(cellIndex, cellToCopy);
                                }
                            }
                        }
                    }
                }
            }
            
            List<Row> rows = newRows.stream()
            		.map(rb -> rb.build(newColumns.size()))
            		.collect(Collectors.toList());
            ColumnModel newColumnModel = new ColumnModel(newColumns);
    		return projectState.getDatamodelRunner().create(newColumnModel, rows, projectState.getOverlayModels());
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
