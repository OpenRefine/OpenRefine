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
import java.util.Properties;

import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.Column;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class ColumnRemovalChange extends ColumnChange {

    final protected int _oldColumnIndex;
    protected Column _oldColumn;
    protected CellAtRow[] _oldCells;

    public ColumnRemovalChange(int index) {
        _oldColumnIndex = index;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {

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

        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader) throws Exception {
        int oldColumnIndex = -1;
        Column oldColumn = null;
        CellAtRow[] oldCells = null;

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
                        oldCells[i] = CellAtRow.load(line);
                    }
                }
            }
        }

        ColumnRemovalChange change = new ColumnRemovalChange(oldColumnIndex);
        change._oldColumn = oldColumn;
        change._oldCells = oldCells;

        return change;
    }
}
