/*******************************************************************************
 * Copyright (C) 2024, OpenRefine contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.operations.row;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.RowRemovalChange;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationDescription;

public class RowDuplicatesRemovalOperation extends EngineDependentOperation {

    final protected List<String> _criteria;
    final List<Column> criteriaColumns = new ArrayList<Column>();

    @JsonCreator
    public RowDuplicatesRemovalOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("criteria") List<String> criteria) {
        super(engineConfig);
        _criteria = criteria;
    }

    @JsonProperty("criteria")
    public List<String> getRows() {
        return _criteria;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.row_remove_duplicates_brief();
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {
        Engine engine = createEngine(project);

        List<Integer> rowIndices = new ArrayList<Integer>();
        for (String c : _criteria) {
            Column toColumn = project.columnModel.getColumnByName(c);
            if (toColumn != null) {
                criteriaColumns.add(toColumn);
            }
        }

        FilteredRows filteredRows = engine.getAllFilteredRows();
        filteredRows.accept(project, createRowVisitor(project, rowIndices));

        return new HistoryEntry(
                historyEntryID,
                project,
                "Remove " + rowIndices.size() + " rows",
                this,
                new RowRemovalChange(rowIndices));
    }

    protected RowVisitor createRowVisitor(Project project, List<Integer> rowIndices) throws Exception {
        return new RowVisitor() {

            List<Integer> rowIndices;
            HashSet<Object> rowUniqueKeys = new HashSet<>();

            public RowVisitor init(List<Integer> rowIndices) {
                this.rowIndices = rowIndices;
                return this;
            }

            @Override
            public void start(Project project) {
                // nothing to do
            }

            @Override
            public void end(Project project) {
                // nothing to do
            }

            private boolean isDuplicate(Row row) {
                List<String> key = criteriaColumns.stream()
                        .map(col -> normalizeValue(row.getCell(col.getCellIndex())))
                        .collect(Collectors.toList());
                int keyHash = key.hashCode();

                if (!rowUniqueKeys.add(keyHash)) {
                    return true;
                }
                return false;
            }

            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                if (isDuplicate(row)) {
                    rowIndices.add(rowIndex);
                }
                return false;
            }
        }.init(rowIndices);
    }

    private static String normalizeValue(Object value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (value == null) {
            return "";
        } else if (value instanceof Date) {
            return dateFormat.format((Date) value);
        }
        return value.toString();
    }

}
