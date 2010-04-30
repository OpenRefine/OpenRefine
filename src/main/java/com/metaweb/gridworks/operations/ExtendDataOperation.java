package com.metaweb.gridworks.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.Engine;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RowVisitor;
import com.metaweb.gridworks.history.HistoryEntry;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.changes.CellAtRow;
import com.metaweb.gridworks.model.changes.DataExtensionChange;
import com.metaweb.gridworks.process.LongRunningProcess;
import com.metaweb.gridworks.process.Process;
import com.metaweb.gridworks.protograph.FreebaseType;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob.ColumnInfo;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob.DataExtension;

public class ExtendDataOperation extends EngineDependentOperation {
    final protected String     _baseColumnName;
    final protected JSONObject _extension;
    final protected int        _columnInsertIndex;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ExtendDataOperation(
            engineConfig,
            obj.getString("baseColumnName"),
            obj.getJSONObject("extension"),
            obj.getInt("columnInsertIndex")
        );
    }
    
    public ExtendDataOperation(
        JSONObject     engineConfig,
        String         baseColumnName,
        JSONObject	   extension,
        int            columnInsertIndex 
    ) {
        super(engineConfig);
        
        _baseColumnName = baseColumnName;
        _extension = extension;
        _columnInsertIndex = columnInsertIndex;
    }

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

    protected String getBriefDescription(Project project) {
        return "Extend data at index " + _columnInsertIndex + 
            " based on column " + _baseColumnName;
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return "Extend data at index " + _columnInsertIndex + 
            " based on column " + column.getName() + 
            " by filling " + cellsAtRows.size();
    }
    
    public Process createProcess(Project project, Properties options) throws Exception {
        return new ExtendDataProcess(
            project, 
            getEngineConfig(),
            getBriefDescription(null)
        );
    }
    
    public class ExtendDataProcess extends LongRunningProcess implements Runnable {
        final protected Project     _project;
        final protected JSONObject  _engineConfig;
        final protected long        _historyEntryID;
        protected int               _cellIndex;
        protected FreebaseDataExtensionJob _job;

        public ExtendDataProcess(
            Project project, 
            JSONObject engineConfig, 
            String description
        ) throws JSONException {
            super(description);
            _project = project;
            _engineConfig = engineConfig;
            _historyEntryID = HistoryEntry.allocateID();
            
            _job = new FreebaseDataExtensionJob(_extension);
        }
        
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
            
            FilteredRows filteredRows = engine.getAllFilteredRows(false);
            filteredRows.accept(_project, new RowVisitor() {
            	List<Integer> _rowIndices;
            	
            	public RowVisitor init(List<Integer> rowIndices) {
            		_rowIndices = rowIndices;
            		return this;
            	}
                public boolean visit(Project project, int rowIndex, Row row, boolean includeContextual, boolean includeDependent) {
                    if (!includeContextual) {
                        Cell cell = row.getCell(_cellIndex);
                        if (cell != null && cell.recon != null && cell.recon.match != null) {
                        	_rowIndices.add(rowIndex);
                        }
                    }
                    return false;
                }
            }.init(rowIndices));
        }
        
        protected int extendRows(
    		List<Integer> rowIndices, 
    		List<DataExtension> dataExtensions, 
    		int from, 
    		int limit,
            Map<String, ReconCandidate> reconCandidateMap
   		) {
            Set<String> guids = new HashSet<String>();
            
            int end;
        	for (end = from; end < limit && guids.size() < 10; end++) {
        		int index = rowIndices.get(end);
        		Row row = _project.rows.get(index);
        		Cell cell = row.getCell(_cellIndex);
        		
                guids.add(cell.recon.match.topicGUID);
        	}
        	
        	Map<String, DataExtension> map = null;
            try {
				map = _job.extend(guids, reconCandidateMap);
			} catch (Exception e) {
				map = new HashMap<String, DataExtension>();
			}
			
        	for (int i = from; i < end; i++) {
        		int index = rowIndices.get(i);
        		Row row = _project.rows.get(index);
        		Cell cell = row.getCell(_cellIndex);
        		String guid = cell.recon.match.topicGUID;
        		
        		if (map.containsKey(guid)) {
        			dataExtensions.add(map.get(guid));
        		} else {
        			dataExtensions.add(null);
        		}
        	}
        	
        	return end;
        }
        
        public void run() {
        	List<Integer> rowIndices = new ArrayList<Integer>();
        	List<DataExtension> dataExtensions = new ArrayList<DataExtension>();
        	
            try {
                populateRowsWithMatches(rowIndices);
            } catch (Exception e2) {
                // TODO : Not sure what to do here?
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
            		columnNames.add(StringUtils.join(info.names, " - "));
            	}
            	
                List<FreebaseType> columnTypes = new ArrayList<FreebaseType>();
                for (ColumnInfo info : _job.columns) {
                    columnTypes.add(info.expectedType);
                }
            	
                HistoryEntry historyEntry = new HistoryEntry(
                    _historyEntryID,
                    _project, 
                    _description, 
                    ExtendDataOperation.this, 
                    new DataExtensionChange(
                    	_baseColumnName,
                    	_columnInsertIndex,
                    	columnNames,
                    	columnTypes,
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
