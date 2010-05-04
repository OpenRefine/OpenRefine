package com.metaweb.gridworks.operations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.importers.ImporterUtilities;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.ColumnSplitChange;
import com.metaweb.gridworks.util.JSONUtilities;

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

    protected String getBriefDescription(Project project) {
        return "Split column " + _columnName + 
            ("separator".equals(_mode) ? " by separator" : " by field lengths");
    }

    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);
        
        Column column = project.columnModel.getColumnByName(_columnName);
        if (column == null) {
            throw new Exception("No column named " + _columnName);
        }
        
        List<String> columnNames = new ArrayList<String>();
        List<Integer> rowIndices = new ArrayList<Integer>(project.rows.size());
        List<List<Serializable>> tuples = new ArrayList<List<Serializable>>(project.rows.size());
        
        FilteredRows filteredRows = engine.getAllFilteredRows(false);
        RowVisitor rowVisitor;
        if ("lengths".equals(_mode)) {
            rowVisitor = new ColumnSplitRowVisitor(project, column.getCellIndex(), columnNames, rowIndices, tuples) {
                protected java.util.List<Serializable> split(String s) {
                    List<Serializable> results = new ArrayList<Serializable>(_fieldLengths.length + 1);
                    
                    int lastIndex = 0;
                    for (int i = 0; i < _fieldLengths.length; i++) {
                        int from = lastIndex;
                        int length = _fieldLengths[i];
                        int to = Math.min(from + length, s.length());
                        
                        results.add(stringToValue(s.substring(from, to)));
                        
                        lastIndex = to;
                    }
                    
                    return results;
                };
            };
        } else if (_regex) {
            Pattern pattern = Pattern.compile(_separator);
            
            rowVisitor = new ColumnSplitRowVisitor(project, column.getCellIndex(), columnNames, rowIndices, tuples) {
                Pattern _pattern;
                
                protected java.util.List<Serializable> split(String s) {
                    return stringArrayToValueList(_pattern.split(s, _maxColumns));
                };
                
                public RowVisitor init(Pattern pattern) {
                    _pattern = pattern;
                    return this;
                }
            }.init(pattern);
        } else {
            rowVisitor = new ColumnSplitRowVisitor(project, column.getCellIndex(), columnNames, rowIndices, tuples) {
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
        Project project;
        int cellIndex;
        List<String> columnNames;
        List<Integer> rowIndices;
        List<List<Serializable>> tuples;
        
        int columnNameIndex = 1;
        
        ColumnSplitRowVisitor(
            Project project,
            int cellIndex,
            List<String> columnNames,
            List<Integer> rowIndices,
            List<List<Serializable>> tuples
        ) {
            this.project = project;
            this.cellIndex = cellIndex;
            this.columnNames = columnNames;
            this.rowIndices = rowIndices;
            this.tuples = tuples;
        }
        
        public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
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
            throw new NotImplementedException();
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
