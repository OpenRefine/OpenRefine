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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.Engine;
import org.openrefine.browsing.Engine.Mode;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.history.History;
import org.openrefine.history.HistoryEntry;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Record;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.DataExtensionChange;
import org.openrefine.model.changes.DataExtensionChange.DataExtensionSerializer;
import org.openrefine.model.changes.RecordChangeDataProducer;
import org.openrefine.model.recon.ReconType;
import org.openrefine.model.recon.ReconciledDataExtensionJob;
import org.openrefine.model.recon.ReconciledDataExtensionJob.ColumnInfo;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtension;
import org.openrefine.model.recon.ReconciledDataExtensionJob.DataExtensionConfig;
import org.openrefine.model.recon.ReconciledDataExtensionJob.RecordDataExtension;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.process.LongRunningProcess;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtendDataOperation extends EngineDependentOperation {
    @JsonProperty("baseColumnName")
    final protected String              _baseColumnName;
    @JsonProperty("endpoint")
    final protected String              _endpoint;
    @JsonProperty("identifierSpace")
    final protected String              _identifierSpace;
    @JsonProperty("schemaSpace")
    final protected String              _schemaSpace;
    @JsonProperty("extension")
    final protected DataExtensionConfig _extension;
    @JsonProperty("columnInsertIndex")
    final protected int                 _columnInsertIndex;

    
    @JsonCreator
    public ExtendDataOperation(
        @JsonProperty("engineConfig")
        EngineConfig        engineConfig,
        @JsonProperty("baseColumnName")
        String              baseColumnName,
        @JsonProperty("endpoint")
        String              endpoint,
        @JsonProperty("identifierSpace")
        String              identifierSpace,
        @JsonProperty("schemaSpace")
        String              schemaSpace,
        @JsonProperty("extension")
        DataExtensionConfig extension,
        @JsonProperty("columnInsertIndex")
        int                 columnInsertIndex 
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
	public String getDescription() {
        return "Extend data at index " + _columnInsertIndex + 
            " based on column " + _baseColumnName;
    }
    
    @Override
    public Process createProcess(History history, ProcessManager manager) throws Exception {
        return new ExtendDataProcess(
            history,
            manager,
            getEngineConfig(),
            getDescription(),
            _identifierSpace,
            _schemaSpace
        );
    }
    
    public class ExtendDataProcess extends LongRunningProcess implements Runnable {
        final protected History     _history;
        final protected ProcessManager _processManager;
        final protected EngineConfig  _engineConfig;
        final protected long        _historyEntryID;
        protected int               _cellIndex;
        protected ReconciledDataExtensionJob _job;

        public ExtendDataProcess(
            History history, 
            ProcessManager processManager,
            EngineConfig engineConfig, 
            String description,
            String identifierSpace,
            String schemaSpace
        ) {
            super(description);
            _history = history;
            _processManager = processManager;
            _engineConfig = engineConfig;
            _historyEntryID = HistoryEntry.allocateID();
            
            _job = new ReconciledDataExtensionJob(_extension, _endpoint, identifierSpace, schemaSpace);
        }
        
        @Override
        protected Runnable getRunnable() {
            return this;
        }
        
        @Override
        public void run() {
            GridState state = _history.getCurrentGridState();
			Engine engine = new Engine(state, _engineConfig);
            
			ColumnModel columnModel = state.getColumnModel();
            
            try {
            _cellIndex = columnModel.getColumnIndexByName(_baseColumnName);
            if (_cellIndex == -1) {
                throw new Exception("No column named " + _baseColumnName);
            }
            
            /**
             * Prefetch column names with an initial request.
             */
            _job.extend(Collections.emptySet());
            List<String> columnNames = new ArrayList<>();
            for (ColumnInfo info : _job.columns) {
                columnNames.add(info.name);
            }
            
            List<ReconType> columnTypes = new ArrayList<>();
            for (ColumnInfo info : _job.columns) {
                columnTypes.add(info.expectedType);
            }
            
            /**
             * This operation does not always respect the rows mode, because
             * when fetching multiple values for the same row, the extra values
             * are spread in the record of the given row. Therefore, the fetching
             * is done in records mode at all times, but in rows mode we also
             * pass down the row filter to the fetcher so that it can filter out
             * rows that should not be fetched inside a given record.
             */
            
            RowFilter rowFilter = RowFilter.ANY_ROW;
            if (Mode.RowBased.equals(engine.getMode())) {
            	rowFilter = engine.combinedRowFilters();
            }
            DataExtensionProducer producer = new DataExtensionProducer(_job, _cellIndex, rowFilter);
            ChangeData<RecordDataExtension> changeData = state.mapRecords(engine.combinedRecordFilters(), producer);
            
            _history.getChangeDataStore().store(changeData, _historyEntryID, "extend", new DataExtensionSerializer());

            if (!_canceled) {
                HistoryEntry historyEntry = new HistoryEntry(
                    _historyEntryID, 
                    _description, 
                    ExtendDataOperation.this, 
                    new DataExtensionChange(
                    	_engineConfig,
                        _baseColumnName,
                        _endpoint,
                        _identifierSpace,
                        _schemaSpace,
                        _columnInsertIndex,
                        columnNames,
                        columnTypes)
                );
                
                _history.addEntry(historyEntry);
                _processManager.onDoneProcess(this);
            }
            } catch(Exception e) {
            	e.printStackTrace();
            	_processManager.onFailedProcess(this, e);
            }
        }
    }
    
    protected static class DataExtensionProducer implements RecordChangeDataProducer<RecordDataExtension> {

		private static final long serialVersionUID = -7946297987163653933L;
		private final ReconciledDataExtensionJob _job;
    	private final int _cellIndex;
    	private final RowFilter _rowFilter;
    	
    	protected DataExtensionProducer(ReconciledDataExtensionJob job, int cellIndex, RowFilter rowFilter) {
    		_job = job;
    		_cellIndex = cellIndex;
    		_rowFilter = rowFilter;
    	}

		@Override
		public RecordDataExtension call(Record record) {
			return call(Collections.singletonList(record)).get(0);
		}
		
		@Override
		public List<RecordDataExtension> call(List<Record> records) {
			
			Set<String> ids = new HashSet<>();
			
			for(Record record : records) {
				for(IndexedRow indexedRow : record.getIndexedRows()) {
					Row row = indexedRow.getRow();
					if (!_rowFilter.filterRow(indexedRow.getIndex(), row)) {
						continue;
					}
					Cell cell = row.getCell(_cellIndex);
					if (cell != null && cell.recon != null && cell.recon.match != null) {
						ids.add(cell.recon.match.id);
					}
				}
			}
			
			Map<String, DataExtension> extensions;
			try {
				extensions = _job.extend(ids);
			} catch (Exception e) {
				e.printStackTrace();
				extensions = Collections.emptyMap();
			}
			
			List<RecordDataExtension> results = new ArrayList<>();
			for(Record record : records) {
				Map<Long, DataExtension> recordExtensions = new HashMap<>();
				for(IndexedRow indexedRow : record.getIndexedRows()) {
					if (!_rowFilter.filterRow(indexedRow.getIndex(), indexedRow.getRow())) {
						continue;
					}
					Cell cell = indexedRow.getRow().getCell(_cellIndex);
					if (cell != null && cell.recon != null && cell.recon.match != null) {
						recordExtensions.put(indexedRow.getIndex(), extensions.get(cell.recon.match.id));
					}
				}
				results.add(new RecordDataExtension(recordExtensions));
			}
			return results;
		}
		
		@Override
		public int getBatchSize() {
			return _job.getBatchSize();
		}
    	
    }
}
