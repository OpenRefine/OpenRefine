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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.refine.messages.OpenRefineMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellChange;
import com.google.refine.model.changes.ReconChange;
import com.google.refine.model.recon.ReconConfig;
import com.google.refine.model.recon.ReconJob;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import com.google.refine.util.ParsingUtilities;

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
                getBriefDescription(null));
    }

    @Override
    protected String getBriefDescription(Project project) {
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
                "    \"name\" : \"" + _columnName + ": " + OpenRefineMessage.recon_operation_judgement_facet_name() + "\"\n" +
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
                "    \"name\" : \"" + _columnName + ": " + OpenRefineMessage.recon_operation_score_facet_name() + "\"\n" +
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

            Column column = _project.columnModel.getColumnByName(_columnName);
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

                    jobToGroup.remove(job);
                    jobs.remove(j);
                    done++;

                    if (recon != null) {
                        recon.judgmentBatchSize = entries.size();
                    }

                    for (ReconEntry entry : entries) {
                        if (recon == null) {
                            // TODO add EvalError instead? That is not so convenient
                            // for users because they would lose the cell contents.
                            // Better leave the cell unreconciled so they can be reconciled again later.
                            continue;
                        }
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

            // TODO: Option to keep partial results after cancellation?
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

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }
    }
}
