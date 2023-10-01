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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.browsing.facets.RangeFacet;
import org.openrefine.history.GridPreservation;
import org.openrefine.messages.OpenRefineMessage;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Grid;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.CellChangeDataSerializer;
import org.openrefine.model.changes.CellListChangeDataSerializer;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.changes.RowInRecordChangeDataJoiner;
import org.openrefine.model.changes.RowInRecordChangeDataProducer;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.operations.ChangeResult;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.exceptions.OperationException;

/**
 * Runs reconciliation on a column.
 * <p>
 * TODO restore records mode
 */
public class ReconOperation extends EngineDependentOperation {

    final static Logger logger = LoggerFactory.getLogger("recon-operation");

    final protected String _columnName;
    final protected ReconConfig _reconConfig;
    final protected String _changeDataId = "recon";

    @JsonCreator
    public ReconOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("columnName") String columnName,
            @JsonProperty("config") ReconConfig reconConfig) {
        super(engineConfig);
        _columnName = columnName;
        _reconConfig = reconConfig;
    }

    @Override
    public ChangeResult apply(Grid projectState, ChangeContext context) throws OperationException {
        ColumnModel columnModel = projectState.getColumnModel();
        int baseColumnIndex = columnModel.getRequiredColumnIndex(_columnName);
        ColumnModel newColumnModel = columnModel
                .withReconConfig(baseColumnIndex, _reconConfig);

        Joiner joiner = new Joiner(baseColumnIndex);

        Grid joined;
        Engine engine = new Engine(projectState, _engineConfig, context.getProjectId());
        long rowCount = projectState.rowCount();
        ReconChangeDataProducer producer = new ReconChangeDataProducer(_columnName, baseColumnIndex, _reconConfig,
                context.getHistoryEntryId(), rowCount, columnModel);
        if (Engine.Mode.RowBased.equals(_engineConfig.getMode())) {
            ChangeData<Cell> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellChangeDataSerializer(),
                        (grid, partialChangeData) -> {
                            Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                            return grid.mapRows(localEngine.combinedRowFilters(), producer, partialChangeData);
                        }, producer.getColumnDependencies(), Engine.Mode.RowBased);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            joined = projectState.join(changeData, joiner, newColumnModel);
        } else {
            ChangeData<List<Cell>> changeData = null;
            try {
                changeData = context.getChangeData(_changeDataId, new CellListChangeDataSerializer(),
                        (grid, partialChangeData) -> {
                            Engine localEngine = new Engine(grid, _engineConfig, context.getProjectId());
                            return grid.mapRecords(localEngine.combinedRecordFilters(), producer, partialChangeData);
                        }, producer.getColumnDependencies(), Engine.Mode.RecordBased);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            joined = projectState.join(changeData, joiner, newColumnModel);
        }

        // add facets after applying the operation
        ListFacet.ListFacetConfig judgmentFacet = new ListFacet.ListFacetConfig(
                _columnName + ": " + OpenRefineMessage.recon_operation_judgement_facet_name(),
                "grel:forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \"(unreconciled)\", \"(blank)\"))",
                _columnName);
        RangeFacet.RangeFacetConfig scoreFacet = new RangeFacet.RangeFacetConfig(
                _columnName + ": " + OpenRefineMessage.recon_operation_score_facet_name(),
                "grel:cell.recon.best.score",
                _columnName,
                null,
                null,
                null,
                null,
                null,
                null);
        List<FacetConfig> createdFacets = Arrays.asList(
                judgmentFacet,
                scoreFacet);

        return new ChangeResult(joined,
                GridPreservation.PRESERVES_ROWS, // TODO add record preservation metadata on Joiner
                createdFacets);
    }

    @Override
    public String getDescription() {
        return _reconConfig.getBriefDescription(_columnName);
    }

    @JsonProperty("config")
    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    protected static class Joiner extends RowInRecordChangeDataJoiner {

        private static final long serialVersionUID = 5848902684901514597L;
        private final int _columnIndex;

        public Joiner(int columnIndex) {
            _columnIndex = columnIndex;
        }

        @Override
        public Row call(Row row, IndexedData<Cell> indexedData) {
            Cell cell = indexedData.getData();
            if (indexedData.isPending()) {
                Cell currentCell = row.getCell(_columnIndex);
                cell = new Cell(
                        currentCell == null ? null : currentCell.value,
                        currentCell == null ? null : currentCell.recon,
                        true);
            }
            if (cell != null) {
                return row.withCell(_columnIndex, cell);
            } else {
                return row;
            }
        }

        @Override
        public boolean preservesRecordStructure() {
            return true; // blank cells are preserved
        }

    }

    protected static class ReconChangeDataProducer extends RowInRecordChangeDataProducer<Cell> {

        private static final long serialVersionUID = 881447948869363218L;
        transient private LoadingCache<ReconJob, Cell> cache = null;
        private final ReconConfig reconConfig;
        private final String columnName;
        private final int columnIndex;
        private final long historyEntryId;
        private final long rowCountEstimate;
        private final ColumnModel columnModel;

        protected ReconChangeDataProducer(
                String columnName,
                int columnIndex,
                ReconConfig reconConfig,
                long historyEntryId,
                long rowCountEstimate,
                ColumnModel columnModel) {
            this.reconConfig = reconConfig;
            this.columnName = columnName;
            this.columnIndex = columnIndex;
            this.historyEntryId = historyEntryId;
            this.rowCountEstimate = rowCountEstimate;
            this.columnModel = columnModel;
        }

        private void initCache() {
            cache = CacheBuilder.newBuilder()
                    .maximumSize(4096)
                    .build(new CacheLoader<ReconJob, Cell>() {

                        @Override
                        public Cell load(ReconJob key) throws Exception {
                            return loadAll(Collections.singletonList(key)).get(key);
                        }

                        @Override
                        public Map<ReconJob, Cell> loadAll(Iterable<? extends ReconJob> jobs) {
                            List<ReconJob> jobList = StreamSupport.stream(jobs.spliterator(), false)
                                    .collect(Collectors.toList());
                            List<Recon> recons = reconConfig.batchRecon(jobList, historyEntryId);
                            Map<ReconJob, Cell> results = new HashMap<>(jobList.size());
                            for (int i = 0; i != jobList.size(); i++) {
                                results.put(jobList.get(i), new Cell(jobList.get(i).getCellValue(), recons.get(i)));
                            }
                            return results;
                        }

                    });
        }

        @Override
        public Cell call(Record record, long rowId, Row row, ColumnModel columnModel) {
            return callRowBatch(Collections.singletonList(new IndexedRow(rowId, row)), columnModel).get(0);
        }

        @Override
        public List<Cell> callRowBatch(List<IndexedRow> rows, ColumnModel columnModel) {
            if (cache == null) {
                initCache();
            }
            List<ReconJob> reconJobs = new ArrayList<>(rows.size());
            for (IndexedRow indexedRow : rows) {
                Row row = indexedRow.getRow();
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    reconJobs.add(reconConfig.createJob(
                            this.columnModel,
                            indexedRow.getIndex(),
                            row,
                            columnName,
                            cell));
                } else {
                    reconJobs.add(null);
                }
            }
            try {
                Map<ReconJob, Cell> results = cache.getAll(reconJobs.stream().filter(r -> r != null).collect(Collectors.toList()));
                return reconJobs.stream().map(job -> results.get(job)).collect(Collectors.toList());
            } catch (ExecutionException e) {
                // the `batchRecon` method should throw IOException, it currently does not.
                // Once that is fixed, we should do a couple of retries here before failing
                throw new IllegalStateException("Fetching reconciliation responses failed", e);
            }
        }

        @Override
        public int getBatchSize() {
            return reconConfig.getBatchSize(rowCountEstimate);
        }

        @Override
        public int getMaxConcurrency() {
            return 1;
        }

    }

    /**
     * Filter used to select only rows which have a non-blank value to reconcile.
     */
    protected static class NonBlankRowFilter implements RowFilter {

        private static final long serialVersionUID = 6646807801184457426L;
        private final int columnIndex;

        protected NonBlankRowFilter(int columnIndex) {
            this.columnIndex = columnIndex;
        }

        @Override
        public boolean filterRow(long rowIndex, Row row) {
            return !row.isCellBlank(columnIndex);
        }
    }

}
