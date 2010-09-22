package com.google.refine.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.refine.history.Change;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.Pool;

public class ColumnReorderChange extends ColumnChange {
    final protected List<String>    _columnNames;
    protected List<Column>          _oldColumns;
    protected List<Column>          _newColumns;
    
    public ColumnReorderChange(List<String> columnNames) {
        _columnNames = columnNames;
    }
    
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
            }
            
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_newColumns);
            project.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_oldColumns);
            project.update();
        }
    }

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
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<String> columnNames = new ArrayList<String>();
        List<Column> oldColumns = new ArrayList<Column>();
        List<Column> newColumns = new ArrayList<Column>();
        
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
            }
        }
        
        ColumnReorderChange change = new ColumnReorderChange(columnNames);
        change._oldColumns = oldColumns;
        change._newColumns = newColumns;
        
        return change;
    }
}
