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

package com.google.refine.operations.recon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.refine.operations.OperationDescription;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.MassChange;
import com.google.refine.operations.EngineDependentOperation;

public class ReconCopyAcrossColumnsOperation extends EngineDependentOperation {

    final protected String _fromColumnName;
    final protected String[] _toColumnNames;
    final protected String[] _judgments;
    final protected boolean _applyToJudgedCells;

    @JsonCreator
    public ReconCopyAcrossColumnsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("fromColumnName") String fromColumnName,
            @JsonProperty("toColumnNames") String[] toColumnNames,
            @JsonProperty("judgments") String[] judgments,
            @JsonProperty("applyToJudgedCells") boolean applyToJudgedCells) {
        super(engineConfig);
        _fromColumnName = fromColumnName;
        _toColumnNames = toColumnNames;
        _judgments = judgments;
        _applyToJudgedCells = applyToJudgedCells;
    }

    @JsonProperty("fromColumnName")
    public String getFromColumnName() {
        return _fromColumnName;
    }

    @JsonProperty("toColumnNames")
    public String[] getToColumnNames() {
        return _toColumnNames;
    }

    @JsonProperty("judgments")
    public String[] getJudgments() {
        return _judgments;
    }

    @JsonProperty("applyToJudgedCells")
    public boolean getApplyToJudgedCells() {
        return _applyToJudgedCells;
    }

    @Override
    protected HistoryEntry createHistoryEntry(final Project project, final long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        final Column fromColumn = project.columnModel.getColumnByName(_fromColumnName);

        final List<Column> toColumns = new ArrayList<Column>(_toColumnNames.length);
        for (String c : _toColumnNames) {
            Column toColumn = project.columnModel.getColumnByName(c);
            if (toColumn != null) {
                toColumns.add(toColumn);
            }
        }

        final Set<Recon.Judgment> judgments = new HashSet<Recon.Judgment>(_judgments.length);
        for (String j : _judgments) {
            judgments.add(Recon.stringToJudgment(j));
        }

        final List<CellChange> cellChanges = new ArrayList<CellChange>(project.rows.size());

        if (fromColumn != null && toColumns.size() > 0) {
            final Map<Object, Recon> cellValueToRecon = new HashMap<Object, Recon>();

            FilteredRows filteredRows = engine.getAllFilteredRows();
            try {
                filteredRows.accept(project, new RowVisitor() {

                    @Override
                    public void start(Project project) {
                        // nothing to do
                    }

                    @Override
                    public void end(Project project) {
                        // nothing to do
                    }

                    @Override
                    public boolean visit(Project project, int rowIndex, Row row) {
                        Cell cell = row.getCell(fromColumn.getCellIndex());
                        if (cell != null && cell.value != null && cell.recon != null) {
                            if (judgments.contains(cell.recon.judgment)) {
                                cellValueToRecon.put(cell.value, cell.recon);
                            }
                        }
                        return false;
                    }
                });

                filteredRows.accept(project, new RowVisitor() {

                    @Override
                    public void start(Project project) {
                        // nothing to do
                    }

                    @Override
                    public void end(Project project) {
                        // nothing to do
                    }

                    @Override
                    public boolean visit(Project project, int rowIndex, Row row) {
                        for (Column column : toColumns) {
                            int cellIndex = column.getCellIndex();
                            Cell cell = row.getCell(cellIndex);
                            if (cell != null && cell.value != null) {
                                Recon reconToCopy = cellValueToRecon.get(cell.value);
                                boolean judged = cell.recon != null && cell.recon.judgment != Judgment.None;

                                if (reconToCopy != null && (!judged || _applyToJudgedCells)) {
                                    Cell newCell = new Cell(cell.value, reconToCopy);
                                    CellChange cellChange = new CellChange(rowIndex, cellIndex, cell, newCell);
                                    cellChanges.add(cellChange);
                                }
                            }
                        }
                        return false;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String description = OperationDescription.recon_copy_across_columns_desc(cellChanges.size(), _fromColumnName,
                StringUtils.join(_toColumnNames));
        return new HistoryEntry(
                historyEntryID, project, description, this, new MassChange(cellChanges, false));
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.recon_copy_across_columns_brief(_fromColumnName, StringUtils.join(_toColumnNames));
    }
}
