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
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Grid;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ColumnNotFoundException;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.Operation;
import org.openrefine.operations.Operation.DoesNotApplyException;
import org.openrefine.operations.OperationDescription;
import org.openrefine.operations.RowMapOperation;

/**
 * Marks all filtered cells in a given column as reconciled to "new". Similar values can either be matched to the same
 * reconciliation id, or distinct ones.
 */
public class ReconMarkNewTopicsOperation extends RowMapOperation {

    final protected boolean _shareNewTopics;
    final protected String _columnName;
    final protected String _service;
    final protected String _identifierSpace;
    final protected String _schemaSpace;

    @JsonCreator
    public ReconMarkNewTopicsOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("shareNewTopics") boolean shareNewTopics,
            @JsonProperty("service") String service,
            @JsonProperty("identifierSpace") String identifierSpace,
            @JsonProperty("schemaSpace") String schemaSpace) {
        super(engineConfig);
        _columnName = columnName;
        _shareNewTopics = shareNewTopics;
        _service = service;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    @JsonProperty("shareNewTopics")
    public boolean getShareNewTopics() {
        return _shareNewTopics;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("service")
    public String getService() {
        return _service;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("identifierSpace")
    public String getIdentifierSpace() {
        return _identifierSpace;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("schemaSpace")
    public String getSchemaSpace() {
        return _schemaSpace;
    }

    @Override
    public String getDescription() {
        return _shareNewTopics ? OperationDescription.recon_mark_new_topics_shared_brief(_columnName)
                : OperationDescription.recon_mark_new_topics_brief(_columnName);
    }

    protected ReconConfig getNewReconConfig(ColumnMetadata column) {
        return column.getReconConfig() != null ? column.getReconConfig()
                : new StandardReconConfig(
                        _service,
                        _identifierSpace,
                        _schemaSpace,
                        null,
                        false,
                        Collections.emptyList(),
                        0);
    }

    @Override
    public RowInRecordMapper getPositiveRowMapper(Grid state, ChangeContext context) throws Operation.DoesNotApplyException {
        int columnIndex = state.getColumnModel().getColumnIndexByName(_columnName);
        if (columnIndex == -1) {
            throw new ColumnNotFoundException(_columnName);
        }
        ReconConfig reconConfig = getNewReconConfig(state.getColumnModel().getColumnByName(_columnName));
        long historyEntryId = context.getHistoryEntryId();

        if (_shareNewTopics) {
            // Aggregate the set of distinct values
            ImmutableMap<String, Long> empty = ImmutableMap.of();
            RowFilter filter = getEngine(state, context.getProjectId()).combinedRowFilters();
            ImmutableMap<String, Long> valueToId = state.aggregateRows(aggregator(columnIndex, filter), empty);

            return rowMapperWithSharing(columnIndex, reconConfig, historyEntryId, valueToId);
        } else {
            return rowMapperNoSharing(columnIndex, reconConfig, historyEntryId);
        }
    }

    protected static RowInRecordMapper rowMapperWithSharing(int columnIndex, ReconConfig reconConfig, long historyEntryId,
            ImmutableMap<String, Long> valueToId) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -2838679493823196821L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value) && !cell.isPending()) {
                    Recon recon = reconConfig.createNewRecon(historyEntryId)
                            .withJudgment(Judgment.New)
                            .withJudgmentAction("mass");
                    String s = cell.value == null ? "" : cell.value.toString();
                    if (valueToId.containsKey(s)) {
                        recon = recon.withId(valueToId.get(s));
                    }

                    Cell newCell = new Cell(cell.value, recon);

                    return row.withCell(columnIndex, newCell);
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // cells remain blank or non-blank after this
            }

        };
    }

    protected static RowInRecordMapper rowMapperNoSharing(int columnIndex, ReconConfig reconConfig, long historyEntryId) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = 5224856110246957223L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                    Recon recon = cell.recon == null ? reconConfig.createNewRecon(historyEntryId) : cell.recon.dup(historyEntryId);
                    recon = recon
                            .withMatch(null)
                            .withMatchRank(-1)
                            .withJudgment(Judgment.New)
                            .withJudgmentAction("mass");

                    Cell newCell = new Cell(cell.value, recon);

                    return row.withCell(columnIndex, newCell);
                }
                return row;
            }

            @Override
            public boolean preservesRecordStructure() {
                return true; // cells remain blank or non-blank after this operation
            }

        };
    }

    protected static RowAggregator<ImmutableMap<String, Long>> aggregator(int columnIndex, RowFilter filter) {
        return new RowAggregator<ImmutableMap<String, Long>>() {

            private static final long serialVersionUID = 2749743046303701107L;

            @Override
            public ImmutableMap<String, Long> sum(ImmutableMap<String, Long> first, ImmutableMap<String, Long> second) {
                Builder<String, Long> builder = ImmutableMap.<String, Long> builder().putAll(first);
                // sadly we cannot call `putAll(second)` as conflicting keys will raise an exception
                for (Entry<String, Long> entry : second.entrySet()) {
                    if (!first.containsKey(entry.getKey())) {
                        builder.put(entry.getKey(), entry.getValue());
                    }
                }
                return builder.build();
            }

            @Override
            public ImmutableMap<String, Long> withRow(ImmutableMap<String, Long> state, long rowId, Row row) {
                if (!filter.filterRow(rowId, row)) {
                    return state;
                }
                Cell cell = row.getCell(columnIndex);
                if (cell != null && cell.value != null) {
                    String value = cell.value.toString();
                    if (!state.containsKey(value)) {
                        long reconId = new Recon(0L, "", "").id;
                        return ImmutableMap.<String, Long> builder().putAll(state).put(value, reconId).build();
                    }
                }
                return state;
            }

        };
    }
}
