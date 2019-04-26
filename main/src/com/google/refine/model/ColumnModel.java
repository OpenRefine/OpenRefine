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

package com.google.refine.model;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ColumnModel  {
    @JsonProperty("columns")
    final public List<Column>      columns = new LinkedList<Column>();
    @JsonProperty("columnGroups")
    final public List<ColumnGroup> columnGroups = new LinkedList<ColumnGroup>();
    
    private int _maxCellIndex = -1;
    private int _keyColumnIndex;
    
    transient protected Map<String, Column>  _nameToColumn;
    transient protected Map<Integer, Column> _cellIndexToColumn;
    transient protected List<ColumnGroup>    _rootColumnGroups;
    transient protected List<String>         _columnNames;
    
    public ColumnModel() {
        internalInitialize();
    }
    
    synchronized public void setMaxCellIndex(int maxCellIndex) {
        this._maxCellIndex = Math.max(this._maxCellIndex, maxCellIndex);
    }

    @JsonIgnore
    synchronized public int getMaxCellIndex() {
        return _maxCellIndex;
    }

    /**
     * @return the next available cell index
     */
    synchronized public int allocateNewCellIndex() {
        return ++_maxCellIndex;
    }
    
    synchronized public void setKeyColumnIndex(int keyColumnIndex) {
        // TODO: check validity of new cell index, e.g., it's not in any group
        this._keyColumnIndex = keyColumnIndex;
    }

    @JsonIgnore
    synchronized public int getKeyColumnIndex() {
        return _keyColumnIndex;
    }
    
    synchronized public void addColumnGroup(int startColumnIndex, int span, int keyColumnIndex) {
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
    
    synchronized public void addColumn(int index, Column column, boolean avoidNameCollision) throws ModelException {
        String name = column.getName();
        
        if (_nameToColumn.containsKey(name)) {
            if (!avoidNameCollision) {
                throw new ModelException("Duplicated column name");
            } else {
                name = getUnduplicatedColumnName(name);
                column.setName(name);
            }
        }
        
        columns.add(index < 0 ? columns.size() : index, column);
        _nameToColumn.put(name, column); // so the next call can check
    }
    
    synchronized public String getUnduplicatedColumnName(String baseName) {
        String name = baseName;
        int i = 1;
        while (true) {
            if (_nameToColumn.containsKey(name)) {
                i++;
                name = baseName + i;
            } else {
                break;
            }
        }
        return name;
    }
    
    synchronized public Column getColumnByName(String name) {
        return _nameToColumn.get(name);
    }
    
    /**
     * Return the index of the column with the given name.
     * 
     * @param name column name to look up
     * @return index of column with given name or -1 if not found.
     */
    synchronized public int getColumnIndexByName(String name) {
        for (int i = 0; i < _columnNames.size(); i++) {
            String s = _columnNames.get(i);
            if (name.equals(s)) {
                return i;
            }
        }
        return -1;
    }
    
    synchronized public Column getColumnByCellIndex(int cellIndex) {
        return _cellIndexToColumn.get(cellIndex);
    }
    
    @JsonIgnore
    synchronized public List<String> getColumnNames() {
        return _columnNames;
    }
    
    @JsonProperty("keyCellIndex")
    @JsonInclude(Include.NON_NULL)
    public Integer getJsonKeyCellIndex() {
        if(columns.size() > 0) {
            return getKeyColumnIndex();
        }
        return null;
    }
    
    @JsonProperty("keyColumnName")
    @JsonInclude(Include.NON_NULL)
    public String getKeyColumnName() {
        if(columns.size() > 0) {
            return columns.get(_keyColumnIndex).getName();
        }
        return null;
    }
    
    synchronized public void save(Writer writer, Properties options) throws IOException {
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

    synchronized public void load(LineNumberReader reader) throws Exception {
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
    
    synchronized protected void internalInitialize() {
        generateMaps();
        
        // Turn the flat list of column groups into a tree
        
        _rootColumnGroups = new LinkedList<ColumnGroup>(columnGroups);
        Collections.sort(_rootColumnGroups, new Comparator<ColumnGroup>() {
            @Override
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
        int maxCellIndex = -1;
        for (Column column : columns) {
            _nameToColumn.put(column.getName(), column);
            int cidx = column.getCellIndex();
            if (cidx > maxCellIndex) {
                maxCellIndex = cidx;
            }
            _cellIndexToColumn.put(cidx, column);
            _columnNames.add(column.getName());
        }
        _maxCellIndex = maxCellIndex;
    }
    
    /**
     * Clear cached value computations for all columns
     */
    public void clearPrecomputes() {
        for (Column column : columns) {
            column.clearPrecomputes();
        }
    }
}
