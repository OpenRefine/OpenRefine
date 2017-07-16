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

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.model.changes.DataExtensionChange;
import com.google.refine.model.recon.ReconciledDataExtensionJob;
import com.google.refine.model.recon.ReconciledDataExtensionJob.ColumnInfo;
import com.google.refine.model.recon.ReconciledDataExtensionJob.DataExtension;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconType;
import com.google.refine.model.Row;
import com.google.refine.model.changes.CellAtRow;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;

public class ExtendDataOperation extends EngineDependentOperation {
    final protected String     _baseColumnName;
    final protected String     _endpoint;
    final protected String     _identifierSpace;
    final protected String     _schemaSpace;
    final protected JSONObject _extension;
    final protected int        _columnInsertIndex;
    
    static public AbstractOperation reconstruct(Project project, JSONObject obj) throws Exception {
        JSONObject engineConfig = obj.getJSONObject("engineConfig");
        
        return new ExtendDataOperation(
            engineConfig,
            obj.getString("baseColumnName"),
            obj.getString("endpoint"),
            obj.getString("identifierSpace"),
            obj.getString("schemaSpace"),
            obj.getJSONObject("extension"),
            obj.getInt("columnInsertIndex")
        );
    }
    
    public ExtendDataOperation(
        JSONObject     engineConfig,
        String         baseColumnName,
        String         endpoint,
        String         identifierSpace,
        String         schemaSpace,
        JSONObject     extension,
        int            columnInsertIndex 
    ) {
        super(engineConfig);
        
        _baseColumnName = baseColumnName;
        _endpoint = endpoint;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
        _extension = extension;
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
        writer.key("endpoint"); writer.value(_endpoint);
        writer.key("identifierSpace"); writer.value(_identifierSpace);
        writer.key("schemaSpace"); writer.value(_schemaSpace);
        writer.key("extension"); writer.value(_extension);
        writer.endObject();
    }

    @Override
    protected String getBriefDescription(Project project) {
        return "Extend data at index " + _columnInsertIndex + 
            " based on column " + _baseColumnName;
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return "Extend data at index " + _columnInsertIndex + 
            " based on column " + column.getName() + 
            " by filling " + cellsAtRows.size();
    }
    
    @Override
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
        protected ReconciledDataExtensionJob _job;

        public ExtendDataProcess(
            Project project, 
            JSONObject engineConfig, 
            String description
        ) throws JSONException {
            super(description);
            _project = project;
            _engineConfig = engineConfig;
            _historyEntryID = HistoryEntry.allocateID();
            
            _job = new ReconciledDataExtensionJob(_extension, _endpoint);
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
                
                @Override
                public boolean visit(Project project, int rowIndex, Row row) {
                    Cell cell = row.getCell(_cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        _rowIndices.add(rowIndex);
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
            Set<String> ids = new HashSet<String>();
            
            int end;
            for (end = from; end < limit && ids.size() < 10; end++) {
                int index = rowIndices.get(end);
                Row row = _project.rows.get(index);
                Cell cell = row.getCell(_cellIndex);
                
                ids.add(cell.recon.match.id);
            }
            
            Map<String, DataExtension> map = null;
            try {
                map = _job.extend(ids, reconCandidateMap);
            } catch (Exception e) {
                map = new HashMap<String, DataExtension>();
            }
            
            for (int i = from; i < end; i++) {
                int index = rowIndices.get(i);
                Row row = _project.rows.get(index);
                Cell cell = row.getCell(_cellIndex);
                String guid = cell.recon.match.id;
                
                if (map.containsKey(guid)) {
                    dataExtensions.add(map.get(guid));
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
                    columnNames.add(info.name);
                }
                
                List<ReconType> columnTypes = new ArrayList<ReconType>();
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
                        _endpoint,
                        _identifierSpace,
                        _schemaSpace,
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
