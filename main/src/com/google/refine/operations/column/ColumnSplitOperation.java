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
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.ColumnSplitChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.util.JSONUtilities;

public class ColumnSplitOperation extends EngineDependentOperation {
    final protected String     _columnName;
    final protected boolean    _guessCellType;
    final protected boolean    _removeOriginalColumn;
    final protected String     _mode;
    
    final protected String     _separator;
    final protected boolean    _regex;
    final protected int        _maxColumns;
    
    final protected int[]      _fieldLengths;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        String mode = obj.getString("mode");
        
        if ("separator".equals(mode)) {
            return new ColumnSplitOperation(
                engineConfig,
                obj.getString("columnName"),
                obj.getBoolean("guessCellType"),
                obj.getBoolean("removeOriginalColumn"),
                obj.getString("separator"),
                obj.getBoolean("regex"),
                obj.getInt("maxColumns")
            );
        } else {
            return new ColumnSplitOperation(
                engineConfig,
                obj.getString("columnName"),
                obj.getBoolean("guessCellType"),
                obj.getBoolean("removeOriginalColumn"),
                JSONUtilities.getIntArray(obj, "fieldLengths")
            );
        }
    }
    
    public ColumnSplitOperation(
        JSONObject     engineConfig,
        String         columnName,
        boolean        guessCellType,
        boolean        removeOriginalColumn,
        String         separator,
        boolean        regex,
        int            maxColumns
    ) {
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
        JSONObject     engineConfig,
        String         columnName,
        boolean        guessCellType,
        boolean        removeOriginalColumn,
        int[]          fieldLengths
    ) {
        super(engineConfig);
        
        _columnName = columnName;
        _guessCellType = guessCellType;
        _removeOriginalColumn = removeOriginalColumn;
        
        _mode = "lengths";
        _separator = null;
        _regex = false;
        _maxColumns = -1;
        
        _fieldLengths = fieldLengths;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnName"); writer.value(_columnName);
        writer.key("guessCellType"); writer.value(_guessCellType);
        writer.key("removeOriginalColumn"); writer.value(_removeOriginalColumn);
        writer.key("mode"); writer.value(_mode);
        if ("separator".equals(_mode)) {
            writer.key("separator"); writer.value(_separator);
            writer.key("regex"); writer.value(_regex);
            writer.key("maxColumns"); writer.value(_maxColumns);
        } else {
            writer.key("fieldLengths"); writer.array();
            for (int l : _fieldLengths) {
                writer.value(l);
            }
            writer.endArray();
        }
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Split column " + _columnName + 
            ("separator".equals(_mode) ? " by separator" : " by field lengths");
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
        
        String description = 
            "Split " + rowIndices.size() + 
            " cell(s) in column " + _columnName + 
            " into several columns" + 
                ("separator".equals(_mode) ? " by separator" : " by field lengths");

        Change change = new ColumnSplitChange(
            _columnName,
            columnNames,
            rowIndices,
            tuples,
            _removeOriginalColumn
        );
        
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
            List<List<Serializable>> tuples
        ) {
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
