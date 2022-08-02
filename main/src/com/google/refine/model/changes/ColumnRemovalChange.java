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
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnGroup;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class ColumnRemovalChange extends ColumnChange {

    final protected int _oldColumnIndex;
    protected Column _oldColumn;
    protected CellAtRow[] _oldCells;
    protected List<ColumnGroup> _oldColumnGroups;

    public ColumnRemovalChange(int index) {
        _oldColumnIndex = index;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            int columnGroupCount = project.columnModel.columnGroups.size();
            _oldColumnGroups = new ArrayList<ColumnGroup>(columnGroupCount);
            for (int i = columnGroupCount - 1; i >= 0; i--) {
                ColumnGroup columnGroup = project.columnModel.columnGroups.get(i);

                _oldColumnGroups.add(columnGroup);

                if (columnGroup.startColumnIndex <= _oldColumnIndex) {
                    if (columnGroup.startColumnIndex + columnGroup.columnSpan > _oldColumnIndex) {
                        // the group starts before or at _oldColumnIndex
                        // but spans to include _oldColumnIndex

                        if (columnGroup.keyColumnIndex == _oldColumnIndex) {
                            // the key column is removed, so we remove the whole group
                            project.columnModel.columnGroups.remove(i);
                        } else {
                            // otherwise, the group's span has been reduced by 1
                            project.columnModel.columnGroups.set(i, new ColumnGroup(
                                    columnGroup.startColumnIndex,
                                    columnGroup.columnSpan - 1,
                                    columnGroup.keyColumnIndex < _oldColumnIndex ? columnGroup.keyColumnIndex
                                            : (columnGroup.keyColumnIndex - 1)));
                        }
                    }
                } else {
                    // the column removed precedes this whole group
                    project.columnModel.columnGroups.set(i, new ColumnGroup(
                            columnGroup.startColumnIndex - 1,
                            columnGroup.columnSpan,
                            columnGroup.keyColumnIndex - 1));
                }
            }

            _oldColumn = project.columnModel.columns.remove(_oldColumnIndex);
            _oldCells = new CellAtRow[project.rows.size()];
            int cellIndex = _oldColumn.getCellIndex();
            for (int i = 0; i < _oldCells.length; i++) {
                Row row = project.rows.get(i);

                Cell oldCell = null;
                if (cellIndex < row.cells.size()) {
                    oldCell = row.cells.get(cellIndex);
                }
                _oldCells[i] = new CellAtRow(i, oldCell);

                row.setCell(cellIndex, null);
            }

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.columns.add(_oldColumnIndex, _oldColumn);

            int cellIndex = _oldColumn.getCellIndex();
            for (CellAtRow cell : _oldCells) {
                project.rows.get(cell.row).cells.set(cellIndex, cell.cell);
            }

            project.columnModel.columnGroups.clear();
            project.columnModel.columnGroups.addAll(_oldColumnGroups);

            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("oldColumnIndex=");
        writer.write(Integer.toString(_oldColumnIndex));
        writer.write('\n');
        writer.write("oldColumn=");
        _oldColumn.save(writer);
        writer.write('\n');
        writer.write("oldCellCount=");
        writer.write(Integer.toString(_oldCells.length));
        writer.write('\n');
        for (CellAtRow c : _oldCells) {
            c.save(writer, options);
            writer.write('\n');
        }
        writeOldColumnGroups(writer, options, _oldColumnGroups);

        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        int oldColumnIndex = -1;
        Column oldColumn = null;
        CellAtRow[] oldCells = null;
        List<ColumnGroup> oldColumnGroups = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if ("oldColumnIndex".equals(field)) {
                oldColumnIndex = Integer.parseInt(line.substring(equal + 1));
            } else if ("oldColumn".equals(field)) {
                oldColumn = Column.load(line.substring(equal + 1));
            } else if ("oldCellCount".equals(field)) {
                int oldCellCount = Integer.parseInt(line.substring(equal + 1));

                oldCells = new CellAtRow[oldCellCount];
                for (int i = 0; i < oldCellCount; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldCells[i] = CellAtRow.load(line, pool);
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));

                oldColumnGroups = readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }

        ColumnRemovalChange change = new ColumnRemovalChange(oldColumnIndex);
        change._oldColumn = oldColumn;
        change._oldCells = oldCells;
        change._oldColumnGroups = oldColumnGroups != null ? oldColumnGroups : new LinkedList<ColumnGroup>();

        return change;
    }
}
