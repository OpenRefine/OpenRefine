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

package org.openrefine.operations.cell;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.RowScanMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.exceptions.OperationException;

/**
 * Transforms a table with a record structure to by spreading non-null values in the rows below, in a specific column.
 */
public class FillDownOperation extends EngineDependentOperation {

    protected String _columnName;

    @JsonCreator
    public FillDownOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName) {
        super(engineConfig);
        _columnName = columnName;
    }

    @Override
    public ChangeResult apply(Grid state, ChangeContext context) throws OperationException {
        ColumnModel model = state.getColumnModel();
        int index = model.getRequiredColumnIndex(_columnName);
        Engine engine = getEngine(state, context.getProjectId());
        boolean recordsPreserved = index != model.getKeyColumnIndex();
        Grid result;
        if (Mode.RecordBased.equals(_engineConfig.getMode())) {
            // simple map of records, since a filled down value cannot spread beyond a record boundary
            result = state.mapRecords(
                    RecordMapper.conditionalMapper(engine.combinedRecordFilters(), recordMapper(index, model.getKeyColumnIndex()),
                            RecordMapper.IDENTITY),
                    model);
        } else {
            // scan map, because we need to remember the last non-null cell
            result = state.mapRows(
                    RowScanMapper.conditionalMapper(engine.combinedRowFilters(), rowScanMapper(index, model.getKeyColumnIndex()),
                            RowMapper.IDENTITY),
                    model);
        }

        // if we are filling down the first column, that is removing the records structure
        if (!recordsPreserved && model.hasRecords()) {
            result = result.withColumnModel(result.getColumnModel().withHasRecords(false));
        }
        return new ChangeResult(result, recordsPreserved ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
    public String getDescription() {
        return "Fill down cells in column " + _columnName;
    }

    protected static RecordMapper recordMapper(int columnIndex, int keyColumnIndex) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5754924505312738966L;

            @Override
            public List<Row> call(Record record) {
                Cell lastNonBlankCell = null;
                List<Row> result = new LinkedList<>();
                for (Row row : record.getRows()) {
                    if (row.isCellBlank(columnIndex)) {
                        result.add(row.withCell(columnIndex, lastNonBlankCell));
                    } else {
                        lastNonBlankCell = row.getCell(columnIndex);
                        result.add(row);
                    }
                }
                return result;
            }

            @Override
            public boolean preservesRowCount() {
                return true;
            }

            @Override
            public boolean preservesRecordStructure() {
                return columnIndex != keyColumnIndex;
            }

        };
    }

    protected static RowScanMapper<Cell> rowScanMapper(int columnIndex, int keyColumnIndex) {
        return new RowScanMapper<Cell>() {

            private static final long serialVersionUID = 2808768242505893380L;

            @Override
            public Cell feed(long rowId, Row row) {
                return row.getCell(columnIndex);
            }

            @Override
            public Cell combine(Cell left, Cell right) {
                if (right != null && (ExpressionUtils.isNonBlankData(right.getValue()) || right.isPending())) {
                    return right;
                } else {
                    return left;
                }
            }

            @Override
            public Cell unit() {
                return Cell.NULL;
            }

            @Override
            public Row map(Cell lastNonBlankCell, long rowId, Row row) {
                if (row.isCellBlank(columnIndex)) {
                    return row.withCell(columnIndex, lastNonBlankCell);
                } else {
                    return row;
                }
            }

            @Override
            public boolean preservesRecordStructure() {
                return keyColumnIndex != columnIndex;
            }

        };
    }
}
