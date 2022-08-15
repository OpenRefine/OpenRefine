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

package com.google.refine.operations.row;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.RecordModel.CellDependency;
import com.google.refine.model.RecordModel.RowDependency;
import com.google.refine.model.Row;
import com.google.refine.model.changes.MassRowChange;
import com.google.refine.operations.OperationDescription;

public class DenormalizeOperation extends AbstractOperation {

    @JsonCreator
    public DenormalizeOperation() {
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.row_denormalize_brief();
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        List<Row> newRows = new ArrayList<Row>();

        List<Row> oldRows = project.rows;
        for (int r = 0; r < oldRows.size(); r++) {
            Row oldRow = oldRows.get(r);
            Row newRow = null;

            RowDependency rd = project.recordModel.getRowDependency(r);
            if (rd.cellDependencies != null) {
                newRow = oldRow.dup();

                for (CellDependency cd : rd.cellDependencies) {
                    if (cd != null) {
                        int contextRowIndex = cd.rowIndex;
                        int contextCellIndex = cd.cellIndex;

                        if (contextRowIndex >= 0 && contextRowIndex < oldRows.size()) {
                            Row contextRow = oldRows.get(contextRowIndex);
                            Cell contextCell = contextRow.getCell(contextCellIndex);

                            newRow.setCell(contextCellIndex, contextCell);
                        }
                    }
                }
            }

            newRows.add(newRow != null ? newRow : oldRow);
        }

        return new HistoryEntry(
                historyEntryID,
                project,
                getBriefDescription(project),
                DenormalizeOperation.this,
                new MassRowChange(newRows));
    }
}
