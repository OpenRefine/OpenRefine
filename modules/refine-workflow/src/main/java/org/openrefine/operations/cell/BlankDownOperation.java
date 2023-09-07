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

import java.io.Serializable;
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
 * Transforms a table without a record structure to blanking out values which are identical to those on the previous
 * row, creating a record structure.
 */
public class BlankDownOperation extends EngineDependentOperation {

    protected String _columnName;

    @JsonCreator
    public BlankDownOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName) {
        super(engineConfig);
        _columnName = columnName;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        ColumnModel model = projectState.getColumnModel();
        int index = model.getRequiredColumnIndex(_columnName);
        Engine engine = getEngine(projectState, context.getProjectId());
        boolean recordsPreserved = index != model.getKeyColumnIndex();
        Grid result;
        if (Mode.RecordBased.equals(_engineConfig.getMode())) {
            // Simple map of records
            result = projectState.mapRecords(
                    RecordMapper.conditionalMapper(engine.combinedRecordFilters(), recordMapper(index), RecordMapper.IDENTITY),
                    model);

        } else {
            // We need to remember the cell from the previous row, so we use a scan map
            result = projectState.mapRows(
                    RowScanMapper.conditionalMapper(engine.combinedRowFilters(), rowScanMapper(index, model.getKeyColumnIndex()),
                            RowMapper.IDENTITY),
                    model);
        }
        // if we are blanking down the key column, this will likely create records
        if (index == model.getKeyColumnIndex() && !model.hasRecords()) {
            result = result.withColumnModel(result.getColumnModel().withHasRecords(true));
        }

        return new ChangeResult(
                result,
                recordsPreserved ? GridPreservation.PRESERVES_RECORDS : GridPreservation.PRESERVES_ROWS);
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @Override
    public String getDescription() {
        return "Blank down cells in column " + _columnName;
    }

    protected static RecordMapper recordMapper(int columnIndex) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5754924505312738966L;

            @Override
            public List<Row> call(Record record) {
                Cell lastCell = null;
                List<Row> result = new LinkedList<>();
                for (Row row : record.getRows()) {
                    Serializable cellValue = row.getCellValue(columnIndex);
                    if (lastCell != null
                            && ExpressionUtils.isNonBlankData(cellValue)
                            && cellValue.equals(lastCell.getValue())) {
                        result.add(row.withCell(columnIndex, null));
                    } else {
                        result.add(row);
                    }
                    lastCell = row.getCell(columnIndex);
                }
                return result;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true;
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
                if (right != null && right.value == null && !right.isPending()) {
                    // Cell.NULL is used as sentinel, for rows that are skipped by facets.
                    // null cells are simply represented by right == null
                    return left;
                } else {
                    return right;
                }
            }

            @Override
            public Cell unit() {
                return Cell.NULL;
            }

            @Override
            public Row map(Cell lastCell, long rowId, Row row) {
                Serializable cellValue = row.getCellValue(columnIndex);
                if (lastCell != null & lastCell.isPending()) {
                    return row.withCell(columnIndex, Cell.PENDING_NULL);
                } else if (ExpressionUtils.isNonBlankData(cellValue)
                        && lastCell != null
                        && cellValue.equals(lastCell.getValue())) {
                    return row.withCell(columnIndex, null);
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
