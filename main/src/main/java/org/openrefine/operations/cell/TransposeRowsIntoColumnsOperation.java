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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransposeRowsIntoColumnsOperation implements Operation {
    final protected String  _columnName;
    final protected int     _rowCount;

    @JsonCreator
    public TransposeRowsIntoColumnsOperation(
        @JsonProperty("columnName")
        String  columnName,
        @JsonProperty("rowCount")
        int     rowCount
    ) {
        _columnName = columnName;
        _rowCount = rowCount;
    }
    
    @JsonProperty("rowCount")
    public int getRowCount() {
        return _rowCount;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
	public String getDescription() {
        return "Transpose every " + _rowCount + " cells in column " + _columnName + " into separate columns";
    }

    @Override
    public Change createChange() {
    	return new TransposeRowsIntoColumnsChange();
    }
    
    public class TransposeRowsIntoColumnsChange implements Change {

    	@Override
    	public GridState apply(GridState projectState, ChangeContext context) throws DoesNotApplyException {
            ColumnModel columnModel = projectState.getColumnModel();
            ColumnModel newColumns = new ColumnModel(Collections.emptyList());
            List<ColumnMetadata> oldColumns = columnModel.getColumns();
            
            int columnIndex = columnModel.getColumnIndexByName(_columnName);
            if (columnIndex == -1) {
            	throw new DoesNotApplyException(String.format("Column %s could not be found", _columnName));
            }
            int columnCount = oldColumns.size();
            
            for (int i = 0; i < columnCount; i++) {           
                if (i == columnIndex) {
                    int newIndex = 1;
                    for (int n = 0; n < _rowCount; n++) {
                        String columnName = _columnName + " " + newIndex++;
                        while (columnModel.getColumnByName(columnName) != null) {
                            columnName = _columnName + " " + newIndex++;
                        }
                        newColumns = newColumns.appendUnduplicatedColumn(new ColumnMetadata(columnName));
                    }
                } else {
                	newColumns = newColumns.appendUnduplicatedColumn(oldColumns.get(i));
                }
            }
            
            int nbNewColumns = newColumns.getColumns().size();
            RowBuilder firstNewRow = null;
            List<RowBuilder> newRows = new ArrayList<>();
            for (IndexedRow indexedRow : projectState.iterateRows(RowFilter.ANY_ROW, SortingConfig.NO_SORTING)) {
            	long r = indexedRow.getIndex();
            	int r2 = (int)(r % (long)_rowCount);
            	
            	RowBuilder newRow = RowBuilder.create(nbNewColumns);
            	newRows.add(newRow);
            	if (r2 == 0) {
            		firstNewRow = newRow;
            	}
                
                Row oldRow = indexedRow.getRow();
                
                for (int c = 0; c < oldColumns.size(); c++) {
                    Cell cell = oldRow.getCell(c);
                    
                    if (cell != null && cell.value != null) {
                        if (c == columnIndex) {
                            firstNewRow.withCell(columnIndex + r2, cell);
                        } else if (c < columnIndex) {
                            newRow.withCell(c, cell);
                        } else {
                            newRow.withCell(c + _rowCount - 1, cell);
                        }
                    }
                }
            }
            
            List<Row> rows = newRows.stream()
            		.map(rb -> rb.build(nbNewColumns))
            		.filter(row -> !row.isEmpty())
            		.collect(Collectors.toList());
    		
            return projectState.getDatamodelRunner().create(newColumns, rows, projectState.getOverlayModels());
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
