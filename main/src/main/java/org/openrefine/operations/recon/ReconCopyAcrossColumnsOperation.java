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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.Change.DoesNotApplyException;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.operations.ImmediateRowMapOperation;

public class ReconCopyAcrossColumnsOperation extends ImmediateRowMapOperation {

    final protected String _fromColumnName;
    final protected List<String> _toColumnNames;
    final protected List<Judgment> _judgments;
    final protected boolean _applyToJudgedCells;

    @JsonCreator
    public ReconCopyAcrossColumnsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("fromColumnName") String fromColumnName,
            @JsonProperty("toColumnNames") List<String> toColumnNames,
            @JsonProperty("judgments") List<Recon.Judgment> judgments,
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
    public List<String> getToColumnNames() {
        return _toColumnNames;
    }

    @JsonProperty("judgments")
    public List<Judgment> getJudgments() {
        return _judgments;
    }

    @JsonProperty("applyToJudgedCells")
    public boolean getApplyToJudgedCells() {
        return _applyToJudgedCells;
    }

    @Override
    public RowMapper getPositiveRowMapper(GridState projectState, ChangeContext context) throws DoesNotApplyException {
        int columnIndex = projectState.getColumnModel().getColumnIndexByName(_fromColumnName);
        List<Integer> targetColumnIndices = _toColumnNames
                .stream()
                .map(name -> projectState.getColumnModel().getColumnIndexByName(name))
                .collect(Collectors.toList());
        if (columnIndex == -1) {
            throw new DoesNotApplyException(String.format("Column '%s' does not exist", _fromColumnName));
        }
        for (Integer targetColumnIndex : targetColumnIndices) {
            if (targetColumnIndex == -1) {
                throw new DoesNotApplyException(String.format("Target column does not exist"));
            }
        }
        Set<Judgment> judgments = new HashSet<>(_judgments);

        // Aggregate the map from cell values to recons
        Map<Serializable, Recon> cellValueToRecon = projectState.aggregateRows(getAggregator(columnIndex, judgments),
                ImmutableMap.<Serializable, Recon> of());

        // TODO update the ReconStats of the target columns?

        // Apply the map in the target columns
        return getRowMapper(targetColumnIndices, cellValueToRecon, _applyToJudgedCells);
    }

    /*
     * There could potentially be a suitable type of immutable map for this, which would support merges too.
     */
    protected static RowAggregator<ImmutableMap<Serializable, Recon>> getAggregator(int cellIndex, Set<Recon.Judgment> judgments) {
        return new RowAggregator<ImmutableMap<Serializable, Recon>>() {

            private static final long serialVersionUID = 1509253451942680839L;

            @Override
            public ImmutableMap<Serializable, Recon> sum(ImmutableMap<Serializable, Recon> first,
                    ImmutableMap<Serializable, Recon> second) {
                return ImmutableMap.<Serializable, Recon> builder().putAll(first).putAll(second).build();
            }

            @Override
            public ImmutableMap<Serializable, Recon> withRow(ImmutableMap<Serializable, Recon> state, long rowId, Row row) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null && cell.value != null && cell.recon != null) {
                    if (judgments.contains(cell.recon.judgment)) {
                        return ImmutableMap.<Serializable, Recon> builder().putAll(state).put(cell.value, cell.recon).build();
                    }
                }
                return state;
            }

        };
    }

    protected static RowMapper getRowMapper(List<Integer> columnIndices, Map<Serializable, Recon> valueToRecon,
            boolean applyToJudgedCells) {
        return new RowMapper() {

            private static final long serialVersionUID = -6427575427288421446L;

            @Override
            public Row call(long rowId, Row row) {
                Row result = row;
                for (Integer columnIndex : columnIndices) {
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null && cell.value != null) {
                        Recon reconToCopy = valueToRecon.get(cell.value);
                        boolean judged = cell.recon != null && cell.recon.judgment != Judgment.None;

                        if (reconToCopy != null && (!judged || applyToJudgedCells)) {
                            Cell newCell = new Cell(cell.value, reconToCopy);
                            result = row.withCell(columnIndex, newCell);
                        }
                    }
                }
                return result;
            }

        };
    }

    @Override
    public String getDescription() {
        return String.format("Copy recon judgments from column %s to %s", _fromColumnName, StringUtils.join(_toColumnNames));
    }
}
