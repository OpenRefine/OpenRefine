package com.google.refine.com.zemanta.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.com.zemanta.model.changes.ExtractEntitiesFromTextChange;
import com.google.refine.com.zemanta.util.ExtractEntitiesFromTextJob;
import com.google.refine.com.zemanta.util.ExtractEntitiesFromTextJob.ColumnInfo;
import com.google.refine.com.zemanta.util.ExtractEntitiesFromTextJob.DataExtension;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;


public class ExtractEntitiesFromTextOperation extends EngineDependentOperation {

    final protected String     _baseColumnName;
    final protected JSONObject _extension;
    final protected int        _columnInsertIndex;

    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
            JSONObject engineConfig = obj.getJSONObject("engineConfig");
            
            return new ExtractEntitiesFromTextOperation(
                engineConfig,
                obj.getString("baseColumnName"),
                obj.getJSONObject("extension"),
                obj.getInt("columnInsertIndex")
            );
     }
        
    public ExtractEntitiesFromTextOperation(
        JSONObject     engineConfig,
        String         baseColumnName,
        JSONObject     extension,
        int            columnInsertIndex 
    ) {
        super(engineConfig);
        
        _baseColumnName = baseColumnName;
        _extension = extension;
        System.out.println("Extension: "+ _extension);
        _columnInsertIndex = columnInsertIndex;
    }

    @Override
    public void write(JSONWriter writer, Properties options) 
                    throws JSONException {
            
        writer.object();
        writer.key("op"); writer.value(OperationRegistry.s_opClassToName.get(this.getClass()));
        writer.key("description"); writer.value(getBriefDescription(null));
        writer.key("engineConfig"); writer.value(getEngineConfig());
        writer.key("columnInsertIndex"); writer.value(_columnInsertIndex);
        writer.key("baseColumnName"); writer.value(_baseColumnName);
        writer.key("extension"); writer.value(_extension);
        writer.endObject();

    }
    
    @Override
    protected String getBriefDescription(Project project) {
        return "Extracting entities from text in column " + _baseColumnName + " at index " + _columnInsertIndex;
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return "Extracting entities from text in column " + column.getName() + " at index " + _columnInsertIndex + 
            " by filling " + cellsAtRows.size();
    }

    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        return new ExtractEntitiesProcess(
            project, 
            getEngineConfig(),
            getBriefDescription(null)
        );
    }
    
    public class ExtractEntitiesProcess extends LongRunningProcess implements Runnable {
            final protected Project     _project;
            final protected JSONObject  _engineConfig;
            final protected long        _historyEntryID;
            protected int               _cellIndex;
            protected ExtractEntitiesFromTextJob _job;

            public ExtractEntitiesProcess(
                Project project, 
                JSONObject engineConfig, 
                String description
            ) throws JSONException {
                super(description);
                _project = project;
                _engineConfig = engineConfig;
                _historyEntryID = HistoryEntry.allocateID();
                
                _job = new ExtractEntitiesFromTextJob(_extension);
            }
            
            @Override
            public void write(JSONWriter writer, Properties options)
                    throws JSONException {
                
                writer.object();
                writer.key("id"); writer.value(hashCode());
                writer.key("description"); writer.value(_description);
                writer.key("immediate"); writer.value(false);
                writer.key("status"); writer.value(_thread == null ? "pending" : (_thread.isAlive() ? "running" : "done"));
                writer.key("progress"); writer.value(_progress);
                writer.endObject();
            }
            
            @Override
            protected Runnable getRunnable() {
                return this;
            }
            
            protected void populateRowsWithMatches(List<Integer> rowIndices) throws Exception {
                Engine engine = new Engine(_project);
                engine.initializeFromJSON(_engineConfig);
                
                Column column = _project.columnModel.getColumnByName(_baseColumnName);
                if (column == null) {
                    throw new Exception("No column named " + _baseColumnName);
                }
                
                _cellIndex = column.getCellIndex();
                
                FilteredRows filteredRows = engine.getAllFilteredRows();
                filteredRows.accept(_project, new RowVisitor() {
                    List<Integer> _rowIndices;
                    
                    public RowVisitor init(List<Integer> rowIndices) {
                        _rowIndices = rowIndices;
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
                    
                    //TODO: probably you'd have to change this
                    @Override
                    public boolean visit(Project project, int rowIndex, Row row) {
                        Cell cell = row.getCell(_cellIndex);
                        //if (cell != null && cell.recon != null && cell.recon.match != null) {
                        if(cell != null) {
                                _rowIndices.add(rowIndex);
                        }
                        
                        return false;
                    }
                }.init(rowIndices));
            }
            
        //TODO: changed ids to full texts
        protected int extendRows(
            List<Integer> rowIndices, 
            List<DataExtension> dataExtensions, 
            int from, 
            int limit,
            Map<String, ReconCandidate> reconCandidateMap
        ) {
            Set<String> fullTexts = new HashSet<String>();
            
            int end;
            for (end = from; end < limit && fullTexts.size() < 10; end++) {
                int index = rowIndices.get(end);
                Row row = _project.rows.get(index);
                Cell cell = row.getCell(_cellIndex);
                
                fullTexts.add(cell.value.toString());
            }
            
            Map<String, DataExtension> map = null;
            try {
                map = _job.extend(fullTexts, reconCandidateMap);
            } catch (Exception e) {
                map = new HashMap<String, DataExtension>();
            }
            
            for (int i = from; i < end; i++) {
                int index = rowIndices.get(i);
                Row row = _project.rows.get(index);
                Cell cell = row.getCell(_cellIndex);
                String key  = cell.value.toString();
                
                if (map.containsKey(key)) {
                    dataExtensions.add(map.get(key));
                } else {
                    dataExtensions.add(null);
                }
            }
            
            return end;
        }
        
        @Override
        public void run() {
            List<Integer> rowIndices = new ArrayList<Integer>();
            List<DataExtension> dataExtensions = new ArrayList<DataExtension>();
            
            try {
                populateRowsWithMatches(rowIndices);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            
            int start = 0;
            Map<String, ReconCandidate> reconCandidateMap = new HashMap<String, ReconCandidate>();
            
            while (start < rowIndices.size()) {
                int end = extendRows(rowIndices, dataExtensions, start, rowIndices.size(), reconCandidateMap);
                start = end;
                
                _progress = end * 100 / rowIndices.size();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    if (_canceled) {
                        break;
                    }
                }
            }
            
            if (!_canceled) {
                List<String> columnNames = new ArrayList<String>();
                for (ColumnInfo info : _job.columns) {  
                    columnNames.add(info.name);
                }
                                                
                HistoryEntry historyEntry = new HistoryEntry(
                    _historyEntryID,
                    _project, 
                    _description, 
                    ExtractEntitiesFromTextOperation.this, 
                    new ExtractEntitiesFromTextChange(
                        _baseColumnName,
                        _columnInsertIndex,
                        columnNames,
                        rowIndices,
                        dataExtensions,
                        _historyEntryID)
                );
                
                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }     
            
    }
}
