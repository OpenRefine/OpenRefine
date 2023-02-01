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
import org.openrefine.expr.ParsingException;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.*;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.overlay.OverlayModel;

/**
 * Runs reconciliation on a column.
 * <p>
 * TODO restore records mode
 */
public class ReconOperation extends EngineDependentOperation {

    final static Logger logger = LoggerFactory.getLogger("recon-operation");

    final protected String _columnName;
    final protected ReconConfig _reconConfig;

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
    public Change createChange() throws ParsingException {
        return new ColumnChangeByChangeData(
                "recon",
                _columnName,
                null,
                _engineConfig,
                _reconConfig) {

            @Override
            public RowInRecordChangeDataProducer<Cell> getChangeDataProducer(int columnIndex, String columnName, ColumnModel columnModel,
                    Map<String, OverlayModel> overlayModels, ChangeContext changeContext) {
                return new ReconChangeDataProducer(
                        columnName,
                        columnIndex,
                        _reconConfig,
                        changeContext.getHistoryEntryId(),
                        columnModel);
            }
        };
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

    // TODO: migrate those to the frontend.
    /*
     * protected final String _addJudgmentFacetJson = "{\n" + "  \"action\" : \"createFacet\",\n" +
     * "  \"facetConfig\" : {\n" + "  \"columnName\" : \"" + _columnName + "\",\n" +
     * "  \"expression\" : \"forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \\\"(unreconciled)\\\", \\\"(blank)\\\"))\",\n"
     * + "    \"name\" : \"" + _columnName + ": judgment\"\n" + "    },\n" + "    \"facetOptions\" : {\n" +
     * "      \"scroll\" : false\n" + "    },\n" + "    \"facetType\" : \"list\"\n" + " }"; protected final String
     * _addScoreFacetJson = "{\n" + "  \"action\" : \"createFacet\",\n" + "  \"facetConfig\" : {\n" +
     * "    \"columnName\" : \"" + _columnName + "\",\n" + "    \"expression\" : \"cell.recon.best.score\",\n" +
     * "    \"mode\" : \"range\",\n" + "    \"name\" : \"" + _columnName + ": best candidate's score\"\n" +
     * "         },\n" + "         \"facetType\" : \"range\"\n" + "}";
     */

    protected static class ReconChangeDataProducer extends RowInRecordChangeDataProducer<Cell> {

        private static final long serialVersionUID = 881447948869363218L;
        transient private LoadingCache<ReconJob, Cell> cache = null;
        private final ReconConfig reconConfig;
        private final String columnName;
        private final int columnIndex;
        private final long historyEntryId;
        private final ColumnModel columnModel;

        protected ReconChangeDataProducer(
                String columnName,
                int columnIndex,
                ReconConfig reconConfig,
                long historyEntryId,
                ColumnModel columnModel) {
            this.reconConfig = reconConfig;
            this.columnName = columnName;
            this.columnIndex = columnIndex;
            this.historyEntryId = historyEntryId;
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
        public Cell call(org.openrefine.model.Record record, long rowId, Row row) {
            return callRowBatch(Collections.singletonList(new IndexedRow(rowId, row))).get(0);
        }

        @Override
        public List<Cell> callRowBatch(List<IndexedRow> rows) {
            if (cache == null) {
                initCache();
            }
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
                return reconJobs.stream().map(job -> results.get(job)).collect(Collectors.toList());
            } catch (ExecutionException e) {
                // the `batchRecon` method should throw IOException, it currently does not.
                // Once that is fixed, we should do a couple of retries here before failing
                throw new IllegalStateException("Fetching reconciliation responses failed", e);
            }
        }

        @Override
        public int getBatchSize() {
            return reconConfig.getBatchSize();
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
