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
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

import org.openrefine.model.recon.ReconConfig;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.util.ColumnDependencyException;

/**
 * The list of columns in a project. For each column, this holds the associated {@link ColumnMetadata}.
 * <p>
 * This class has only immutable members are is meant to be modified by creating copies of it, for instance using the
 * "with" methods provided.
 */
public class ColumnModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("columns")
    private final List<ColumnMetadata> _columns;

    private final int _keyColumnIndex;
    private final boolean _hasRecords;

    protected final Map<String, Integer> _nameToPosition;
    protected final Map<ColumnId, Integer> _idToPosition;
    protected final List<String> _columnNames;

    @JsonCreator
    public ColumnModel(
            @JsonProperty("columns") List<ColumnMetadata> columns,
            @JsonProperty("keyCellIndex") int keyColumnIndex,
            @JsonProperty("hasRecords") boolean hasRecords) {
        this._columns = Collections.unmodifiableList(columns);
        _keyColumnIndex = keyColumnIndex;
        _hasRecords = hasRecords;
        _nameToPosition = new HashMap<>();
        _idToPosition = new HashMap<>();
        _columnNames = new ArrayList<String>();
        int index = 0;
        for (ColumnMetadata column : columns) {
            if (_nameToPosition.containsKey(column.getName())) {
                throw new IllegalArgumentException(
                        String.format("Duplicate columns for name %s", column.getName()));
            }
            _nameToPosition.put(column.getName(), index);
            _idToPosition.put(column.getColumnId(), index);
            _columnNames.add(column.getName());
            index++;
        }
    }

    public ColumnModel(List<ColumnMetadata> columns) {
        this(columns, 0, false);
    }

    /**
     * @return the index of the column used as key to group rows into records
     */
    @JsonIgnore
    public int getKeyColumnIndex() {
        return _keyColumnIndex;
    }

    /**
     * Returns a copy of this column model with a different key column.
     * 
     * @param keyColumnIndex
     *            the index of the column to use as a key
     */
    public ColumnModel withKeyColumnIndex(int keyColumnIndex) {
        return new ColumnModel(_columns, keyColumnIndex, _hasRecords);
    }

    /**
     * Replace a column metadata at the given index. This instance is left as is and a modified copy is returned.
     * 
     * @param index
     *            the index of the column
     * @param column
     *            the new metadata
     * @throws ModelException
     *             if the new column name conflicts with another column
     */
    public ColumnModel replaceColumn(int index, ColumnMetadata column) throws ModelException {
        String name = column.getName();

        if (_nameToPosition.containsKey(name) && _nameToPosition.get(name) != index) {
            throw new ModelException("Duplicated column name");
        }
        List<ColumnMetadata> newColumns = new ArrayList<>();
        newColumns.addAll(getColumns().subList(0, index));
        newColumns.add(column);
        newColumns.addAll(getColumns().subList(index + 1, getColumns().size()));
        return new ColumnModel(newColumns, _keyColumnIndex, _hasRecords);
    }

    /**
     * Replaces the recon config at the given column index.
     * 
     * @return a modified copy of this column model.
     */
    public ColumnModel withReconConfig(int index, ReconConfig config) {
        try {
            return replaceColumn(index, _columns.get(index).withReconConfig(config));
        } catch (ModelException e) {
            return null; // unreachable
        }
    }

    /**
     * Updates the last modification field of a column to a newer history entry id.
     * 
     * @return a modified copy of this column model
     */
    public ColumnModel markColumnAsModified(int index, long historyEntryId) {
        try {
            return replaceColumn(index, _columns.get(index).markAsModified(historyEntryId));
        } catch (ModelException e) {
            return null; // unreachable
        }
    }

    /**
     * Inserts a column at the given index. It returns a modified copy of this column model.
     * 
     * @param index
     *            the index where to insert the column
     * @param column
     *            the column metadata
     * @throws ModelException
     *             if the name conflicts with another column
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
        return new ColumnModel(newColumns, _keyColumnIndex, _hasRecords);
    }

    /**
     * Inserts a column at the given index, possibly changing the name to ensure that it does not conflict with any
     * other column. It returns a modified copy of this column model.
     * 
     * @param index
     *            the place where to insert the column
     * @param column
     *            the column metadata
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
        return new ColumnModel(newColumns, _keyColumnIndex, _hasRecords);
    }

    /**
     * Shortcut for {@link #insertUnduplicatedColumn(int, ColumnMetadata)}, for inserting at the last position.
     */
    public ColumnModel appendUnduplicatedColumn(ColumnMetadata columnMetadata) {
        return insertUnduplicatedColumn(getColumns().size(), columnMetadata);
    }

    /**
     * Change the name of a column.
     * 
     * @param index
     *            the index of the column
     * @param newName
     *            the new name to give to the column
     * @throws ModelException
     *             if the new name conflicts with any other column
     */
    public ColumnModel renameColumn(int index, String newName) throws ModelException {
        ColumnMetadata newColumn = _columns.get(index);
        return replaceColumn(index, newColumn.withName(newName));
    }

    /**
     * Removes a column at the given index. It returns a modified copy of this column model.
     * 
     * @param index
     *            the index of the column to remove
     */
    public ColumnModel removeColumn(int index) {
        List<ColumnMetadata> newColumns = new ArrayList<>();
        List<ColumnMetadata> columns = getColumns();
        newColumns.addAll(columns.subList(0, index));
        newColumns.addAll(columns.subList(index + 1, columns.size()));
        return new ColumnModel(newColumns, _keyColumnIndex, _hasRecords);
    }

    /**
     * Given another column model with the same number of columns, merge the recon configuration and other metadata in
     * each n-th column.
     *
     * @throws IllegalArgumentException
     *             if the number of columns is different or columns have incompatible reconciliation configurations.
     */
    public ColumnModel merge(ColumnModel other) {
        List<ColumnMetadata> otherColumns = other.getColumns();
        if (otherColumns.size() != _columns.size()) {
            throw new IllegalArgumentException(
                    String.format("Attempting to merge column models with %d and %d columns",
                            _columns.size(), otherColumns.size()));
        }

        List<ColumnMetadata> newColumns = new ArrayList<>(_columns.size());
        for (int i = 0; i != _columns.size(); i++) {
            newColumns.add(_columns.get(i));
        }
        return new ColumnModel(newColumns, _keyColumnIndex, _hasRecords);
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

    /**
     * Utility method to get a column index by name and throw an exception if this column does not exist.
     */
    public int getRequiredColumnIndex(String columnName) throws MissingColumnException {
        if (_nameToPosition.containsKey(columnName)) {
            return _nameToPosition.get(columnName);
        } else {
            throw new MissingColumnException(columnName);
        }
    }

    /**
     * Utility method to get a column index based on its column id (original column name and version).
     *
     * @throws ColumnDependencyException
     *             if the column does not exist
     */
    public int getRequiredColumnIndex(ColumnId id) throws ColumnDependencyException {
        if (_idToPosition.containsKey(id)) {
            return _idToPosition.get(id);
        } else {
            throw new ColumnDependencyException(id);
        }
    }

    /**
     * Checks whether this column model contains a column with the sepecified id.
     *
     * @param columnId
     *            the id of the column to lookup
     * @return true if the column is present
     */
    public boolean hasColumnId(ColumnId columnId) {
        return _idToPosition.containsKey(columnId);
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

    /**
     * Returns whether the associated grid has an intentional records structure. This happens when the grid is produced
     * by an importer or operation which introduces a record structure, which can be preserved by following operations.
     */
    @JsonProperty("hasRecords")
    public boolean hasRecords() {
        return _hasRecords;
    }

    /**
     * Returns a copy of this column model with a different value for the {@link #hasRecords()} field.
     */
    public ColumnModel withHasRecords(boolean newHasRecords) {
        return new ColumnModel(_columns, _keyColumnIndex, newHasRecords);
    }

    public List<ColumnMetadata> getColumns() {
        return _columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnModel that = (ColumnModel) o;
        return _keyColumnIndex == that._keyColumnIndex && _hasRecords == that._hasRecords && _columns.equals(that._columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_columns, _keyColumnIndex, _hasRecords);
    }

    @Override
    public String toString() {
        return String.format("[ColumnModel: %s]", StringUtils.join(_columns, ", "));
    }

}
