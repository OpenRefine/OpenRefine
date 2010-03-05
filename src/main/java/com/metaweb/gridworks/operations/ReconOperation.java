package com.metaweb.gridworks.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellChange;
import com.metaweb.gridworks.model.changes.ReconChange;
import com.metaweb.gridworks.model.recon.ReconConfig;
import com.metaweb.gridworks.model.recon.ReconJob;
import com.metaweb.gridworks.process.LongRunningProcess;
import com.metaweb.gridworks.process.Process;

public class ReconOperation extends EngineDependentOperation {
    private static final long serialVersionUID = 838795186905314865L;
    
    final protected String        _columnName;
    final protected ReconConfig _reconConfig;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ReconOperation(
            engineConfig, 
            obj.getString("columnName"),
            ReconConfig.reconstruct(obj.getJSONObject("config"))
        );
    }
    
    public ReconOperation(
        JSONObject engineConfig, 
        String columnName, 
        ReconConfig reconConfig
    ) {
        super(engineConfig);
        _columnName = columnName;
        _reconConfig = reconConfig;
    }

    public Process createProcess(Project project, Properties options) throws Exception {
        return new ReconProcess(
            project, 
            getEngineConfig(),
            getBriefDescription(null)
        );
    }
    
    protected String getBriefDescription(Project project) {
        return _reconConfig.getBriefDescription(project, _columnName);
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("columnName"); writer.value(_columnName);
        writer.key("config"); _reconConfig.write(writer, options);
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.endObject();
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
        
        public JobGroup(ReconJob job) {
            this.job = job;
        }
    }
    
    public class ReconProcess extends LongRunningProcess implements Runnable {
        final protected Project     _project;
        final protected JSONObject  _engineConfig;
        protected List<ReconEntry>  _entries;
        protected int               _cellIndex;
        
        public ReconProcess(
            Project project, 
            JSONObject engineConfig, 
            String description
        ) {
            super(description);
            _project = project;
            _engineConfig = engineConfig;
        }
        
        public void write(JSONWriter writer, Properties options)
                throws JSONException {
            
            writer.object();
            writer.key("id"); writer.value(hashCode());
            writer.key("description"); writer.value(_description);
            writer.key("immediate"); writer.value(false);
            writer.key("status"); writer.value(_thread == null ? "pending" : (_thread.isAlive() ? "running" : "done"));
            writer.key("progress"); writer.value(_progress);
            writer.key("onDone");
                writer.array();
                    writer.object();
                        writer.key("action"); writer.value("createFacet");
                        writer.key("facetType"); writer.value("list");
                        writer.key("facetConfig");
                            writer.object();
                                writer.key("name"); writer.value(_columnName + ": judgment");
                                writer.key("columnName"); writer.value(_columnName);
                                writer.key("expression"); writer.value("cell.recon.judgment");
                            writer.endObject();
                        writer.key("facetOptions");
                            writer.object();
                                writer.key("scroll"); writer.value(false);
                            writer.endObject();
                    writer.endObject();
                    writer.object();
                        writer.key("action"); writer.value("createFacet");
                        writer.key("facetType"); writer.value("range");
                        writer.key("facetConfig");
                            writer.object();
                                writer.key("name"); writer.value(_columnName + ": best candidate's score");
                                writer.key("columnName"); writer.value(_columnName);
                                writer.key("expression"); writer.value("cell.recon.best.score");
                                writer.key("mode"); writer.value("range");
                            writer.endObject();
                    writer.endObject();
                writer.endArray();
            writer.endObject();
        }
        
        protected Runnable getRunnable() {
            return this;
        }
        
        protected void populateEntries() throws Exception {
            Engine engine = new Engine(_project);
            engine.initializeFromJSON(_engineConfig);
            
            Column column = _project.columnModel.getColumnByName(_columnName);
            if (column == null) {
                throw new Exception("No column named " + _columnName);
            }
            
            _entries = new ArrayList<ReconEntry>(_project.rows.size());
            _cellIndex = column.getCellIndex();
            
            FilteredRows filteredRows = engine.getAllFilteredRows(false);
            filteredRows.accept(_project, new RowVisitor() {
                public boolean visit(Project project, int rowIndex, Row row, boolean contextual) {
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
        
        public void run() {
            try {
                populateEntries();
            } catch (Exception e2) {
                // TODO : Not sure what to do here?
                e2.printStackTrace();
            }
            
            Map<Integer, JobGroup> jobKeyToGroup = new HashMap<Integer, JobGroup>();
            
            for (ReconEntry entry : _entries) {
                ReconJob job = _reconConfig.createJob(
                    _project, 
                    entry.rowIndex, 
                    _project.rows.get(entry.rowIndex), 
                    _columnName, 
                    entry.cell
                );
                
                int key = job.getKey();
                JobGroup group = jobKeyToGroup.get(key);
                if (group == null) {
                    group = new JobGroup(job);
                    jobKeyToGroup.put(key, group);
                }
                group.entries.add(entry);
            }
            
            List<CellChange> cellChanges = new ArrayList<CellChange>(_entries.size());
            List<JobGroup> groups = new ArrayList<JobGroup>(jobKeyToGroup.values());
            
            int batchSize = _reconConfig.getBatchSize();
            for (int i = 0; i < groups.size(); i += batchSize) {
                int to = Math.min(i + batchSize, groups.size());
                
                List<ReconJob> jobs = new ArrayList<ReconJob>(to - i);
                for (int j = i; j < to; j++) {
                    jobs.add(groups.get(j).job);
                }
                
                List<Recon> recons = _reconConfig.batchRecon(jobs);
                for (int j = i; j < to; j++) {
                    Recon recon = recons.get(j - i);
                    if (recon == null) {
                        recon = new Recon();
                    }
                    
                    for (ReconEntry entry : groups.get(j).entries) {
                        Cell oldCell = entry.cell;
                        Cell newCell = new Cell(oldCell.value, recon);
                        
                        CellChange cellChange = new CellChange(
                            entry.rowIndex, 
                            _cellIndex, 
                            oldCell, 
                            newCell
                        );
                        cellChanges.add(cellChange);
                    }
                }
                
                _progress = i * 100 / groups.size();
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
                    null
                );
                
                HistoryEntry historyEntry = new HistoryEntry(
                    _project, 
                    _description, 
                    ReconOperation.this, 
                    reconChange
                );
                
                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }
    }
}
