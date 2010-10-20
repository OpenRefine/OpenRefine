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

package com.google.refine.operations.cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassRowColumnChange;
import com.google.refine.operations.OperationRegistry;

public class TransposeColumnsIntoRowsOperation extends AbstractOperation {
    final protected String  _startColumnName;
    final protected int     _columnCount;
    final protected String  _combinedColumnName;
    final protected boolean _prependColumnName;
    final protected String  _separator;
    final protected boolean _ignoreBlankCells;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new TransposeColumnsIntoRowsOperation(
            obj.getString("startColumnName"),
            obj.getInt("columnCount"),
            obj.getString("combinedColumnName"),
            obj.getBoolean("prependColumnName"),
            obj.getString("separator"),
            obj.getBoolean("ignoreBlankCells")
        );
    }
    
    public TransposeColumnsIntoRowsOperation(
        String  startColumnName,
        int     columnCount,
        String  combinedColumnName,
        boolean prependColumnName,
        String  separator,
        boolean ignoreBlankCells
    ) {
        _startColumnName = startColumnName;
        _columnCount = columnCount;
        _combinedColumnName = combinedColumnName;
        _prependColumnName = prependColumnName;
        _separator = separator;
        _ignoreBlankCells = ignoreBlankCells;
    }

   public void write(JSONWriter writer, Properties options)
           throws JSONException {
       
       writer.object();
       writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
       writer.key("description"); writer.value("Transpose cells in " + _columnCount + " column(s) starting with " + _startColumnName + " into rows");
       writer.key("startColumnName"); writer.value(_startColumnName);
       writer.key("columnCount"); writer.value(_columnCount);
       writer.key("combinedColumnName"); writer.value(_combinedColumnName);
       writer.key("prependColumnName"); writer.value(_prependColumnName);
       writer.key("separator"); writer.value(_separator);
       writer.key("ignoreBlankCells"); writer.value(_ignoreBlankCells);
       writer.endObject();
    }

    protected String getBriefDescription(Project project) {
        return "Transpose cells in " + _columnCount + " column(s) starting with " + _startColumnName + " into rows";
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        if (_combinedColumnName != null && 
            !_combinedColumnName.isEmpty() &&
            project.columnModel.getColumnByName(_combinedColumnName) != null) {
            throw new Exception("Another column already named " + _combinedColumnName);
        }

        List<Column> newColumns = new ArrayList<Column>();
        List<Column> oldColumns = project.columnModel.columns;
        
        int columnsLeftToTranspose = _columnCount;
        int startColumnIndex = oldColumns.size();
        for (int c = 0; c < oldColumns.size(); c++) {
            Column column = oldColumns.get(c);
            if (columnsLeftToTranspose == 0) {
                // This column is beyond the columns to transpose
                
                Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                newColumn.setName(column.getName());
                
                newColumns.add(newColumn);
            } else if (columnsLeftToTranspose < _columnCount) {
                // This column is a column to transpose, but not the first
                // nothing to do
                
                columnsLeftToTranspose--;
            } else if (_startColumnName.equals(column.getName())) {
                // This is the first column to transpose
                
                startColumnIndex = c;
                
                String columnName = _combinedColumnName != null && _combinedColumnName.length() > 0 ? _combinedColumnName : column.getName();
                Column newColumn = new Column(newColumns.size(), columnName);
                
                newColumns.add(newColumn);
                
                columnsLeftToTranspose--;
            } else {
                // This column is before all columns to transpose
                
                Column newColumn = new Column(newColumns.size(), column.getOriginalHeaderLabel());
                newColumn.setName(column.getName());
                
                newColumns.add(newColumn);
            }
        }
        
        
        List<Row> oldRows = project.rows;
        List<Row> newRows = new ArrayList<Row>(oldRows.size() * _columnCount);
        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = project.rows.get(r);
            Row firstNewRow = new Row(newColumns.size());
            
            newRows.add(firstNewRow);
            
            int transposedCells = 0;
            for (int c = 0; c < oldColumns.size(); c++) {
                Column column = oldColumns.get(c);
                Cell cell = oldRow.getCell(column.getCellIndex());
                
                if (c < startColumnIndex) {
                    firstNewRow.setCell(c, cell);
                } else if (c == startColumnIndex || c < startColumnIndex + _columnCount) {
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
                    
                    if (transposedCells == 0) {
                        firstNewRow.setCell(startColumnIndex, newCell);
                    } else {
                        Row newRow = new Row(newColumns.size());
                        
                        newRow.setCell(startColumnIndex, newCell);
                        newRows.add(newRow);
                    }
                    
                    transposedCells++;
                } else {
                    firstNewRow.setCell(c - _columnCount + 1, cell);
                }
            }
        }
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            getBriefDescription(null), 
            this, 
            new MassRowColumnChange(newColumns, newRows)
        );
    }
}
