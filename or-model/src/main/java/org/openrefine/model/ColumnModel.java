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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ColumnModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("columns")
    private final List<ColumnMetadata> _columns;

    final private int _keyColumnIndex;

    transient protected Map<String, Integer> _nameToPosition;
    transient protected List<String> _columnNames;

    @JsonCreator
    public ColumnModel(
            @JsonProperty("columns") List<ColumnMetadata> columns) {
        this._columns = Collections.unmodifiableList(columns);
        _keyColumnIndex = 0;
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

    @JsonIgnore
    public int getKeyColumnIndex() {
        return _keyColumnIndex;
    }

    public ColumnModel withColumn(int index, ColumnMetadata column, boolean avoidNameCollision) throws ModelException {
        String name = column.getName();

        if (_nameToPosition.containsKey(name)) {
            if (!avoidNameCollision) {
                throw new ModelException("Duplicated column name");
            } else {
                name = getUnduplicatedColumnName(name);
                column = column.withName(name);
            }
        }
        List<ColumnMetadata> newColumns = getColumns().subList(0, index);
        newColumns.add(column);
        newColumns.addAll(getColumns().subList(index, getColumns().size()));
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

    /**
     * Return the index of the column with the given name.
     * 
     * @param name
     *            column name to look up
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
        if (getColumns().size() > 0) {
            return getKeyColumnIndex();
        }
        return null;
    }

    @JsonProperty("keyColumnName")
    @JsonInclude(Include.NON_NULL)
    public String getKeyColumnName() {
        if (getColumns().size() > 0) {
            return getColumns().get(_keyColumnIndex).getName();
        }
        return null;
    }

    public List<ColumnMetadata> getColumns() {
        return _columns;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ColumnModel)) {
            return false;
        }
        ColumnModel otherModel = (ColumnModel) other;
        return _columns.equals(otherModel.getColumns());
    }
}
