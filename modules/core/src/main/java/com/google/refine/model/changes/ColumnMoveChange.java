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
import com.google.refine.util.Pool;

public class ColumnMoveChange extends ColumnChange {

    final protected String _columnName;
    final protected int _newColumnIndex;
    protected int _oldColumnIndex;
    protected List<ColumnGroup> _oldColumnGroups;

    public ColumnMoveChange(String columnName, int index) {
        _columnName = columnName;
        _newColumnIndex = index;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            _oldColumnIndex = project.columnModel.getColumnIndexByName(_columnName);

            if (_oldColumnIndex < 0 || _newColumnIndex < 0
                    || _newColumnIndex > project.columnModel.getMaxCellIndex()) {
                throw new RuntimeException("Column index out of range");
            }
            if (_oldColumnGroups == null) {
                _oldColumnGroups = new ArrayList<ColumnGroup>(project.columnModel.columnGroups);
            }

            Column column = project.columnModel.columns.remove(_oldColumnIndex);
            project.columnModel.columns.add(_newColumnIndex, column);
            project.columnModel.columnGroups.clear();

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            Column column = project.columnModel.columns.remove(_newColumnIndex);
            project.columnModel.columns.add(_oldColumnIndex, column);

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
        writer.write("oldColumnIndex=");
        writer.write(Integer.toString(_oldColumnIndex));
        writer.write('\n');
        writer.write("newColumnIndex=");
        writer.write(Integer.toString(_newColumnIndex));
        writer.write('\n');
        writeOldColumnGroups(writer, options, _oldColumnGroups);
        writer.write("/ec/\n"); // end of change marker
    }

    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        String columnName = null;
        int oldColumnIndex = -1;
        int newColumnIndex = -1;
        List<ColumnGroup> oldColumnGroups = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            String value = line.substring(equal + 1);
            if ("oldColumnIndex".equals(field)) {
                oldColumnIndex = Integer.parseInt(value);
            } else if ("newColumnIndex".equals(field)) {
                newColumnIndex = Integer.parseInt(value);
            } else if ("columnName".equals(field)) {
                columnName = value;
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));

                oldColumnGroups = readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }

        ColumnMoveChange change = new ColumnMoveChange(columnName, newColumnIndex);
        change._oldColumnIndex = oldColumnIndex;
        change._oldColumnGroups = oldColumnGroups != null ? oldColumnGroups : new LinkedList<ColumnGroup>();

        return change;
    }
}
