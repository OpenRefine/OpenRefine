package com.metaweb.gridworks.model;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;

public class ColumnModel implements Jsonizable {
    final public List<Column>      columns = new LinkedList<Column>();
    final public List<ColumnGroup> columnGroups = new LinkedList<ColumnGroup>();
    
    private int _maxCellIndex;
    private int _keyColumnIndex;
    
    transient protected Map<String, Column>  _nameToColumn;
    transient protected Map<Integer, Column> _cellIndexToColumn;
    transient protected List<ColumnGroup>    _rootColumnGroups;
    transient protected List<String>		 _columnNames;
    transient boolean _hasDependentRows;
    
    public ColumnModel() {
        internalInitialize();
    }
    
    public void setMaxCellIndex(int maxCellIndex) {
        this._maxCellIndex = Math.max(this._maxCellIndex, maxCellIndex);
    }

    public int getMaxCellIndex() {
        return _maxCellIndex;
    }

    public int allocateNewCellIndex() {
        return ++_maxCellIndex;
    }
    
    public void setKeyColumnIndex(int keyColumnIndex) {
        // TODO: check validity of new cell index, e.g., it's not in any group
        this._keyColumnIndex = keyColumnIndex;
    }

    public int getKeyColumnIndex() {
        return _keyColumnIndex;
    }
    
    public void addColumnGroup(int startColumnIndex, int span, int keyColumnIndex) {
        for (ColumnGroup g : columnGroups) {
            if (g.startColumnIndex == startColumnIndex && g.columnSpan == span) {
                if (g.keyColumnIndex == keyColumnIndex) {
                    return;
                } else {
                    columnGroups.remove(g);
                    break;
                }
            }
        }
        
        ColumnGroup cg = new ColumnGroup(startColumnIndex, span, keyColumnIndex);
        
        columnGroups.add(cg);
        
    }

    public void update() {
        internalInitialize();
    }
    
    public Column getColumnByName(String name) {
        return _nameToColumn.get(name);
    }
    
    public Column getColumnByCellIndex(int cellIndex) {
        return _cellIndexToColumn.get(cellIndex);
    }
    
    public List<String> getColumnNames() {
    	return _columnNames;
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        
        writer.key("hasDependentRows"); writer.value(_hasDependentRows);
        
        writer.key("columns");
        writer.array();
        for (Column column : columns) {
            column.write(writer, options);
        }
        writer.endArray();
        
        if (columns.size() > 0) {
            writer.key("keyCellIndex"); writer.value(getKeyColumnIndex());
            writer.key("keyColumnName"); writer.value(columns.get(_keyColumnIndex).getName());
        }
        
        writer.key("columnGroups");
        writer.array();
        for (ColumnGroup g : _rootColumnGroups) {
            g.write(writer, options);
        }
        writer.endArray();
        
        writer.endObject();
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write("maxCellIndex="); writer.write(Integer.toString(_maxCellIndex)); writer.write('\n');
        writer.write("keyColumnIndex="); writer.write(Integer.toString(_keyColumnIndex)); writer.write('\n');

        writer.write("columnCount="); writer.write(Integer.toString(columns.size())); writer.write('\n');
        for (Column column : columns) {
            column.save(writer); writer.write('\n');
        }
        
        writer.write("columnGroupCount="); writer.write(Integer.toString(columnGroups.size())); writer.write('\n');
        for (ColumnGroup group : columnGroups) {
            group.save(writer); writer.write('\n');
        }
        
        writer.write("/e/\n");
    }

    public void load(LineNumberReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null && !"/e/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("maxCellIndex".equals(field)) {
                _maxCellIndex = Integer.parseInt(value);
            } else if ("keyColumnIndex".equals(field)) {
                _keyColumnIndex = Integer.parseInt(value);
            } else if ("columnCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    columns.add(Column.load(reader.readLine()));
                }
            } else if ("columnGroupCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                for (int i = 0; i < count; i++) {
                    columnGroups.add(ColumnGroup.load(reader.readLine()));
                }
            }
        }
        
        internalInitialize();
    }
    
    protected void internalInitialize() {
        generateMaps();
        
        // Turn the flat list of column groups into a tree
        
        _rootColumnGroups = new LinkedList<ColumnGroup>(columnGroups);
        Collections.sort(_rootColumnGroups, new Comparator<ColumnGroup>() {
            public int compare(ColumnGroup o1, ColumnGroup o2) {
                int firstDiff = o1.startColumnIndex - o2.startColumnIndex;
                return firstDiff != 0 ?
                    firstDiff : // whichever group that starts first goes first 
                    (o2.columnSpan - o1.columnSpan); // otherwise, the larger group goes first
            }
        });
        
        for (int i = _rootColumnGroups.size() - 1; i >= 0; i--) {
            ColumnGroup g = _rootColumnGroups.get(i);
            
            for (int j = i + 1; j < _rootColumnGroups.size(); j++) {
                ColumnGroup g2 = _rootColumnGroups.get(j);
                if (g2.parentGroup == null && g.contains(g2)) {
                    g2.parentGroup = g;
                    g.subgroups.add(g2);
                }
            }
        }
        
        for (int i = _rootColumnGroups.size() - 1; i >= 0; i--) {
            if (_rootColumnGroups.get(i).parentGroup != null) {
                _rootColumnGroups.remove(i);
            }
        }
    }
    
    protected void generateMaps() {
        _nameToColumn = new HashMap<String, Column>();
        _cellIndexToColumn = new HashMap<Integer, Column>();
        _columnNames = new ArrayList<String>();
        
        for (Column column : columns) {
            _nameToColumn.put(column.getName(), column);
            _cellIndexToColumn.put(column.getCellIndex(), column);
            _columnNames.add(column.getName());
        }
    }
}
