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

package com.google.refine.operations.cell;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine.Mode;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.operations.EngineDependentMassCellOperation;

public class BlankDownOperation extends EngineDependentMassCellOperation {
    
    @JsonCreator
    public BlankDownOperation(
            @JsonProperty("engineConfig")
            EngineConfig engineConfig,
            @JsonProperty("columnName")
            String columnName
        ) {
        super(engineConfig, columnName, true);
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Blank down cells in column " + _columnName;
    }

    @Override
    protected String createDescription(Column column,
            List<CellChange> cellChanges) {
        
        return "Blank down " + cellChanges.size() + 
            " cells in column " + column.getName();
    }

    @Override
    protected RowVisitor createRowVisitor(Project project, List<CellChange> cellChanges, long historyEntryID) throws Exception {
        Column column = project.columnModel.getColumnByName(_columnName);
        Mode engineMode = createEngine(project).getMode();
        
        return new RowVisitor() {
            int                 cellIndex;
            int 			    keyCellIndex;
            List<CellChange>    cellChanges;
            Cell                previousCell;
            Mode                engineMode;
            
            public RowVisitor init(int cellIndex, List<CellChange> cellChanges, Mode engineMode) {
                this.cellIndex = cellIndex;
                this.cellChanges = cellChanges;
                this.engineMode = engineMode;
                return this;
            }

            @Override
            public void start(Project project) {
            	keyCellIndex = project.columnModel.columns.get(
                		project.columnModel.getKeyColumnIndex()).getCellIndex();
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                if (engineMode.equals(Mode.RecordBased) && ExpressionUtils.isNonBlankData(row.getCellValue(keyCellIndex))) {
                    previousCell = null;
                }
                Object value = row.getCellValue(cellIndex);
                if (ExpressionUtils.isNonBlankData(value)) {
                    Cell cell = row.getCell(cellIndex);
                    if (previousCell != null && cell.value.equals(previousCell.value)) {
                        CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, null);
                        cellChanges.add(cellChange);
                    }
                    previousCell = cell;
                } else {
                    previousCell = null;
                }
                return false;
            }
        }.init(column.getCellIndex(), cellChanges, engineMode);
    }
}
