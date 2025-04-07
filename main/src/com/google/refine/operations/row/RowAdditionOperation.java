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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.ColumnsDiff;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.RowAdditionChange;
import com.google.refine.operations.OperationDescription;

public class RowAdditionOperation extends AbstractOperation {

    final private List<Row> _rows;
    final private int _insertionIndex;

    public RowAdditionOperation(
            List<Row> rows,
            int insertionIndex) {
        _rows = rows;
        _insertionIndex = insertionIndex;
    }

    /**
     * Deserialization constructor to provide compatibility for the legacy serialization format. In this format, only
     * the number of rows is relevant: the contents of the rows must be ignored, because they might have been corrupted
     * due to mutability issues in https://github.com/OpenRefine/OpenRefine/issues/7245.
     * 
     * @param addedRows
     *            the rows to add to the project
     * @param rows
     *            a legacy serialization field, whose length is is the only thing that matters. If provided, it will be
     *            converted to a list of empty rows of the same size.
     * @param insertionIndex
     *            the place in the grid where to insert this list.
     * @deprecated should not be called directly, is only provided for JSON deserialization.
     */
    @Deprecated
    @JsonCreator
    public RowAdditionOperation(
            @JsonProperty("addedRows") List<Row> addedRows,
            @JsonProperty("rows") List<Object> rows,
            @JsonProperty("insertionIndex") int insertionIndex) {
        _rows = addedRows != null ? addedRows
                : (rows == null ? List.of() : rows.stream().map(r -> new Row(0)).collect(Collectors.toList()));
        _insertionIndex = insertionIndex;
    }

    @JsonProperty("addedRows")
    public List<Row> getRows() {
        return _rows;
    }

    @JsonProperty("index")
    public int getInsertionIndex() {
        return _insertionIndex;
    }

    @Override
    protected String getBriefDescription(Project project) {
        return OperationDescription.row_addition_brief();
    }

    @Override
    public Optional<Set<String>> getColumnDependencies() {
        return Optional.of(Set.of());
    }

    @JsonIgnore
    public Optional<ColumnsDiff> getColumnsDiff() {
        return Optional.of(ColumnsDiff.empty());
    }

    @Override
    public RowAdditionOperation renameColumns(Map<String, String> newColumnNames) {
        return this;
    }

    @Override
    protected HistoryEntry createHistoryEntry(Project project, long historyEntryID) throws Exception {

        int count = _rows.size();
        String description = "Add " + count + " row" + ((count > 1) ? "s" : "");

        return new HistoryEntry(
                historyEntryID,
                project,
                description,
                this,
                new RowAdditionChange(_rows, _insertionIndex));
    }

}
