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

import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.facets.ListFacet;
import org.openrefine.browsing.facets.RangeFacet;
import org.openrefine.messages.OpenRefineMessage;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnInsertion;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowInRecordMapper;
import org.openrefine.model.changes.ChangeContext;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.operations.RowMapOperation;
import org.openrefine.operations.exceptions.OperationException;
import org.openrefine.overlay.OverlayModel;

/**
 * Runs reconciliation on a column.
 * <p>
 * TODO restore records mode
 */
public class ReconOperation extends RowMapOperation {

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
    public List<String> getColumnDependencies() {
        List<String> dependencies = new ArrayList<>();
        dependencies.add(_columnName);
        List<String> depNames = _reconConfig.getColumnDependencies();
        if (depNames != null) {
            for (String depName : depNames) {
                if (dependencies.indexOf(depName) == -1) {
                    dependencies.add(depName);
                }
            }
            return dependencies;
        } else {
            return null;
        }
    }

    @Override
    public List<ColumnInsertion> getColumnInsertions() {
        return Collections.singletonList(new ColumnInsertion(_columnName, _columnName, true, null, _reconConfig, true));
    }

    @Override
    public List<FacetConfig> getCreatedFacets() {
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
        return Arrays.asList(
                judgmentFacet,
                scoreFacet);
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

    @Override
    protected RowInRecordMapper getPositiveRowMapper(ColumnModel columnModel, Map<String, OverlayModel> overlayModels,
            long estimatedRowCount, ChangeContext context) throws OperationException {
        return new Mapper(_columnName, _reconConfig, context.getHistoryEntryId(), estimatedRowCount, columnModel);
    }

    protected static class Mapper extends RowInRecordMapper {

        private static final long serialVersionUID = 881447948869363218L;
        transient private LoadingCache<ReconJob, Cell> cache = null;
        private final ReconConfig reconConfig;
        private final String columnName;
        private final long historyEntryId;
        private final long rowCountEstimate;
        private final ColumnModel columnModel;

        protected Mapper(
                String columnName,
                ReconConfig reconConfig,
                long historyEntryId,
                long rowCountEstimate,
                ColumnModel columnModel) {
            this.reconConfig = reconConfig;
            this.columnName = columnName;
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
        public Row call(Record record, long rowId, Row row) {
            return callRowBatch(Collections.singletonList(record), Collections.singletonList(new IndexedRow(rowId, row))).get(0);
        }

        @Override
        public List<Row> callRowBatch(List<Record> records, List<IndexedRow> rows) {
            if (cache == null) {
                initCache();
            }
            int columnIndex = columnModel.getColumnIndexByName(columnName);
            List<ReconJob> reconJobs = new ArrayList<>(rows.size());
            for (IndexedRow indexedRow : rows) {
                Row row = indexedRow.getRow();
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    reconJobs.add(reconConfig.createJob(
                            columnModel,
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
                List<Row> rowsResult = new ArrayList<>(results.size());
                for (int i = 0; i != reconJobs.size(); i++) {
                    Row oldRow = rows.get(i).getRow();
                    rowsResult.add(new Row(Collections.singletonList(results.get(reconJobs.get(i))), oldRow.flagged, oldRow.starred));
                }
                return rowsResult;
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

        @Override
        public boolean preservesRecordStructure() {
            return true; // blank cells are preserved
        }

        @Override
        public boolean persistResults() {
            return true;
        }

    }

}
