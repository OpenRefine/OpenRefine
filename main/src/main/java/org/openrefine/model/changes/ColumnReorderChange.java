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

package org.openrefine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class ColumnReorderChange extends ColumnChange {

    final protected List<String> _columnNames;
    protected List<ColumnMetadata> _oldColumns;
    protected List<ColumnMetadata> _newColumns;
    protected List<ColumnMetadata> _removedColumns;
    protected CellAtRowCellIndex[] _oldCells;

    public ColumnReorderChange(List<String> columnNames) {
        _columnNames = columnNames;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_newColumns == null) {
                _newColumns = new ArrayList<ColumnMetadata>();
                _oldColumns = new ArrayList<ColumnMetadata>(project.columnModel.getColumns());

                for (String n : _columnNames) {
                    ColumnMetadata column = project.columnModel.getColumnByName(n);
                    if (column != null) {
                        _newColumns.add(column);
                    }
                }
            }

            if (_removedColumns == null) {
                _removedColumns = new ArrayList<ColumnMetadata>();
                for (String n : project.columnModel.getColumnNames()) {
                    ColumnMetadata oldColumn = project.columnModel.getColumnByName(n);
                    if (!_newColumns.contains(oldColumn)) {
                        _removedColumns.add(oldColumn);
                    }
                }
            }

            if (_oldCells == null) {
                _oldCells = new CellAtRowCellIndex[project.rows.size() * _removedColumns.size()];

                int count = 0;
                for (int i = 0; i < project.rows.size(); i++) {
                    for (int j = 0; j < _removedColumns.size(); j++) {
                        int cellIndex = _removedColumns.get(j).getCellIndex();
                        Row row = project.rows.get(i);

                        Cell oldCell = null;
                        if (cellIndex < row.cells.size()) {
                            oldCell = row.cells.get(cellIndex);
                        }
                        _oldCells[count++] = new CellAtRowCellIndex(i, cellIndex, oldCell);
                    }
                }
            }

            // Clear cells on removed columns.
            for (int i = 0; i < project.rows.size(); i++) {
                for (int j = 0; j < _removedColumns.size(); j++) {
                    int cellIndex = _removedColumns.get(j).getCellIndex();
                    Row row = project.rows.get(i);
                    row.setCell(cellIndex, null);
                }
            }

            project.columnModel.getColumns().clear();
            project.columnModel.getColumns().addAll(_newColumns);

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.getColumns().clear();
            project.columnModel.getColumns().addAll(_oldColumns);

            for (int i = 0; i < _oldCells.length; i++) {
                Row row = project.rows.get(_oldCells[i].row);
                row.setCell(_oldCells[i].cellIndex, _oldCells[i].cell);
            }

            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("columnNameCount=");
        writer.write(Integer.toString(_columnNames.size()));
        writer.write('\n');
        for (String n : _columnNames) {
            writer.write(n);
            writer.write('\n');
        }
        writer.write("oldColumnCount=");
        writer.write(Integer.toString(_oldColumns.size()));
        writer.write('\n');
        for (ColumnMetadata c : _oldColumns) {
            c.save(writer);
            writer.write('\n');
        }
        writer.write("newColumnCount=");
        writer.write(Integer.toString(_newColumns.size()));
        writer.write('\n');
        for (ColumnMetadata c : _newColumns) {
            c.save(writer);
            writer.write('\n');
        }
        writer.write("removedColumnCount=");
        writer.write(Integer.toString(_removedColumns.size()));
        writer.write('\n');
        for (ColumnMetadata c : _removedColumns) {
            c.save(writer);
            writer.write('\n');
        }
        writer.write("oldCellCount=");
        writer.write(Integer.toString(_oldCells.length));
        writer.write('\n');
        for (CellAtRowCellIndex c : _oldCells) {
            c.save(writer, options);
            writer.write('\n');
        }

        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader) throws Exception {
        List<String> columnNames = new ArrayList<String>();
        List<ColumnMetadata> oldColumns = new ArrayList<ColumnMetadata>();
        List<ColumnMetadata> newColumns = new ArrayList<ColumnMetadata>();
        List<ColumnMetadata> removedColumns = new ArrayList<ColumnMetadata>();
        CellAtRowCellIndex[] oldCells = new CellAtRowCellIndex[0];

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if ("columnNameCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        columnNames.add(line);
                    }
                }
            } else if ("oldColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldColumns.add(ColumnMetadata.load(line));
                    }
                }
            } else if ("newColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newColumns.add(ColumnMetadata.load(line));
                    }
                }
            } else if ("removedColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        removedColumns.add(ColumnMetadata.load(line));
                    }
                }
            } else if ("oldCellCount".equals(field)) {
                int oldCellCount = Integer.parseInt(line.substring(equal + 1));

                oldCells = new CellAtRowCellIndex[oldCellCount];
                for (int i = 0; i < oldCellCount; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldCells[i] = CellAtRowCellIndex.load(line);
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));
            }
        }

        ColumnReorderChange change = new ColumnReorderChange(columnNames);
        change._oldColumns = oldColumns;
        change._newColumns = newColumns;
        change._removedColumns = removedColumns;
        change._oldCells = oldCells;

        return change;
    }
}
