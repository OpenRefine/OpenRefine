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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RowVisitor;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.changes.CellChange;
import org.openrefine.model.changes.Change;
import org.openrefine.model.changes.ReconChange;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconJob;
import org.openrefine.model.recon.StandardReconConfig;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.util.ParsingUtilities;

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
    public Process createProcess(Project project, Properties options) throws Exception {
        return new ReconProcess(
                project,
                getEngineConfig(),
                getDescription());
    }

    @Override
    protected String getDescription() {
        return _reconConfig.getBriefDescription(project, _columnName);
    }

    @JsonProperty("config")
    public ReconConfig getReconConfig() {
        return _reconConfig;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _columnName;
    }

    static protected class ReconEntry {

        final public int rowIndex;
        final public Cell cell;

        public ReconEntry(int rowIndex, Cell cell) {
            this.rowIndex = rowIndex;
            this.cell = cell;
        }
    }

    static protected class JobGroup {

        final public ReconJob job;
        final public List<ReconEntry> entries = new ArrayList<ReconEntry>();
        public int trials = 0;

        public JobGroup(ReconJob job) {
            this.job = job;
        }
    }

    public class ReconProcess extends LongRunningProcess implements Runnable {

        final protected Project _project;
        final protected EngineConfig _engineConfig;
        final protected long _historyEntryID;
        protected List<ReconEntry> _entries;
        protected int _cellIndex;

        protected final String _addJudgmentFacetJson = "{\n" +
                "  \"action\" : \"createFacet\",\n" +
                "  \"facetConfig\" : {\n" +
                "  \"columnName\" : \"" + _columnName + "\",\n" +
                "  \"expression\" : \"forNonBlank(cell.recon.judgment, v, v, if(isNonBlank(value), \\\"(unreconciled)\\\", \\\"(blank)\\\"))\",\n"
                +
                "    \"name\" : \"" + _columnName + ": judgment\"\n" +
                "    },\n" +
                "    \"facetOptions\" : {\n" +
                "      \"scroll\" : false\n" +
                "    },\n" +
                "    \"facetType\" : \"list\"\n" +
                " }";
        protected final String _addScoreFacetJson = "{\n" +
                "  \"action\" : \"createFacet\",\n" +
                "  \"facetConfig\" : {\n" +
                "    \"columnName\" : \"" + _columnName + "\",\n" +
                "    \"expression\" : \"cell.recon.best.score\",\n" +
                "    \"mode\" : \"range\",\n" +
                "    \"name\" : \"" + _columnName + ": best candidate's score\"\n" +
                "         },\n" +
                "         \"facetType\" : \"range\"\n" +
                "}";
        protected JsonNode _addJudgmentFacet, _addScoreFacet;

        public ReconProcess(
                Project project,
                EngineConfig engineConfig,
                String description) {
            super(description);
            _project = project;
            _engineConfig = engineConfig;
            _historyEntryID = HistoryEntry.allocateID();
            try {
                _addJudgmentFacet = ParsingUtilities.mapper.readValue(_addJudgmentFacetJson, JsonNode.class);
                _addScoreFacet = ParsingUtilities.mapper.readValue(_addScoreFacetJson, JsonNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @JsonProperty("onDone")
        public List<JsonNode> onDoneActions() {
            List<JsonNode> onDone = new ArrayList<>();
            onDone.add(_addJudgmentFacet);
            if (_reconConfig instanceof StandardReconConfig) {
                onDone.add(_addScoreFacet);
            }
            return onDone;
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        protected void populateEntries() throws Exception {
            Engine engine = new Engine(_project);
            engine.initializeFromConfig(_engineConfig);

            ColumnMetadata column = _project.columnModel.getColumnByName(_columnName);
            if (column == null) {
                throw new Exception("No column named " + _columnName);
            }

            _entries = new ArrayList<ReconEntry>(_project.rows.size());
            _cellIndex = column.getCellIndex();

            FilteredRows filteredRows = engine.getAllFilteredRows();
            filteredRows.accept(_project, new RowVisitor() {

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
                    if (_cellIndex < row.cells.size()) {
                        Cell cell = row.cells.get(_cellIndex);
                        if (cell != null && ExpressionUtils.isNonBlankData(cell.value)) {
                            _entries.add(new ReconEntry(rowIndex, cell));
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public void run() {
            try {
                populateEntries();
            } catch (Exception e2) {
                // TODO : Not sure what to do here?
                e2.printStackTrace();
            }

            Map<String, JobGroup> jobKeyToGroup = new HashMap<String, JobGroup>();

            for (ReconEntry entry : _entries) {
                ReconJob job = _reconConfig.createJob(
                        _project,
                        entry.rowIndex,
                        _project.rows.get(entry.rowIndex),
                        _columnName,
                        entry.cell);

                String key = job.getStringKey();
                JobGroup group = jobKeyToGroup.get(key);
                if (group == null) {
                    group = new JobGroup(job);
                    jobKeyToGroup.put(key, group);
                }
                group.entries.add(entry);
            }

            int batchSize = _reconConfig.getBatchSize();
            int done = 0;

            List<CellChange> cellChanges = new ArrayList<CellChange>(_entries.size());
            List<JobGroup> groups = new ArrayList<JobGroup>(jobKeyToGroup.values());

            List<ReconJob> jobs = new ArrayList<ReconJob>(batchSize);
            Map<ReconJob, JobGroup> jobToGroup = new HashMap<ReconJob, ReconOperation.JobGroup>();

            for (int i = 0; i < groups.size(); /* don't increment here */) {
                while (jobs.size() < batchSize && i < groups.size()) {
                    JobGroup group = groups.get(i++);

                    jobs.add(group.job);
                    jobToGroup.put(group.job, group);
                }

                List<Recon> recons = _reconConfig.batchRecon(jobs, _historyEntryID);
                for (int j = jobs.size() - 1; j >= 0; j--) {
                    ReconJob job = jobs.get(j);
                    Recon recon = j < recons.size() ? recons.get(j) : null;
                    JobGroup group = jobToGroup.get(job);
                    List<ReconEntry> entries = group.entries;

                    if (recon == null) {
                        group.trials++;
                        if (group.trials < 3) {
                            logger.warn("Re-trying job including cell containing: " + entries.get(0).cell.value);
                            continue; // try again next time
                        }
                        logger.warn("Failed after 3 trials for job including cell containing: " + entries.get(0).cell.value);
                    }

                    jobToGroup.remove(job);
                    jobs.remove(j);
                    done++;

                    if (recon == null) {
                        recon = _reconConfig.createNewRecon(_historyEntryID);
                    }
                    recon.judgmentBatchSize = entries.size();

                    for (ReconEntry entry : entries) {
                        Cell oldCell = entry.cell;
                        Cell newCell = new Cell(oldCell.value, recon);

                        CellChange cellChange = new CellChange(
                                entry.rowIndex,
                                _cellIndex,
                                oldCell,
                                newCell);
                        cellChanges.add(cellChange);
                    }
                }

                _progress = done * 100 / groups.size();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    if (_canceled) {
                        break;
                    }
                }
            }

            if (!_canceled) {
                Change reconChange = new ReconChange(
                        cellChanges,
                        _columnName,
                        _reconConfig,
                        null);

                HistoryEntry historyEntry = new HistoryEntry(
                        _historyEntryID,
                        _project,
                        _description,
                        ReconOperation.this,
                        reconChange);

                _project.getHistory().addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }
    }
}
