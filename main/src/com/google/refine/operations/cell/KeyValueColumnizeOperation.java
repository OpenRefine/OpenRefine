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
        Row reusableRow = null;
        List<Row> currentRows = new ArrayList<Row>();
        String recordKey = null; // key which indicates the start of a record
        if (unchangedColumns.isEmpty()) {
            reusableRow = new Row(1);
            newRows.add(reusableRow);
            currentRows.clear();
            currentRows.add(reusableRow);
        }

        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = oldRows.get(r);
            
            Object key = oldRow.getCellValue(keyColumn.getCellIndex());
            if (!ExpressionUtils.isNonBlankData(key)) {
                if (unchangedColumns.isEmpty()) {
                    // For degenerate 2 column case (plus optional note column), 
                    // start a new row when we hit a blank line
                    reusableRow = new Row(newColumns.size());
                    newRows.add(reusableRow);
                    currentRows.clear();
                    currentRows.add(reusableRow);
                } else {
                    // Copy rows with no key
                    newRows.add(buildNewRow(unchangedColumns, oldRow, unchangedColumns.size()));
                }
                continue; 
            }
            
            String keyString = key.toString();
            // Start a new row on our beginning of record key
            // TODO: Add support for processing in record mode instead of just by rows
            if (keyString.equals(recordKey) || recordKey == null) {
                reusableRow = new Row(newColumns.size());
                newRows.add(reusableRow);
                currentRows.clear();
                currentRows.add(reusableRow);
            }
            Column newColumn = keyValueToColumn.get(keyString);
            if (newColumn == null) {
                // Allocate new column
                newColumn = new Column(
                    project.columnModel.allocateNewCellIndex(),
                    project.columnModel.getUnduplicatedColumnName(keyString));
                keyValueToColumn.put(keyString, newColumn);
                newColumns.add(newColumn);

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

                reusableRow = groupByCellValuesToRow.get(unchangedCellValues);
                if (reusableRow == null ||
                        reusableRow.getCellValue(valueColumn.getCellIndex()) != null) {
                    reusableRow = buildNewRow(unchangedColumns, oldRow, newColumn.getCellIndex() + 1);
                    groupByCellValuesToRow.put(unchangedCellValues, reusableRow);
                    newRows.add(reusableRow);
                }
            }
            
            Cell cell = oldRow.getCell(valueColumn.getCellIndex());
            if (unchangedColumns.size() == 0) {
                int index = newColumn.getCellIndex();
                Row row = getAvailableRow(currentRows, newRows, index);
                row.setCell(index, cell);
            } else {
                // TODO: support repeating keys in this mode too
                reusableRow.setCell(newColumn.getCellIndex(), cell);
            }
            
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
        
        List<Column> allColumns = new ArrayList<Column>(unchangedColumns);
        allColumns.addAll(newColumns);
        allColumns.addAll(newNoteColumns);
        
        // clean up the empty rows 
        for (int i = newRows.size() - 1;i>=0;i--) {
            if (newRows.get(i).isEmpty())
                newRows.remove(i);
        }
        
        return new HistoryEntry(
            historyEntryID,
            project, 
            getBriefDescription(null), 
            this, 
            new MassRowColumnChange(allColumns, newRows)
        );
    }

    private Row getAvailableRow(List<Row> currentRows, List<Row> newRows, int index) {
        for (Row row : currentRows) {
            if (row.getCell(index) == null) {
                return row;
            }
        }
        // If we couldn't find a row with an empty spot, we'll need a new row
        Row row = new Row(index);
        newRows.add(row);
        currentRows.add(row);
        return row;
    }

    private Row buildNewRow(List<Column> unchangedColumns, Row oldRow, int size) {
        Row reusableRow = new Row(size);
        for (int c = 0; c < unchangedColumns.size(); c++) {
            Column unchangedColumn = unchangedColumns.get(c);
            int cellIndex = unchangedColumn.getCellIndex();
            reusableRow.setCell(cellIndex, oldRow.getCell(cellIndex));
        }
        return reusableRow;
    }
}
