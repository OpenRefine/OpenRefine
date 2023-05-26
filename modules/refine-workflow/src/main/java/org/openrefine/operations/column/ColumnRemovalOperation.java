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

package org.openrefine.operations.column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jsoup.helper.Validate;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.DoesNotApplyException;
import org.openrefine.operations.RowMapOperation;

public class ColumnRemovalOperation extends RowMapOperation {

    final protected List<String> _columnNames;

    /**
     * Constructor.
     *
     * @param columnNames
     *            list of column names to remove
     */
    public ColumnRemovalOperation(List<String> columnNames) {
        this(null, columnNames);
    }

    /**
     * Constructor for JSON deserialization, which accepts "columnName" as a single column to remove, for compatibility
     * with previous syntaxes.
     */
    @JsonCreator
    public ColumnRemovalOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("columnNames") List<String> columnNames) {
        super(EngineConfig.ALL_ROWS);
        _columnNames = new ArrayList<>(columnNames == null ? Collections.emptyList() : columnNames);
        if (columnName != null && !_columnNames.contains(columnName)) {
            _columnNames.add(columnName);
        }
        Validate.isFalse(_columnNames.isEmpty(), "Empty list of columns to remove in column removal operation");
    }

    @JsonProperty("columnNames")
    public List<String> getColumnName() {
        return _columnNames;
    }

    @Override
    public String getDescription() {
        return _columnNames.size() == 1 ? "Remove column " + _columnNames.get(0) : "Remove columns " + String.join(", ", _columnNames);
    }

    @Override
    public ColumnModel getNewColumnModel(Grid state, ChangeContext context) throws Operation.DoesNotApplyException {
        ColumnModel model = state.getColumnModel();
        for (String columnName : _columnNames) {
            int columnIndex = columnIndex(model, columnName);
            model = model.removeColumn(columnIndex);
        }
        return model;
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws Operation.DoesNotApplyException {
        List<Integer> columnIndices = new ArrayList<>(_columnNames.size());
        for (String columnName : _columnNames) {
            int columnIndex = columnIndex(state.getColumnModel(), columnName);
            columnIndices.add(columnIndex);
        }
        columnIndices.sort(Comparator.<Integer> naturalOrder().reversed());
        return mapper(columnIndices, state.getColumnModel().getKeyColumnIndex());
    }

    protected static RowInRecordMapper mapper(List<Integer> columnIndices, int keyColumnIndex) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -120614551816915787L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Row newRow = row;
                // we know that the column indices are sorted in decreasing order,
                // so it is fine to remove the cells in this way
                for (int columnIndex : columnIndices) {
                    newRow = newRow.removeCell(columnIndex);
                }
                return newRow;
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO adapt for arbitrary key column index
                return columnIndices.get(columnIndices.size() - 1) > keyColumnIndex;
            }

        };
    }

    // engine config is never useful, so we remove it from the JSON serialization
    @Override
    @JsonIgnore
    public EngineConfig getEngineConfig() {
        return super.getEngineConfig();
    }
}
