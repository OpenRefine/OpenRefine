package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.history.Change;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.ReconStats;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.model.recon.DataExtensionReconConfig;
import com.metaweb.gridworks.protograph.FreebaseType;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.metaweb.gridworks.util.Pool;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob.DataExtension;

public class DataExtensionChange implements Change {
	final protected String				_baseColumnName;
	final protected int		  			_columnInsertIndex;
	
	final protected List<String>        _columnNames;
	final protected List<FreebaseType>  _columnTypes;
	
    final protected List<Integer> 		_rowIndices;
    final protected List<DataExtension> _dataExtensions;
    
    protected int 						_firstNewCellIndex = -1;
    protected List<Row>       			_oldRows;
    protected List<Row>       			_newRows;
    
    public DataExtensionChange(
		String baseColumnName, 
		int columnInsertIndex, 
		List<String> columnNames,
		List<FreebaseType> columnTypes,
		List<Integer> rowIndices,
		List<DataExtension> dataExtensions
	) {
    	_baseColumnName = baseColumnName;
    	_columnInsertIndex = columnInsertIndex;
    	
    	_columnNames = columnNames;
    	_columnTypes = columnTypes;
    	
        _rowIndices = rowIndices;
        _dataExtensions = dataExtensions;
    }

    protected DataExtensionChange(
		String 				baseColumnName, 
		int 				columnInsertIndex,
		
		List<String> 		columnNames,
        List<FreebaseType> columnTypes,
        
		List<Integer> 		rowIndices,
		List<DataExtension> dataExtensions,
		int 				firstNewCellIndex,
		List<Row> 			oldRows,
		List<Row> 			newRows
	) {
    	_baseColumnName = baseColumnName;
    	_columnInsertIndex = columnInsertIndex;
    	
    	_columnNames = columnNames;
        _columnTypes = columnTypes;
        
        _rowIndices = rowIndices;
        _dataExtensions = dataExtensions;
        
        _firstNewCellIndex = firstNewCellIndex;
        _oldRows = oldRows;
        _newRows = newRows;
    }

    public void apply(Project project) {
        synchronized (project) {
            if (_firstNewCellIndex < 0) {
            	_firstNewCellIndex = project.columnModel.allocateNewCellIndex();
            	for (int i = 1; i < _columnNames.size(); i++) {
            		project.columnModel.allocateNewCellIndex();
            	}
            	
                _oldRows = new ArrayList<Row>(project.rows);
                
                _newRows = new ArrayList<Row>(project.rows.size());
                
                int cellIndex = project.columnModel.getColumnByName(_baseColumnName).getCellIndex();
                int keyCellIndex = project.columnModel.columns.get(project.columnModel.getKeyColumnIndex()).getCellIndex();
                int index = 0;
                
                int rowIndex = _rowIndices.get(index);
                DataExtension dataExtension = _dataExtensions.get(index);
                index++;
                
                for (int r = 0; r < _oldRows.size(); r++) {
                	Row oldRow = _oldRows.get(r);
                	if (r < rowIndex) {
                		_newRows.add(oldRow.dup());
                		continue;
                	}
                	
                	if (dataExtension == null || dataExtension.data.length == 0) {
                		_newRows.add(oldRow);
                	} else {
                		Row firstNewRow = oldRow.dup();
                		extendRow(firstNewRow, dataExtension, 0);
                		_newRows.add(firstNewRow);
                		
                        int r2 = r + 1;
                        for (int subR = 1; subR < dataExtension.data.length; subR++) {
                            if (r2 < project.rows.size()) {
                                Row oldRow2 = project.rows.get(r2);
                                if (oldRow2.isCellBlank(cellIndex) && 
                                    oldRow2.isCellBlank(keyCellIndex)) {
                                    
                                    Row newRow = oldRow2.dup();
                                    extendRow(newRow, dataExtension, subR);
                                    
                                    _newRows.add(newRow);
                                    r2++;
                                    
                                    continue;
                                }
                            }
                            
                            Row newRow = new Row(cellIndex + _columnNames.size());
                            extendRow(newRow, dataExtension, subR);
                            
                            _newRows.add(newRow);
                        }
                        
                        r = r2 - 1; // r will be incremented by the for loop anyway
                	}
                	
                    rowIndex = index < _rowIndices.size() ? _rowIndices.get(index) : _oldRows.size();
                    dataExtension = index < _rowIndices.size() ? _dataExtensions.get(index) : null;
                    index++;
                }
            }
            
            project.rows.clear();
            project.rows.addAll(_newRows);
            
            for (int i = 0; i < _columnNames.size(); i++) {
            	String name = _columnNames.get(i);
            	int cellIndex = _firstNewCellIndex + i;
            	
            	Column column = new Column(cellIndex, name);
            	column.setReconConfig(new DataExtensionReconConfig(_columnTypes.get(i)));
            	column.setReconStats(ReconStats.create(project, cellIndex));
            	
            	project.columnModel.columns.add(_columnInsertIndex + i, column);
            }
            
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }
    
    protected void extendRow(Row row, DataExtension dataExtension, int extensionRowIndex) {
    	Object[] values = dataExtension.data[extensionRowIndex];
    	for (int c = 0; c < values.length; c++) {
    		Object value = values[c];
    		Cell cell = null;
    		
    		if (value instanceof ReconCandidate) {
    			ReconCandidate rc = (ReconCandidate) value;
    			Recon recon = new Recon();
    			recon.addCandidate(rc);
    			recon.service = "mql";
    			recon.match = rc;
    			recon.matchRank = 0;
    			recon.judgment = Judgment.Matched;
    			recon.judgmentAction = "auto";
    			recon.judgmentBatchSize = 1;
    			
    			cell = new Cell(rc.topicName, recon);
    		} else {
    			cell = new Cell((Serializable) value, null);
    		}
    		
    		row.setCell(_firstNewCellIndex + c, cell);
    	}
    }

    public void revert(Project project) {
        synchronized (project) {
            project.rows.clear();
            project.rows.addAll(_oldRows);
            
            for (int i = 0; i < _columnNames.size(); i++) {
            	project.columnModel.columns.remove(_columnInsertIndex);
            }
            
            project.columnModel.update();
            project.recomputeRowContextDependencies();
        }
    }

    public void save(Writer writer, Properties options) throws IOException {
    	writer.write("baseColumnName="); writer.write(_baseColumnName); writer.write('\n');
    	writer.write("columnInsertIndex="); writer.write(Integer.toString(_columnInsertIndex)); writer.write('\n');
        writer.write("columnNameCount="); writer.write(Integer.toString(_columnNames.size())); writer.write('\n');
        for (String name : _columnNames) {
        	writer.write(name); writer.write('\n');
        }
        writer.write("columnTypeCount="); writer.write(Integer.toString(_columnTypes.size())); writer.write('\n');
        for (FreebaseType type : _columnTypes) {
            try {
                JSONWriter jsonWriter = new JSONWriter(writer);
                
                type.write(jsonWriter, options);
            } catch (JSONException e) {
                // ???
            }
            writer.write('\n');
        }
        writer.write("rowIndexCount="); writer.write(Integer.toString(_rowIndices.size())); writer.write('\n');
        for (Integer rowIndex : _rowIndices) {
        	writer.write(rowIndex.toString()); writer.write('\n');
        }
        writer.write("dataExtensionCount="); writer.write(Integer.toString(_dataExtensions.size())); writer.write('\n');
        for (DataExtension dataExtension : _dataExtensions) {
            writer.write(Integer.toString(dataExtension.data.length)); writer.write('\n');
            
            for (Object[] values : dataExtension.data) {
            	for (Object value : values) {
            	    if (value == null) {
            	        writer.write("null");
            	    } else if (value instanceof ReconCandidate) {
                        try {
                            JSONWriter jsonWriter = new JSONWriter(writer);
                            ((ReconCandidate) value).write(jsonWriter, options);
                        } catch (JSONException e) {
                            // ???
                        }
            		} else if (value instanceof String) {
            			writer.write(JSONObject.quote((String) value));
            		} else {
            		    writer.write(value.toString());
            		}
            		writer.write('\n');
            	}
            }
        }
        
    	writer.write("firstNewCellIndex="); writer.write(Integer.toString(_firstNewCellIndex)); writer.write('\n');
    	
        writer.write("newRowCount="); writer.write(Integer.toString(_newRows.size())); writer.write('\n');
        for (Row row : _newRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("oldRowCount="); writer.write(Integer.toString(_oldRows.size())); writer.write('\n');
        for (Row row : _oldRows) {
            row.save(writer, options);
            writer.write('\n');
        }
        writer.write("/ec/\n"); // end of change marker
    }
    
    static public Change load(LineNumberReader reader, Pool pool) throws Exception {
    	String baseColumnName = null;
    	int columnInsertIndex = -1;
    	
    	List<String> columnNames = null;
    	List<FreebaseType> columnTypes = null;
    	
    	List<Integer> rowIndices = null;
    	List<DataExtension> dataExtensions = null;
    	
        List<Row> oldRows = null;
        List<Row> newRows = null;
        
        int firstNewCellIndex = -1;
        
        String line;
        while ((line = reader.readLine()) != null && !"/ec/".equals(line)) {
            int equal = line.indexOf('=');
            CharSequence field = line.subSequence(0, equal);
            String value = line.substring(equal + 1);
            
            if ("baseColumnName".equals(field)) {
            	baseColumnName = value;
            } else if ("columnInsertIndex".equals(field)) {
            	columnInsertIndex = Integer.parseInt(value);
            } else if ("firstNewCellIndex".equals(field)) {
            	firstNewCellIndex = Integer.parseInt(value);
            } else if ("rowIndexCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                rowIndices = new ArrayList<Integer>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        rowIndices.add(Integer.parseInt(line));
                    }
                }
            } else if ("columnNameCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                columnNames = new ArrayList<String>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        columnNames.add(line);
                    }
                }
            } else if ("columnTypeCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                columnTypes = new ArrayList<FreebaseType>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    columnTypes.add(FreebaseType.load(ParsingUtilities.evaluateJsonStringToObject(line)));
                }
            } else if ("dataExtensionCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                dataExtensions = new ArrayList<DataExtension>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    
                    if (line == null) continue;
                    
                    int rowCount = Integer.parseInt(line);
                    Object[][] data = new Object[rowCount][];
                    
                    for (int r = 0; r < rowCount; r++) {
                    	Object[] row = new Object[columnNames.size()];
                    	for (int c = 0; c < columnNames.size(); c++) {
                    		line = reader.readLine();
                    		
                    		row[c] = ReconCandidate.loadStreaming(line);
                    	}
                    	
                    	data[r] = row;
                    }
                    
                    dataExtensions.add(new DataExtension(data));
                }
            } else if ("oldRowCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                oldRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        oldRows.add(Row.load(line, pool));
                    }
                }
            } else if ("newRowCount".equals(field)) {
                int count = Integer.parseInt(value);
                
                newRows = new ArrayList<Row>(count);
                for (int i = 0; i < count; i++) {
                    line = reader.readLine();
                    if (line != null) {
                        newRows.add(Row.load(line, pool));
                    }
                }
            }

        }
        
        DataExtensionChange change = new DataExtensionChange(
    		baseColumnName, 
    		columnInsertIndex, 
    		columnNames,
    		columnTypes,
    		rowIndices,
    		dataExtensions,
    		firstNewCellIndex,
    		oldRows,
    		newRows
        );
        
        
        return change;
    }
}
