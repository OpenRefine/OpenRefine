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

import com.google.refine.ProjectManager;
import com.google.refine.history.Change;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnGroup;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class MassRowColumnChange implements Change {

    final protected List<Column> _newColumns;
    final protected List<Row> _newRows;
    protected List<Column> _oldColumns;
    protected List<Row> _oldRows;
    protected List<ColumnGroup> _oldColumnGroups;

    public MassRowColumnChange(List<Column> newColumns, List<Row> newRows) {
        _newColumns = newColumns;
        _newRows = newRows;
    }

    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_oldColumnGroups == null) {
                _oldColumnGroups = new ArrayList<ColumnGroup>(project.columnModel.columnGroups);
            }
            if (_oldColumns == null) {
                _oldColumns = new ArrayList<Column>(project.columnModel.columns);
            }
            if (_oldRows == null) {
                _oldRows = new ArrayList<Row>(project.rows);
            }

            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_newColumns);
            project.columnModel.columnGroups.clear();

            project.rows.clear();
            project.rows.addAll(_newRows);

            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProject(project.id);

            project.update();
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_oldColumns);

            project.columnModel.columnGroups.clear();
            project.columnModel.columnGroups.addAll(_oldColumnGroups);

            project.rows.clear();
            project.rows.addAll(_oldRows);

            ProjectManager.singleton.getLookupCacheManager().flushLookupsInvolvingProject(project.id);

            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newColumnCount=");
        writer.write(Integer.toString(_newColumns.size()));
        writer.write('\n');
        for (Column column : _newColumns) {
            column.save(writer);
            writer.write('\n');
        }
        writer.write("oldColumnCount=");
        writer.write(Integer.toString(_oldColumns.size()));
        writer.write('\n');
        for (Column column : _oldColumns) {
            column.save(writer);
            writer.write('\n');
        }
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
        List<Column> oldColumns = null;
        List<Column> newColumns = null;
        List<ColumnGroup> oldColumnGroups = null;

        List<Row> oldRows = null;
        List<Row> newRows = null;

        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);

            if ("oldRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));

                oldRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldRows.add(Row.load(line, pool));
                    }
                }
            } else if ("newRowCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));

                newRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newRows.add(Row.load(line, pool));
                    }
                }
            } else if ("oldColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));

                oldColumns = new ArrayList<Column>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldColumns.add(Column.load(line));
                    }
                }
            } else if ("newColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));

                newColumns = new ArrayList<Column>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newColumns.add(Column.load(line));
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));

                oldColumnGroups = ColumnChange.readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }

        MassRowColumnChange change = new MassRowColumnChange(newColumns, newRows);
        change._oldColumns = oldColumns;
        change._oldRows = oldRows;
        change._oldColumnGroups = oldColumnGroups != null ? oldColumnGroups : new LinkedList<ColumnGroup>();

        return change;
    }
}
