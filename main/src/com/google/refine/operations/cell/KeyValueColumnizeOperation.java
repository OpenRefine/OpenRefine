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

package com.google.refine.operations.cell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassRowColumnChange;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.JSONUtilities;

public class KeyValueColumnizeOperation extends AbstractOperation {
    final protected String  _keyColumnName;
    final protected String  _valueColumnName;
    final protected String  _noteColumnName;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        return new KeyValueColumnizeOperation(
            obj.getString("keyColumnName"),
            obj.getString("valueColumnName"),
            JSONUtilities.getString(obj, "noteColumnName", null)
        );
    }
    
    public KeyValueColumnizeOperation(
        String keyColumnName,
        String valueColumnName,
        String noteColumnName
    ) {
        _keyColumnName = keyColumnName;
        _valueColumnName = valueColumnName;
        _noteColumnName = noteColumnName;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {

        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(
            "Columnize by key column " +
            _keyColumnName + " and value column " + _valueColumnName +
            (_noteColumnName != null ? (" with note column " + _noteColumnName) : ""));
        writer.key("keyColumnName"); writer.value(_keyColumnName);
        writer.key("valueColumnName"); writer.value(_valueColumnName);
        writer.key("noteColumnName"); writer.value(_noteColumnName);
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Columnize by key column " +
            _keyColumnName + " and value column " + _valueColumnName +
            (_noteColumnName != null ? (" with note column " + _noteColumnName) : "");
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        int keyColumnIndex = project.columnModel.getColumnIndexByName(_keyColumnName);
        int valueColumnIndex = project.columnModel.getColumnIndexByName(_valueColumnName);
        int noteColumnIndex = _noteColumnName == null ? -1 :
            project.columnModel.getColumnIndexByName(_noteColumnName);
        Column keyColumn = project.columnModel.getColumnByName(_keyColumnName);
        Column valueColumn = project.columnModel.getColumnByName(_valueColumnName);
        Column noteColumn = _noteColumnName == null ? null :
            project.columnModel.getColumnByName(_noteColumnName);
        
        List<Column> unchangedColumns = new ArrayList<Column>();
        List<Column> oldColumns = project.columnModel.columns;
        for (int i = 0; i < oldColumns.size(); i++) {
            if (i != keyColumnIndex &&
                i != valueColumnIndex &&
                i != noteColumnIndex) {
                unchangedColumns.add(oldColumns.get(i));
            }
        }
        
        List<Column> newColumns = new ArrayList<Column>();
        List<Column> newNoteColumns = new ArrayList<Column>();
        Map<String, Column> keyValueToColumn = new HashMap<String, Column>();
        Map<String, Column> keyValueToNoteColumn = new HashMap<String, Column>();
        Map<String, Row> groupByCellValuesToRow = new HashMap<String, Row>();
        
        List<Row> newRows = new ArrayList<Row>();
        List<Row> oldRows = project.rows;
        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = oldRows.get(r);
            
            Object value = oldRow.getCellValue(valueColumn.getCellIndex());
            Object key = oldRow.getCellValue(keyColumn.getCellIndex());
            if (!ExpressionUtils.isNonBlankData(value) ||
                !ExpressionUtils.isNonBlankData(key)) {
                continue; // TODO: ignore this row entirely?
            }
            
            String keyString = key.toString();
            Column newColumn = keyValueToColumn.get(keyString);
            if (newColumn == null) {
                // Allocate new column
                newColumn = new Column(
                    project.columnModel.allocateNewCellIndex(),
                    project.columnModel.getUnduplicatedColumnName(keyString));
                keyValueToColumn.put(keyString, newColumn);
                newColumns.add(newColumn);
            }
            
            StringBuffer sb = new StringBuffer();
            for (int c = 0; c < unchangedColumns.size(); c++) {
                Column unchangedColumn = unchangedColumns.get(c);
                Object cellValue = oldRow.getCellValue(unchangedColumn.getCellIndex());
                if (c > 0) {
                    sb.append('\0');
                }
                if (cellValue != null) {
                    sb.append(cellValue.toString());
                }
            }
            String unchangedCellValues = sb.toString();
            
            Row reusableRow = groupByCellValuesToRow.get(unchangedCellValues);
            if (reusableRow == null ||
                reusableRow.getCellValue(valueColumn.getCellIndex()) != null) {
                reusableRow = new Row(newColumn.getCellIndex() + 1);
                
                for (int c = 0; c < unchangedColumns.size(); c++) {
                    Column unchangedColumn = unchangedColumns.get(c);
                    int cellIndex = unchangedColumn.getCellIndex();
                    
                    reusableRow.setCell(cellIndex, oldRow.getCell(cellIndex));
                }
                
                groupByCellValuesToRow.put(unchangedCellValues, reusableRow);
                newRows.add(reusableRow);
            }
            
            reusableRow.setCell(
                newColumn.getCellIndex(),
                oldRow.getCell(valueColumn.getCellIndex()));
            
            if (noteColumn != null) {
                Object noteValue = oldRow.getCellValue(noteColumn.getCellIndex());
                if (ExpressionUtils.isNonBlankData(noteValue)) {
                    Column newNoteColumn = keyValueToNoteColumn.get(keyString);
                    if (newNoteColumn == null) {
                        // Allocate new column
                        newNoteColumn = new Column(
                            project.columnModel.allocateNewCellIndex(),
                            project.columnModel.getUnduplicatedColumnName(
                                noteColumn.getName() + " : " + keyString));
                        keyValueToNoteColumn.put(keyString, newNoteColumn);
                        newNoteColumns.add(newNoteColumn);
                    }
                    
                    int newNoteCellIndex = newNoteColumn.getCellIndex();
                    Object existingNewNoteValue = reusableRow.getCellValue(newNoteCellIndex);
                    if (ExpressionUtils.isNonBlankData(existingNewNoteValue)) {
                        Cell concatenatedNoteCell = new Cell(
                            existingNewNoteValue.toString() + ";" + noteValue.toString(), null);
                        reusableRow.setCell(newNoteCellIndex, concatenatedNoteCell);
                    } else {
                        reusableRow.setCell(newNoteCellIndex, oldRow.getCell(noteColumn.getCellIndex()));
                    }
                }
            }
        }
        
        unchangedColumns.addAll(newColumns);
        unchangedColumns.addAll(newNoteColumns);
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            getBriefDescription(null), 
            this, 
            new MassRowColumnChange(unchangedColumns, newRows)
        );
    }
}
