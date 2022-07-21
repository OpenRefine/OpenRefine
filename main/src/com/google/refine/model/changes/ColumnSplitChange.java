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

package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnGroup;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class ColumnSplitChange implements Change {

    final protected String _columnName;

    final protected List<String> _columnNames;
    final protected List<Integer> _rowIndices;
    final protected List<List<Serializable>> _tuples;

    final protected boolean _removeOriginalColumn;

    protected Column _column;
    protected int _columnIndex;

    protected int _firstNewCellIndex = -1;
    protected List<Row> _oldRows;
    protected List<Row> _newRows;

    protected List<ColumnGroup> _oldColumnGroups;

    public ColumnSplitChange(
            String columnName,
            List<String> columnNames,
            List<Integer> rowIndices,
            List<List<Serializable>> tuples,
            boolean removeOriginalColumn) {
        _columnName = columnName;

        _columnNames = columnNames;
        _rowIndices = rowIndices;
        _tuples = tuples;

        _removeOriginalColumn = removeOriginalColumn;
    }

    protected ColumnSplitChange(
            String columnName,
            List<String> columnNames,
            List<Integer> rowIndices,
            List<List<Serializable>> tuples,
            boolean removeOriginalColumn,

            Column column,
            int columnIndex,

            int firstNewCellIndex,
            List<Row> oldRows,
            List<Row> newRows) {
        _columnName = columnName;

        _columnNames = columnNames;
        _rowIndices = rowIndices;
        _tuples = tuples;

        _removeOriginalColumn = removeOriginalColumn;

        _column = column;
        _columnIndex = columnIndex;

        _firstNewCellIndex = firstNewCellIndex;
        _oldRows = oldRows;
        _newRows = newRows;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_firstNewCellIndex < 0) {
                _firstNewCellIndex = project.columnModel.allocateNewCellIndex();
                for (int i = 1; i < _columnNames.size(); i++) {
                    project.columnModel.allocateNewCellIndex();
                }

                ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _columnName);
                _column = project.columnModel.getColumnByName(_columnName);
                _columnIndex = project.columnModel.getColumnIndexByName(_columnName);

                _oldRows = new ArrayList<Row>(_rowIndices.size());
                _newRows = new ArrayList<Row>(_rowIndices.size());

                int cellIndex = _column.getCellIndex();

                for (int i = 0; i < _rowIndices.size(); i++) {
                    int r = _rowIndices.get(i);
                    List<Serializable> tuple = _tuples.get(i);

                    Row oldRow = project.rows.get(r);
                    Row newRow = oldRow.dup();

                    _oldRows.add(oldRow);
                    _newRows.add(newRow);

                    for (int c = 0; c < tuple.size(); c++) {
                        Serializable value = tuple.get(c);
                        if (value != null) {
                            newRow.setCell(_firstNewCellIndex + c, new Cell(value, null));
                        }
                    }

                    if (_removeOriginalColumn) {
                        newRow.setCell(cellIndex, null);
                    }
                }
            }

            int columnGroupCount = project.columnModel.columnGroups.size();
            int columnCountChange = _columnNames.size() - (_removeOriginalColumn ? 1 : 0);
            _oldColumnGroups = new ArrayList<ColumnGroup>(columnGroupCount);
            for (int i = columnGroupCount - 1; i >= 0; i--) {
                ColumnGroup columnGroup = project.columnModel.columnGroups.get(i);

                _oldColumnGroups.add(columnGroup);

                if (columnGroup.startColumnIndex <= _columnIndex) {
                    if (columnGroup.startColumnIndex + columnGroup.columnSpan > _columnIndex) {
                        // the column being split is in the middle of the group

                        if (columnGroup.keyColumnIndex == _columnIndex) {
                            if (_removeOriginalColumn) {
                                // the key column is being split and removed
                                project.columnModel.columnGroups.remove(i);
                            } else {
                                project.columnModel.columnGroups.set(i, new ColumnGroup(
                                        columnGroup.startColumnIndex,
                                        columnGroup.columnSpan + columnCountChange,
                                        columnGroup.keyColumnIndex));
                            }
                        } else {
                            project.columnModel.columnGroups.set(i, new ColumnGroup(
                                    columnGroup.startColumnIndex,
                                    columnGroup.columnSpan + columnCountChange,
                                    columnGroup.keyColumnIndex < _columnIndex ? columnGroup.keyColumnIndex
                                            : (columnGroup.keyColumnIndex + columnCountChange)));
                        }
                    }
                } else {
                    // the new column precedes this whole group
                    project.columnModel.columnGroups.set(i, new ColumnGroup(
                            columnGroup.startColumnIndex + columnCountChange,
                            columnGroup.columnSpan,
                            columnGroup.keyColumnIndex + columnCountChange));
                }
            }

            for (int i = 0; i < _rowIndices.size(); i++) {
                int r = _rowIndices.get(i);
                Row newRow = _newRows.get(i);

                project.rows.set(r, newRow);
            }

            for (int i = 0; i < _columnNames.size(); i++) {
                String name = _columnNames.get(i);
                int cellIndex = _firstNewCellIndex + i;

                Column column = new Column(cellIndex, name);

                project.columnModel.columns.add(_columnIndex + 1 + i, column);
            }

            if (_removeOriginalColumn) {
                project.columnModel.columns.remove(_columnIndex);
            }

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            for (int i = 0; i < _rowIndices.size(); i++) {
                int r = _rowIndices.get(i);
                Row oldRow = _oldRows.get(i);

                project.rows.set(r, oldRow);
            }

            if (_removeOriginalColumn) {
                project.columnModel.columns.add(_columnIndex, _column);
            }

            for (int i = 0; i < _columnNames.size(); i++) {
                project.columnModel.columns.remove(_columnIndex + 1);
                ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProjectColumn(project.id, _columnNames.get(i));
            }

            project.columnModel.columnGroups.clear();
            project.columnModel.columnGroups.addAll(_oldColumnGroups);

            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("columnName=");
        writer.write(_columnName);
        writer.write('\n');

        writer.write("columnNameCount=");
        writer.write(Integer.toString(_columnNames.size()));
        writer.write('\n');
        for (String name : _columnNames) {
            writer.write(name);
            writer.write('\n');
        }
        writer.write("rowIndexCount=");
        writer.write(Integer.toString(_rowIndices.size()));
        writer.write('\n');
        for (Integer rowIndex : _rowIndices) {
            writer.write(rowIndex.toString());
            writer.write('\n');
        }
        writer.write("tupleCount=");
        writer.write(Integer.toString(_tuples.size()));
        writer.write('\n');
        for (List<Serializable> tuple : _tuples) {
            writer.write(Integer.toString(tuple.size()));
            writer.write('\n');

            for (Serializable value : tuple) {
                if (value == null) {
                    writer.write("null");
                } else if (value instanceof String) {
                    writer.write(ParsingUtilities.mapper.writeValueAsString((String) value));
                } else {
                    writer.write(value.toString());
                }
                writer.write('\n');
            }
        }
        writer.write("removeOriginalColumn=");
        writer.write(Boolean.toString(_removeOriginalColumn));
        writer.write('\n');

        writer.write("column=");
        _column.save(writer);
        writer.write('\n');
        writer.write("columnIndex=");
        writer.write(Integer.toString(_columnIndex));
        writer.write('\n');

        writer.write("firstNewCellIndex=");
        writer.write(Integer.toString(_firstNewCellIndex));
        writer.write('\n');

        writer.write("newRowCount=");
        writer.write(Integer.toString(_newRows.size()));
        writer.write('\n');
        for (Row row : _newRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("oldRowCount=");
        writer.write(Integer.toString(_oldRows.size()));
        writer.write('\n');
        for (Row row : _oldRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        ColumnChange.writeOldColumnGroups(writer, options, _oldColumnGroups);
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String columnName = null;
        List<String> columnNames = null;
        List<Integer> rowIndices = null;
        List<List<Serializable>> tuples = null;
        boolean removeOriginalColumn = false;

        Column column = null;
        int columnIndex = -1;

        int firstNewCellIndex = -1;
        List<Row> oldRows = null;
        List<Row> newRows = null;

        List<ColumnGroup> oldColumnGroups = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);

            if ("columnName".equals(field)) {
                columnName = value;
            } else if ("columnNameCount".equals(field)) {
                int count = Integer.parseInt(value);

                columnNames = new ArrayList<String>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        columnNames.add(line);
                    }
                }
            } else if ("rowIndexCount".equals(field)) {
                int count = Integer.parseInt(value);

                rowIndices = new ArrayList<Integer>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        rowIndices.add(Integer.parseInt(line));
                    }
                }
            } else if ("tupleCount".equals(field)) {
                int count = Integer.parseInt(value);

                tuples = new ArrayList<List<Serializable>>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();

                    if (line == null) {
                        continue;
                    }

                    int valueCount = Integer.parseInt(line);

                    List<Serializable> tuple = new ArrayList<Serializable>(valueCount);
                    for (int r = 0; r < valueCount; r++) {
                        line = reader.readLine();

                        tuple.add((Serializable) ParsingUtilities.mapper.readValue(line, Object.class));
                    }

                    tuples.add(tuple);
                }
            } else if ("removeOriginalColumn".equals(field)) {
                removeOriginalColumn = Boolean.parseBoolean(value);

            } else if ("column".equals(field)) {
                column = Column.load(value);
            } else if ("columnIndex".equals(field)) {
                columnIndex = Integer.parseInt(value);
            } else if ("firstNewCellIndex".equals(field)) {
                firstNewCellIndex = Integer.parseInt(value);
            } else if ("oldRowCount".equals(field)) {
                int count = Integer.parseInt(value);

                oldRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldRows.add(Row.load(line, pool));
                    }
                }
            } else if ("newRowCount".equals(field)) {
                int count = Integer.parseInt(value);

                newRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newRows.add(Row.load(line, pool));
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));

                oldColumnGroups = ColumnChange.readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }

        ColumnSplitChange change = new ColumnSplitChange(
                columnName,
                columnNames,
                rowIndices,
                tuples,
                removeOriginalColumn,

                column,
                columnIndex,

                firstNewCellIndex,
                oldRows,
                newRows);
        change._oldColumnGroups = oldColumnGroups != null ? oldColumnGroups : new LinkedList<ColumnGroup>();

        return change;
    }
}
