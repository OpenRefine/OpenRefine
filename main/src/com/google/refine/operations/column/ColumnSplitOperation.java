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

package com.google.refine.operations.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.google.refine.operations.OperationDescription;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.ColumnSplitChange;
import com.google.refine.operations.EngineDependentOperation;

public class ColumnSplitOperation extends EngineDependentOperation {

    final protected String _columnName;
    final protected boolean _guessCellType;
    final protected boolean _removeOriginalColumn;
    final protected String _mode;

    final protected String _separator;
    final protected Boolean _regex;
    final protected Integer _maxColumns;

    final protected int[] _fieldLengths;

    @JsonCreator
    public static ColumnSplitOperation deserialize(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("guessCellType") boolean guessCellType,
            @JsonProperty("removeOriginalColumn") boolean removeOriginalColumn,
            @JsonProperty("mode") String mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") Boolean regex,
            @JsonProperty("maxColumns") Integer maxColumns,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        if ("separator".equals(mode)) {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    separator,
                    regex,
                    maxColumns);
        } else {
            return new ColumnSplitOperation(
                    engineConfig,
                    columnName,
                    guessCellType,
                    removeOriginalColumn,
                    fieldLengths);
        }
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            String separator,
            boolean regex,
            int maxColumns) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = "separator";
        _separator = separator;
        _regex = regex;
        _maxColumns = maxColumns;

        _fieldLengths = null;
    }

    public ColumnSplitOperation(
            EngineConfig engineConfig,
            String columnName,
            boolean guessCellType,
            boolean removeOriginalColumn,
            int[] fieldLengths) {
        super(engineConfig);

        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;

        _mode = "lengths";
        _separator = null;
        _regex = null;
        _maxColumns = null;

        _fieldLengths = fieldLengths;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("guessCellType")
    public boolean getGuessCellType() {
        return _guessCellType;
    }

    @JsonProperty("removeOriginalColumn")
    public boolean getRemoveOriginalColumn() {
        return _removeOriginalColumn;
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

    @JsonProperty("maxColumns")
    @JsonInclude(Include.NON_NULL)
    public Integer getMaxColumns() {
        return _maxColumns;
    }

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return _fieldLengths;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return ("separator".equals(_mode)) ? OperationDescription.column_split_separator_brief(_columnName)
                : OperationDescription.column_split_brief(_columnName);
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        Column column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }

        List<String> columnNames = new ArrayList<String>();
        List<Integer> rowIndices = new ArrayList<Integer>(project.rows.size());
        List<List<Serializable>> tuples = new ArrayList<List<Serializable>>(project.rows.size());

        FilteredRows filteredRows = engine.getAllFilteredRows();
        RowVisitor rowVisitor;
        if ("lengths".equals(_mode)) {
            rowVisitor = new ColumnSplitRowVisitor(column.getCellIndex(), columnNames, rowIndices, tuples) {

                @Override
                protected java.util.List<Serializable> split(String s) {
                    List<Serializable> results = new ArrayList<Serializable>(_fieldLengths.length + 1);

                    int lastIndex = 0;
                    for (int length : _fieldLengths) {
                        int from = lastIndex;
                        int to = Math.min(from + length, s.length());

                        results.add(stringToValue(s.substring(from, to)));

                        lastIndex = to;
                    }

                    return results;
                };
            };
        } else if (_regex) {
            Pattern pattern = Pattern.compile(_separator);

            rowVisitor = new ColumnSplitRowVisitor(column.getCellIndex(), columnNames, rowIndices, tuples) {

                Pattern _pattern;

                @Override
                protected java.util.List<Serializable> split(String s) {
                    return stringArrayToValueList(_pattern.split(s, _maxColumns));
                };

                public RowVisitor init(Pattern pattern) {
                    _pattern = pattern;
                    return this;
                }
            }.init(pattern);
        } else {
            rowVisitor = new ColumnSplitRowVisitor(column.getCellIndex(), columnNames, rowIndices, tuples) {

                @Override
                protected java.util.List<Serializable> split(String s) {
                    return stringArrayToValueList(
                            StringUtils.splitByWholeSeparatorPreserveAllTokens(s, _separator, _maxColumns));
                };
            };
        }

        filteredRows.accept(project, rowVisitor);

        String description = "Split " + rowIndices.size() +
                " cell(s) in column " + _columnName +
                " into several columns" +
                ("separator".equals(_mode) ? " by separator" : " by field lengths");

        Change change = new ColumnSplitChange(
                _columnName,
                columnNames,
                rowIndices,
                tuples,
                _removeOriginalColumn);

        return new HistoryEntry(
                historyEntryID, project, description, this, change);
    }

    protected class ColumnSplitRowVisitor implements RowVisitor {

        int cellIndex;
        List<String> columnNames;
        List<Integer> rowIndices;
        List<List<Serializable>> tuples;

        int columnNameIndex = 1;

        ColumnSplitRowVisitor(
                int cellIndex,
                List<String> columnNames,
                List<Integer> rowIndices,
                List<List<Serializable>> tuples) {
            this.cellIndex = cellIndex;
            this.columnNames = columnNames;
            this.rowIndices = rowIndices;
            this.tuples = tuples;
        }

        @Override
        public void start(Project project) {
            // nothing to do
        }

        @Override
        public void end(Project project) {
            // nothing to do
        }

        @Override
        public boolean visit(Project project, int rowIndex, Row row) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = value instanceof String ? ((String) value) : value.toString();

                List<Serializable> tuple = split(s);

                rowIndices.add(rowIndex);
                tuples.add(tuple);

                for (int i = columnNames.size(); i < tuple.size(); i++) {
                    while (true) {
                        String newColumnName = _columnName + " " + columnNameIndex++;
                        if (project.columnModel.getColumnByName(newColumnName) == null) {
                            columnNames.add(newColumnName);
                            break;
                        }
                    }
                }
            }
            return false;
        }

        protected List<Serializable> split(String s) {
            throw new UnsupportedOperationException();
        }

        protected Serializable stringToValue(String s) {
            return _guessCellType ? ImporterUtilities.parseCellValue(s) : s;
        }

        protected List<Serializable> stringArrayToValueList(String[] cells) {
            List<Serializable> results = new ArrayList<Serializable>(cells.length);
            for (String cell : cells) {
                results.add(stringToValue(cell));
            }

            return results;
        }
    }
}
