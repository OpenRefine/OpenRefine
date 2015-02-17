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

package com.google.refine.freebase.model.changes;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.freebase.FreebaseType;
import com.google.refine.freebase.model.recon.DataExtensionReconConfig;
import com.google.refine.freebase.util.FreebaseDataExtensionJob.DataExtension;
import com.google.refine.history.Change;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Recon;
import com.google.refine.model.Recon.Judgment;
import com.google.refine.model.ReconCandidate;
import com.google.refine.model.ReconStats;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

public class DataExtensionChange implements Change {
    final protected String              _baseColumnName;
    final protected int                 _columnInsertIndex;
    
    final protected List<String>        _columnNames;
    final protected List<FreebaseType>  _columnTypes;
    
    final protected List<Integer>       _rowIndices;
    final protected List<DataExtension> _dataExtensions;
    
    final protected String              _reconServiceUrl;
    final protected String              _identifierSpace;
    final protected String              _schemaSpace;

    protected long                      _historyEntryID;
    protected int                       _firstNewCellIndex = -1;
    protected List<Row>                 _oldRows;
    protected List<Row>                 _newRows;
    
    public DataExtensionChange(
        String baseColumnName, 
        int columnInsertIndex, 
        List<String> columnNames,
        List<FreebaseType> columnTypes,
        List<Integer> rowIndices,
        List<DataExtension> dataExtensions,
        String reconServiceUrl,
        String identifierSpace,
        String schemaSpace,
        long historyEntryID
        ) {
        _baseColumnName = baseColumnName;
        _columnInsertIndex = columnInsertIndex;
        
        _columnNames = columnNames;
        _columnTypes = columnTypes;
        
        _rowIndices = rowIndices;
        _dataExtensions = dataExtensions;

        _reconServiceUrl = reconServiceUrl;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;
        
        _historyEntryID = historyEntryID;
        }

    protected DataExtensionChange(
        String              baseColumnName, 
        int                 columnInsertIndex,
        
        List<String>        columnNames,
        List<FreebaseType> columnTypes,
        
        List<Integer>       rowIndices,
        List<DataExtension> dataExtensions,
        String reconServiceUrl,
        String identifierSpace,
        String schemaSpace,
        int                 firstNewCellIndex,
        List<Row>           oldRows,
        List<Row>           newRows
    ) {
        _baseColumnName = baseColumnName;
        _columnInsertIndex = columnInsertIndex;
        
        _columnNames = columnNames;
        _columnTypes = columnTypes;
        
        _rowIndices = rowIndices;
        _dataExtensions = dataExtensions;
        
        _reconServiceUrl = reconServiceUrl;
        _identifierSpace = identifierSpace;
        _schemaSpace = schemaSpace;

        _firstNewCellIndex = firstNewCellIndex;
        _oldRows = oldRows;
        _newRows = newRows;
    }

    @Override
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
                
                int rowIndex = index < _rowIndices.size() ? _rowIndices.get(index) : _oldRows.size();
                DataExtension dataExtension = index < _rowIndices.size() ? _dataExtensions.get(index) : null;
                
                index++;
                
                Map<String, Recon> reconMap = new HashMap<String, Recon>();
                
                for (int r = 0; r < _oldRows.size(); r++) {
                    Row oldRow = _oldRows.get(r);
                    if (r < rowIndex) {
                        _newRows.add(oldRow.dup());
                        continue;
                    }
                    
                    if (dataExtension == null || dataExtension.data.length == 0) {
                        // Rows without a MQL result
                        _newRows.add(oldRow);
                    } else {
                        // Rows with a MQL result
                        Row firstNewRow = oldRow.dup();
                        extendRow(firstNewRow, dataExtension, 0, reconMap);
                        _newRows.add(firstNewRow);
                        
                        int r2 = r + 1;
                        for (int subR = 1; subR < dataExtension.data.length; subR++) {
                            // Rows with more than one MQL result
                            if (r2 < project.rows.size()) {
                                Row oldRow2 = project.rows.get(r2);
                                if (oldRow2.isCellBlank(cellIndex) && 
                                    oldRow2.isCellBlank(keyCellIndex)) {
                                    
                                    Row newRow = oldRow2.dup();
                                    extendRow(newRow, dataExtension, subR, reconMap);
                                    
                                    _newRows.add(newRow);
                                    r2++;
                                    
                                    continue;
                                }
                            }
                            
                            Row newRow = new Row(cellIndex + _columnNames.size());
                            extendRow(newRow, dataExtension, subR, reconMap);
                            
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
                
                try {
                    project.columnModel.addColumn(_columnInsertIndex + i, column, true);

                    // the column might have been renamed to avoid collision
                    _columnNames.set(i, column.getName());
                } catch (ModelException e) {
                    // won't get here since we set the avoid collision flag
                }
            }
            
            project.update();
        }
    }
    
    protected void extendRow(
        Row row, 
        DataExtension dataExtension, 
        int extensionRowIndex,
        Map<String, Recon> reconMap
    ) {
        Object[] values = dataExtension.data[extensionRowIndex];
        for (int c = 0; c < values.length; c++) {
            Object value = values[c];
            Cell cell = null;
            
            if (value instanceof ReconCandidate) {
                ReconCandidate rc = (ReconCandidate) value;
                Recon recon;
                if (reconMap.containsKey(rc.id)) {
                    recon = reconMap.get(rc.id);
                } else {
                    recon = new Recon(_historyEntryID, _identifierSpace, _schemaSpace);
                    recon.service = _reconServiceUrl;
                    recon.addCandidate(rc);
                    recon.match = rc;
                    recon.matchRank = 0;
                    recon.judgment = Judgment.Matched;
                    recon.judgmentAction = "auto";
                    recon.judgmentBatchSize = 1;
                    
                    reconMap.put(rc.id, recon);
                }
                cell = new Cell(rc.name, recon);
            } else {
                cell = new Cell((Serializable) value, null);
            }
            
            row.setCell(_firstNewCellIndex + c, cell);
        }
    }

    @Override
    public void revert(Project project) {
        synchronized (project) {
            project.rows.clear();
            project.rows.addAll(_oldRows);
            
            for (int i = 0; i < _columnNames.size(); i++) {
                project.columnModel.columns.remove(_columnInsertIndex);
            }
            
            project.update();
        }
    }

    @Override
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
            if (dataExtension == null) {
                writer.write('\n');
                continue;
            }
            
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
        
        String reconServiceUrl = null;
        String identifierSpace = null;
        String schemaSpace = null;

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
                    
                    if (line == null) {
                        continue;
                    }
                    
                    if (line.length() == 0) {
                        dataExtensions.add(null);
                        continue;
                    }
                    
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
            } else if ("reconServiceUrl".equals(field)) {
                reconServiceUrl = value;
            } else if ("identifierSpace".equals(field)) {
                identifierSpace = value;
            } else if ("schemaSpace".equals(field)) {
                schemaSpace = value;
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
            reconServiceUrl,
            identifierSpace,
            schemaSpace,
            firstNewCellIndex,
            oldRows,
            newRows
        );
        
        
        return change;
    }
}
