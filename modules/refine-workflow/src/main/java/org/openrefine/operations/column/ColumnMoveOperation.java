/*

Copyright 2010,2012. Google Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

/**
 * Moves a column to a different position. The new position of the column can either be specified by giving the name of
 * the column to insert it after, or by an explicit insertion index. The latter is preserved for backwards-compatibility
 * purposes, to be able to interpret operations executed up to OpenRefine 3.x, but is discouraged because it does not
 * generalize as well.
 */
public class ColumnMoveOperation extends RowMapOperation {

    final protected String _columnName;
    final protected Integer _index;
    final protected String _afterColumn;

    @JsonCreator
    public ColumnMoveOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("index") Integer index,
            @JsonProperty("afterColumn") String afterColumn) {
        super(EngineConfig.ALL_ROWS);
        _columnName = columnName;
        _index = index != null && index == 0 ? null : index;
        _afterColumn = afterColumn;
        if (_index != null && _afterColumn != null) {
            throw new IllegalArgumentException("At most one of 'index' and 'afterColumn' must be provided");
        }
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("index")
    @JsonInclude(Include.NON_NULL)
    public Integer getIndex() {
        return _index;
    }

    @JsonProperty("afterColumn")
    @JsonInclude(Include.NON_NULL)
    public String getAfterColumn() {
        return _afterColumn;
    }

    @Override
    public String getDescription() {
        if (_index != null) {
            return "Move column \"" + _columnName + "\" to position " + _index;
        } else if (_afterColumn != null) {
            return "Move column \"" + _columnName + "\" to after column \"" + _afterColumn + "\"";
        } else {
            return "Move column \"" + _columnName + "\" to first position";
        }
    }

    @Override
    public List<ColumnInsertion> getColumnInsertions() {
        if (_index == null) {
            return Collections.singletonList(
                    ColumnInsertion.builder()
                            .withName(_columnName)
                            .withCopiedFrom(_columnName)
                            .withInsertAt(_afterColumn)
                            .build());
        } else {
            return super.getColumnInsertions();
        }
    }

    @Override
    public Set<String> getColumnDeletions() {
        if (_index == null) {
            return Collections.singleton(_columnName);
        } else {
            return super.getColumnDeletions();
        }
    }

    @Override
    public ColumnModel getNewColumnModel(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, ChangeContext context)
            throws OperationException {
        if (_index == null) {
            return super.getNewColumnModel(columnModel, overlayModels, context);
        } else {
            int fromIndex = columnModel.getRequiredColumnIndex(_columnName);
            ColumnMetadata column = columnModel.getColumns().get(fromIndex);
            return columnModel.removeColumn(fromIndex).insertUnduplicatedColumn(_index, column);
        }
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels, long estimatedRowCount,
            ChangeContext context)
            throws OperationException {
        if (_index == null) {
            return super.getPositiveRowMapper(columnModel, overlayModels, estimatedRowCount, context);
        } else {
            int fromIndex = columnModel.getRequiredColumnIndex(_columnName);
            return mapper(fromIndex, _index, columnModel.getKeyColumnIndex());
        }
    }

    protected static RowInRecordMapper mapper(int fromIndex, int toIndex, int keyColumnIndex) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 1L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                List<Cell> cells = row.getCells();
                List<Cell> newCells = new ArrayList<>(cells.size());
                if (fromIndex <= toIndex) {
                    newCells.addAll(cells.subList(0, fromIndex));
                    newCells.addAll(cells.subList(fromIndex + 1, toIndex + 1));
                    newCells.add(cells.get(fromIndex));
                    newCells.addAll(cells.subList(toIndex + 1, cells.size()));
                } else {
                    newCells.addAll(cells.subList(0, toIndex));
                    newCells.add(cells.get(fromIndex));
                    newCells.addAll(cells.subList(toIndex, fromIndex));
                    newCells.addAll(cells.subList(fromIndex + 1, cells.size()));
                }
                return new Row(newCells);
            }

            @Override
            public boolean preservesRecordStructure() {
                // TODO: we should adjust the key column index in the resulting grid
                // if it was affected by the move. To be added if we add support for moving
                // the key column index.
                if (fromIndex <= toIndex) {
                    return keyColumnIndex < fromIndex || keyColumnIndex > toIndex;
                } else {
                    return keyColumnIndex < toIndex || keyColumnIndex > fromIndex;
                }
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
