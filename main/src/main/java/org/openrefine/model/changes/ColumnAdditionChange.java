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
import org.openrefine.model.Column;
import org.openrefine.model.Project;
import org.openrefine.model.Row;

public class ColumnAdditionChange extends ColumnChange {

    final protected String _columnName;
    final protected int _columnIndex;
    final protected CellAtRow[] _newCells;
    protected int _newCellIndex = -1;

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
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader) throws Exception {
        String columnName = null;
        int columnIndex = -1;
        int newCellIndex = -1;
        List<CellAtRow> newCells = null;

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
                        newCells.add(CellAtRow.load(line));
                    }
                }
            }
        }

        ColumnAdditionChange change = new ColumnAdditionChange(columnName, columnIndex, newCells);
        change._newCellIndex = newCellIndex;

        return change;
    }
}
