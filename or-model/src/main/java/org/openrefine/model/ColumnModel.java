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

package org.openrefine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconStats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ColumnModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("columns")
    private
    final List<ColumnMetadata>      _columns;
    
    final private int _keyColumnIndex;
    
    protected Map<String, Integer>         _nameToPosition;
    protected List<String>                 _columnNames;
    
    @JsonCreator
    public ColumnModel(
            @JsonProperty("columns")
            List<ColumnMetadata> columns,
            @JsonProperty("keyCellIndex")
            int keyColumnIndex) {
        this._columns = Collections.unmodifiableList(columns);
        _keyColumnIndex = keyColumnIndex;
        _nameToPosition = new HashMap<>();
        _columnNames = new ArrayList<String>();
        int index = 0;
        for (ColumnMetadata column : columns) {
            if (_nameToPosition.containsKey(column.getName())) {
                throw new IllegalArgumentException(
                        String.format("Duplicate columns for name %1", column.getName()));
            }
            _nameToPosition.put(column.getName(), index);
            _columnNames.add(column.getName());
            index++;
        }
    }
    
    public ColumnModel(List<ColumnMetadata> columns) {
        this(columns, 0);
    }

    /**
     * @return the index of the column used as key to group rows into records
     */
    @JsonIgnore // see getKeyCellIndex below
    public int getKeyColumnIndex() {
        return _keyColumnIndex;
    }
    
    /**
     * Returns a copy of this column model with a different
     * key column.
     * @param keyColumnIndex the index of the column to use as a key
     * @return
     */
    public ColumnModel withKeyColumnIndex(int keyColumnIndex) {
        return new ColumnModel(_columns, keyColumnIndex);
    }
    
    /**
     * Replace a column metadata at the given index
     * @param index the index of the column
     * @param column the new metadata
     * @return
     * @throws ModelException if the new column name conflicts with another column
     */
    public ColumnModel replaceColumn(int index, ColumnMetadata column) throws ModelException {
        String name = column.getName();
        
        if (_nameToPosition.containsKey(name) && _nameToPosition.get(name) != index) {
            throw new ModelException("Duplicated column name");
        }
        List<ColumnMetadata> newColumns = new ArrayList<>();
        newColumns.addAll(getColumns().subList(0, index));
        newColumns.add(column);
        newColumns.addAll(getColumns().subList(index+1, getColumns().size()));
        return new ColumnModel(newColumns);
    }
    
    /**
     * Replaces the recon statistics at the given column index
     * @param index
     * @param stats
     * @return
     */
    public ColumnModel withReconStats(int index, ReconStats stats) {
        try {
            return replaceColumn(index, _columns.get(index).withReconStats(stats));
        } catch (ModelException e) {
            return null; // unreachable
        }
    }
    
    /**
     * Replaces the recon config at the given column index
     * @param index
     * @param config
     * @return
     */
    public ColumnModel withReconConfig(int index, ReconConfig config) {
        try {
            return replaceColumn(index, _columns.get(index).withReconConfig(config));
        } catch (ModelException e) {
            return null; // unreachable
        }
    }
    
    /**
     * Inserts a column at the given index
     * @param index the index where to insert the column
     * @param column the column metadata
     * @return
     * @throws ModelException if the name conflicts with another column
     */
    public ColumnModel insertColumn(int index, ColumnMetadata column) throws ModelException {
        String name = column.getName();
        
        if (_nameToPosition.containsKey(name)) {
            throw new ModelException("Duplicated column name");
        }
        List<ColumnMetadata> newColumns = new ArrayList<>();
        newColumns.addAll(getColumns().subList(0, index));
        newColumns.add(column);
        newColumns.addAll(getColumns().subList(index, getColumns().size()));
        return new ColumnModel(newColumns);
    }
    
    /**
     * Inserts a column at the given index, possibly changing the name 
     * to ensure that it does not conflict with any other column
     * @param index the place where to insert the column
     * @param column the column metadata
     * @return
     */
    public ColumnModel insertUnduplicatedColumn(int index, ColumnMetadata column) {
        String name = column.getName();
        
        if (_nameToPosition.containsKey(name)) {
            name = getUnduplicatedColumnName(name);
            column = column.withName(name);
        }
        List<ColumnMetadata> newColumns = new ArrayList<>();
        newColumns.addAll(getColumns().subList(0, index));
        newColumns.add(column);
        newColumns.addAll(getColumns().subList(index, getColumns().size()));
        return new ColumnModel(newColumns);
    }
    
    /**
     * Shortcut for the above, for inserting at the last position.
     * @param columnMetadata
     * @return
     */
    public ColumnModel appendUnduplicatedColumn(ColumnMetadata columnMetadata) {
        return insertUnduplicatedColumn(getColumns().size(), columnMetadata);
    }
    
    /**
     * Change the name of a column
     * @param index the index of the column
     * @param newName the new name to give to the column
     * @return
     * @throws ModelException if the new name conflicts with any other column
     */
    public ColumnModel renameColumn(int index, String newName) throws ModelException {
        ColumnMetadata newColumn = _columns.get(index);
        return replaceColumn(index, newColumn.withName(newName));
    }
    
    /**
     * Removes a column at the given index
     * @param index the index of the column to remove
     * @return
     */
    public ColumnModel removeColumn(int index) {
        List<ColumnMetadata> newColumns = new ArrayList<>();
        List<ColumnMetadata> columns = getColumns();
        newColumns.addAll(columns.subList(0, index));
        newColumns.addAll(columns.subList(index+1, columns.size()));
        return new ColumnModel(newColumns);
    }
    
    
    public String getUnduplicatedColumnName(String baseName) {
        String name = baseName;
        int i = 1;
        while (true) {
            if (_nameToPosition.containsKey(name)) {
                i++;
                name = baseName + i;
            } else {
                break;
            }
        }
        return name;
    }
    
    public ColumnMetadata getColumnByName(String name) {
        if (_nameToPosition.containsKey(name)) {
            int index = _nameToPosition.get(name);
            return _columns.get(index);
        }
        return null;
    }
    
    public ColumnMetadata getColumnByIndex(int cellIndex) {
        return _columns.get(cellIndex);
    }
    
    /**
     * Return the index of the column with the given name.
     * 
     * @param name column name to look up
     * @return index of column with given name or -1 if not found.
     */
    public int getColumnIndexByName(String name) {
        if (_nameToPosition.containsKey(name)) {
            return _nameToPosition.get(name);
        } else {
            return -1;
        }
    }
    
    
    @JsonIgnore
    public List<String> getColumnNames() {
        return _columnNames;
    }
    
    @JsonProperty("keyCellIndex")
    @JsonInclude(Include.NON_NULL)
    public Integer getJsonKeyCellIndex() {
        if(getColumns().size() > 0) {
            return getKeyColumnIndex();
        }
        return null;
    }
    
    @JsonProperty("keyColumnName")
    @JsonInclude(Include.NON_NULL)
    public String getKeyColumnName() {
        if(getColumns().size() > 0) {
            return getColumns().get(_keyColumnIndex).getName();
        }
        return null;
    }


    public List<ColumnMetadata> getColumns() {
        return _columns;
    }
    
    @Override
    public boolean equals(Object other) {
    	if(!(other instanceof ColumnModel)) {
    		return false;
    	}
    	ColumnModel otherModel = (ColumnModel)other;
    	return _columns.equals(otherModel.getColumns());
    }
    
    @Override
    public int hashCode() {
        return _columns.hashCode();
    }
    
    @Override
    public String toString() {
        return String.format("[ColumnModel: %s]", StringUtils.join(_columns, ", "));
    }

}
