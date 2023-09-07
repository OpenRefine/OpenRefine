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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.GridPreservation;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.Operation;
import org.openrefine.operations.exceptions.OperationException;

/**
 * Within a record, joins the non-blank cells of a column into the first cell, with the specified separator. The
 * keyColumnName can be used to specify which column should be treated as record key (although this parameter has never
 * been exposed in the UI as of 2020-05).
 * 
 * @author Antonin Delpeuch
 *
 */
public class MultiValuedCellJoinOperation implements Operation {

    final protected String _columnName;
    final protected String _keyColumnName;
    final protected String _separator;

    @JsonCreator
    public MultiValuedCellJoinOperation(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("separator") String separator) {
        _columnName = columnName;
        _keyColumnName = keyColumnName;
        _separator = separator;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        ColumnModel columnModel = projectState.getColumnModel();
        int columnIdx = columnModel.getRequiredColumnIndex(_columnName);
        int keyColumnIdx = _keyColumnName == null ? 0 : columnModel.getRequiredColumnIndex(_keyColumnName);
        if (keyColumnIdx != columnModel.getKeyColumnIndex()) {
            projectState = projectState.withColumnModel(columnModel.withKeyColumnIndex(keyColumnIdx));
        }
        return new ChangeResult(
                projectState.mapRecords(
                        recordMapper(columnIdx, _separator),
                        columnModel),
                GridPreservation.NO_ROW_PRESERVATION);
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return _keyColumnName;
    }

    @JsonProperty("separator")
    public String getSeparator() {
        return _separator;
    }

    @Override
    public String getDescription() {
        return "Join multi-valued cells in column " + _columnName;
    }

    protected static RecordMapper recordMapper(int columnIdx, String separator) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5684754503934565526L;

            @Override
            public List<Row> call(Record record) {
                List<Row> rows = record.getRows();

                // Join the non-blank cell values
                StringBuffer sb = new StringBuffer();
                boolean pendingCellFound = false;
                for (int i = 0; i != rows.size() && !pendingCellFound; i++) {
                    Object value = rows.get(i).getCellValue(columnIdx);
                    if (rows.get(i).isCellPending(columnIdx)) {
                        pendingCellFound = true;

                    } else if (ExpressionUtils.isNonBlankData(value)) {
                        if (sb.length() > 0) {
                            sb.append(separator);
                        }
                        sb.append(value.toString());
                    }
                }

                // Compute the new rows
                List<Row> newRows = new ArrayList<>(rows.size());
                String joined = pendingCellFound ? "" : sb.toString();
                newRows.add(rows.get(0).withCell(columnIdx, new Cell(joined.isEmpty() ? null : joined, null, pendingCellFound)));
                for (int i = 1; i < rows.size(); i++) {
                    Row row = rows.get(i).withCell(columnIdx, null);
                    // Only add rows if they are not entirely blank after removing the joined value
                    if (row.getCells().stream().anyMatch(c -> c != null && ExpressionUtils.isNonBlankData(c.getValue()))) {
                        newRows.add(row);
                    }
                }

                return newRows;
            }

            @Override
            public boolean preservesRecordStructure() {
                return false;
            }

        };
    }

}
