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
import com.google.refine.model.Row;
import com.google.refine.util.Pool;

public class MassRowColumnChange implements Change {
    final protected List<Column>    _newColumns;
    final protected List<Row>       _newRows;
    protected List<Column>          _oldColumns;
    protected List<Row>             _oldRows;
    
    public MassRowColumnChange(List<Column> newColumns, List<Row> newRows) {
        _newColumns = newColumns;
        _newRows = newRows;
    }
    
    public void apply(Project project) {
        synchronized (project) {
            _oldColumns = new ArrayList<Column>(project.columnModel.columns);
            _oldRows = new ArrayList<Row>(project.rows);
            
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_newColumns);
            
            project.rows.clear();
            project.rows.addAll(_newRows);
            
            project.update();
        }
    }

    public void revert(Project project) {
        synchronized (project) {
            project.columnModel.columns.clear();
            project.columnModel.columns.addAll(_oldColumns);
            
            project.rows.clear();
            project.rows.addAll(_oldRows);
            
            project.update();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
        writer.write("newColumnCount="); writer.write(Integer.toString(_newColumns.size())); writer.write('\n');
        for (Column column : _newColumns) {
            column.save(writer);
            writer.write('\n');
        }
        writer.write("oldColumnCount="); writer.write(Integer.toString(_oldColumns.size())); writer.write('\n');
        for (Column column : _oldColumns) {
            column.save(writer);
            writer.write('\n');
        }
        writer.write("newRowCount="); writer.write(Integer.toString(_newRows.size())); writer.write('\n');
        for (Row row : _newRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("oldRowCount="); writer.write(Integer.toString(_oldRows.size())); writer.write('\n');
        for (Row row : _oldRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
        List<Column> oldColumns = null;
        List<Column> newColumns = null;
        
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
            }
        }
        
        MassRowColumnChange change = new MassRowColumnChange(newColumns, newRows);
        change._oldColumns = oldColumns;
        change._oldRows = oldRows;
        
        return change;
    }
}
