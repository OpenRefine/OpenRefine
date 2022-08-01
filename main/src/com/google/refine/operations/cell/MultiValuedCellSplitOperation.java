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
import java.util.regex.Pattern;

import com.google.refine.operations.OperationDescription;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassRowChange;

public class MultiValuedCellSplitOperation extends AbstractOperation {

    final protected String _columnName;
    final protected String _keyColumnName;
    final protected String _mode;
    final protected String _separator;
    final protected Boolean _regex;

    final protected int[] _fieldLengths;

    @JsonCreator
    public static MultiValuedCellSplitOperation deserialize(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("mode") String mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") boolean regex,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if ("separator".equals(mode)) {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    separator,
                    regex);
        } else {
            return new MultiValuedCellSplitOperation(
                    columnName,
                    keyColumnName,
                    fieldLengths);
        }
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            String separator,
            boolean regex) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;
        _separator = separator;
        _mode = "separator";
        _regex = regex;

        _fieldLengths = null;
    }

    public MultiValuedCellSplitOperation(
            String columnName,
            String keyColumnName,
            int[] fieldLengths) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;

        _mode = "lengths";
        _separator = null;
        _regex = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
    }

    @JsonProperty("mode")
    public String getMode() {
        return _mode;
    }

    @JsonProperty("separator")
    @JsonInclude(Include.NON_NULL)
    public String getSeparator() {
        return _separator;
    }

    @JsonProperty("regex")
    @JsonInclude(Include.NON_NULL)
    public Boolean getRegex() {
        return _regex;
    }

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.cell_multivalued_cell_split_brief(_columnName);
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }
        int cellIndex = column.getCellIndex();

        Column keyColumn = project.columnModel.getColumnByName(_keyColumnName);
        if (keyColumn == null) {
            throw new Exception("No key column named " + _keyColumnName);
        }
        int keyCellIndex = keyColumn.getCellIndex();

        List<Row> newRows = new ArrayList<Row>();

        int oldRowCount = project.rows.size();
        for (int r = 0; r < oldRowCount; r++) {
            Row oldRow = project.rows.get(r);
            if (oldRow.isCellBlank(cellIndex)) {
                newRows.add(oldRow.dup());
                continue;
            }

            Object value = oldRow.getCellValue(cellIndex);
            String s = value instanceof String ? ((String) value) : value.toString();
            String[] values = null;
            if ("lengths".equals(_mode)) {
                if (_fieldLengths.length > 0 && _fieldLengths[0] > 0) {
                    values = new String[_fieldLengths.length];

                    int lastIndex = 0;

                    for (int i = 0; i < _fieldLengths.length; i++) {
                        int thisIndex = lastIndex;

                        Object o = _fieldLengths[i];
                        if (o instanceof Number) {
                            thisIndex = Math.min(s.length(), lastIndex + Math.max(0, ((Number) o).intValue()));
                        }

                        values[i] = s.substring(lastIndex, thisIndex);
                        lastIndex = thisIndex;
                    }
                }
            } else if (_regex) {
                Pattern pattern = Pattern.compile(_separator, Pattern.UNICODE_CHARACTER_CLASS);
                values = pattern.split(s);
            } else {
                values = StringUtils.splitByWholeSeparatorPreserveAllTokens(s, _separator);
            }

            if (values.length < 2) {
                newRows.add(oldRow.dup());
                continue;
            }

            // First value goes into the same row
            {
                Row firstNewRow = oldRow.dup();
                firstNewRow.setCell(cellIndex, new Cell(values[0], null));

                newRows.add(firstNewRow);
            }

            int r2 = r + 1;
            for (int v = 1; v < values.length; v++) {
                Cell newCell = new Cell(values[v], null);

                if (r2 < project.rows.size()) {
                    Row oldRow2 = project.rows.get(r2);
                    if (oldRow2.isCellBlank(cellIndex) &&
                            oldRow2.isCellBlank(keyCellIndex)) {

                        Row newRow = oldRow2.dup();
                        newRow.setCell(cellIndex, newCell);

                        newRows.add(newRow);
                        r2++;

                        continue;
                    }
                }

                Row newRow = new Row(cellIndex + 1);
                newRow.setCell(cellIndex, newCell);

                newRows.add(newRow);
            }

            r = r2 - 1; // r will be incremented by the for loop anyway
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                getBriefDescription(null),
                this,
                new MassRowChange(newRows));
    }
}
