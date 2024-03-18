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

package org.openrefine.operations.recon;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.operations.OperationDescription;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

public class ReconClearSimilarCellsOperation extends RowMapOperation {

    final protected String _similarValue;
    final protected String _columnName;

    @JsonCreator
    public ReconClearSimilarCellsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("similarValue") String similarValue) {
        super(engineConfig);
        _similarValue = similarValue;
        _columnName = columnName;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("similarValue")
    public String getSimilarValue() {
        return _similarValue;
    }

    @Override
    public String getDescription() {
        return OperationDescription.recon_clear_similar_cells_brief(_similarValue, _columnName);
    }
    
    @Override
    public List<String> getColumnDependencies() {
        return Collections.singletonList(_columnName);
    }

    @Override
    public List<ColumnInsertion> getColumnInsertions() {
        return Collections.singletonList(ColumnInsertion.replacement(_columnName));
    }

    
    @Override
    protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        int cellIndex = columnModel.getRequiredColumnIndex(_columnName);
        return rowMapper(cellIndex, _similarValue);
    }

    protected static RowInRecordMapper rowMapper(int cellIndex, String _similarValue) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -7567386480566899008L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null && cell.recon != null) {
                    String value = cell.value instanceof String ? ((String) cell.value) : cell.value.toString();

                    if (_similarValue.equals(value)) {
                        Cell newCell = new Cell(cell.value, null, cell.isPending());
                        return new Row(Collections.singletonList(newCell));
                    }
                }
                return new Row(Collections.singletonList(cell));
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // blank cells remain blank, non-blank ones remain non-blank
            }

        };
    }
}
