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
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnGroup;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class ColumnAdditionChange extends ColumnChange {

    final protected String _columnName;
    final protected int _columnIndex;
    final protected CellAtRow[] _newCells;
    protected int _newCellIndex = -1;
    protected List<ColumnGroup> _oldColumnGroups;

    public ColumnAdditionChange(String columnName, int columnIndex, List<CellAtRow> newCells) {
        _columnName = columnName;
        _columnIndex = columnIndex;
        _newCells = new CellAtRow[newCells.size()];
        newCells.toArray(_newCells);
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_newCellIndex < 0) {
                _newCellIndex = project.columnModel.allocateNewCellIndex();
            }

            int columnGroupCount = project.columnModel.columnGroups.size();
            _oldColumnGroups = new ArrayList<ColumnGroup>(columnGroupCount);
            for (int i = columnGroupCount - 1; i >= 0; i--) {
                ColumnGroup columnGroup = project.columnModel.columnGroups.get(i);

                _oldColumnGroups.add(columnGroup);

                if (columnGroup.startColumnIndex <= _columnIndex) {
                    if (columnGroup.startColumnIndex + columnGroup.columnSpan > _columnIndex) {
                        // the new column is inserted right in the middle of the group
                        project.columnModel.columnGroups.set(i, new ColumnGroup(
                                columnGroup.startColumnIndex,
                                columnGroup.columnSpan + 1,
                                columnGroup.keyColumnIndex < _columnIndex ? columnGroup.keyColumnIndex : (columnGroup.keyColumnIndex + 1)));
                    }
                } else {
                    // the new column precedes this whole group
                    project.columnModel.columnGroups.set(i, new ColumnGroup(
                            columnGroup.startColumnIndex + 1,
                            columnGroup.columnSpan,
                            columnGroup.keyColumnIndex + 1));
                }
            }

            Column column = new Column(_newCellIndex, _columnName);
            project.columnModel.columns.add(_columnIndex, column);
            try {
                for (CellAtRow cell : _newCells) {
                    project.rows.get(cell.row).setCell(_newCellIndex, cell.cell);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            for (CellAtRow cell : _newCells) {
                Row row = project.rows.get(cell.row);
                row.setCell(_newCellIndex, null);
            }

            project.columnModel.columns.remove(_columnIndex);

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
        writer.write("columnIndex=");
        writer.write(Integer.toString(_columnIndex));
        writer.write('\n');
        writer.write("newCellIndex=");
        writer.write(Integer.toString(_newCellIndex));
        writer.write('\n');
        writer.write("newCellCount=");
        writer.write(Integer.toString(_newCells.length));
        writer.write('\n');
        for (CellAtRow c : _newCells) {
            c.save(writer, options);
            writer.write('\n');
        }
        writeOldColumnGroups(writer, options, _oldColumnGroups);
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String columnName = null;
        int columnIndex = -1;
        int newCellIndex = -1;
        List<CellAtRow> newCells = null;
        List<ColumnGroup> oldColumnGroups = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if ("columnName".equals(field)) {
                columnName = line.substring(equal + 1);
            } else if ("columnIndex".equals(field)) {
                columnIndex = Integer.parseInt(line.substring(equal + 1));
            } else if ("newCellIndex".equals(field)) {
                newCellIndex = Integer.parseInt(line.substring(equal + 1));
            } else if ("newCellCount".equals(field)) {
                int newCellCount = Integer.parseInt(line.substring(equal + 1));

                newCells = new ArrayList<CellAtRow>(newCellCount);
                for (int i = 0; i < newCellCount; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newCells.add(CellAtRow.load(line, pool));
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));

                oldColumnGroups = readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }

        ColumnAdditionChange change = new ColumnAdditionChange(columnName, columnIndex, newCells);
        change._newCellIndex = newCellIndex;
        change._oldColumnGroups = oldColumnGroups != null ? oldColumnGroups : new LinkedList<ColumnGroup>();

        return change;
    }
}
