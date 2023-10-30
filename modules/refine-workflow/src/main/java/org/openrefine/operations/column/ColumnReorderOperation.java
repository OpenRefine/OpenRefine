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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

public class ColumnReorderOperation extends RowMapOperation {

    final protected List<String> _columnNames;

    @JsonCreator
    public ColumnReorderOperation(
            @JsonProperty("columnNames") List<String> columnNames) {
        super(EngineConfig.ALL_ROWS);
        _columnNames = columnNames;
        Set<String> deduplicated = new HashSet<>(_columnNames);
        if (deduplicated.size() != _columnNames.size()) {
            throw new IllegalArgumentException("Duplicates in the list of final column names");
        }
    }

    @JsonProperty("columnNames")
    public List<String> getColumnNames() {
        return _columnNames;
    }

    @Override
    public String getDescription() {
        return "Reorder columns";
    }

    @Override
    public ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        ColumnModel model = columnModel;
        List<ColumnMetadata> columns = new ArrayList<>(_columnNames.size());
        for (String columnName : _columnNames) {
            ColumnMetadata meta = model.getColumnByName(columnName);
            if (meta == null) {
                throw new MissingColumnException(columnName);
            }
            columns.add(meta);
        }
        return new ColumnModel(columns);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, long estimatedRowCount, ChangeContext context)
            throws OperationException {
        // Build a map from new indices to original ones
        List<Integer> origIndex = new ArrayList<>(_columnNames.size());
        for (int i = 0; i != _columnNames.size(); i++) {
            origIndex.add(columnModel.getRequiredColumnIndex(_columnNames.get(i)));
        }

        int keyColumnIndex = columnModel.getKeyColumnIndex();
        return mapper(origIndex, origIndex.isEmpty() || origIndex.get(keyColumnIndex) == keyColumnIndex);
    }

    protected static RowInRecordMapper mapper(List<Integer> origIndex, boolean keyColumnPreserved) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 7653347685611673401L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                List<Cell> newCells = origIndex.stream()
                        .map(i -> row.getCell(i))
                        .collect(Collectors.toList());
                return new Row(newCells);
            }

            @Override
            public boolean preservesRecordStructure() {
                return keyColumnPreserved;
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
