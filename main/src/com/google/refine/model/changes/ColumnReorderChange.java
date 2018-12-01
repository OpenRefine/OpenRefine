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

public class ColumnReorderChange extends ColumnChange {
    final protected List<String>    _columnNames;
    protected List<Column>          _oldColumns;
    protected List<Column>          _newColumns;
    protected List<ColumnGroup>     _oldColumnGroups;
    
    public ColumnReorderChange(List<String> columnNames) {
        _columnNames = columnNames;
    }
    
    @Override
    public void apply(Project project) {
        synchronized (project) {
            if (_newColumns == null) {
                _newColumns = new ArrayList<Column>();
                _oldColumns = new ArrayList<Column>(project.columnModel.columns);
                
                for (String n : _columnNames) {
                    Column column = project.columnModel.getColumnByName(n);
                    if (column != null) {
                        _newColumns.add(column);
                    }
                }
                
                _oldColumnGroups = new ArrayList<ColumnGroup>(project.columnModel.columnGroups);
            }
            
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_newColumns);
            project.columnModel.columnGroups.clear();

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

            project.update();
        }
    }

    @Override
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("columnNameCount="); writer.write(Integer.toString(_columnNames.size())); writer.write('\n');
        for (String n : _columnNames) {
            writer.write(n);
            writer.write('\n');
        }
        writer.write("oldColumnCount="); writer.write(Integer.toString(_oldColumns.size())); writer.write('\n');
        for (Column c : _oldColumns) {
            c.save(writer);
            writer.write('\n');
        }
        writer.write("newColumnCount="); writer.write(Integer.toString(_newColumns.size())); writer.write('\n');
        for (Column c : _newColumns) {
            c.save(writer);
            writer.write('\n');
        }
        writeOldColumnGroups(writer, options, _oldColumnGroups);
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<String> columnNames = new ArrayList<String>();
        List<Column> oldColumns = new ArrayList<Column>();
        List<Column> newColumns = new ArrayList<Column>();
        List<ColumnGroup> oldColumnGroups = null;
        
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
                        oldColumns.add(Column.load(line));
                    }
                }
            } else if ("newColumnCount".equals(field)) {
                int count = Integer.parseInt(line.substring(equal + 1));
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newColumns.add(Column.load(line));
                    }
                }
            } else if ("oldColumnGroupCount".equals(field)) {
                int oldColumnGroupCount = Integer.parseInt(line.substring(equal + 1));
                
                oldColumnGroups = readOldColumnGroups(reader, oldColumnGroupCount);
            }
        }
        
        ColumnReorderChange change = new ColumnReorderChange(columnNames);
        change._oldColumns = oldColumns;
        change._newColumns = newColumns;
        change._oldColumnGroups = oldColumnGroups != null ?
                oldColumnGroups : new LinkedList<ColumnGroup>();
        
        return change;
    }
}
