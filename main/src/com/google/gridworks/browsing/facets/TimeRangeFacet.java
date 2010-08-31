package com.google.gridworks.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.gridworks.browsing.FilteredRecords;
import com.google.gridworks.browsing.FilteredRows;
import com.google.gridworks.browsing.RowFilter;
import com.google.gridworks.browsing.filters.ExpressionTimeComparisonRowFilter;
import com.google.gridworks.browsing.util.ExpressionTimeValueBinner;
import com.google.gridworks.browsing.util.RowEvaluable;
import com.google.gridworks.browsing.util.TimeBinIndex;
import com.google.gridworks.browsing.util.TimeBinRecordIndex;
import com.google.gridworks.browsing.util.TimeBinRowIndex;
import com.google.gridworks.expr.MetaParser;
import com.google.gridworks.expr.ParsingException;
import com.google.gridworks.model.Column;
import com.google.gridworks.model.Project;
import com.google.gridworks.util.JSONUtilities;

public class TimeRangeFacet extends RangeFacet {
	    
    protected boolean   _selectTime; // whether the time selection applies, default true
    protected boolean   _selectNonTime;
            
    protected int       _baseTimeCount;
    protected int       _baseNonTimeCount;
    
    protected int       _timeCount;
    protected int       _nonTimeCount;
    
    public void write(JSONWriter writer, Properties options) throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("expression"); writer.value(_expression);
        writer.key("columnName"); writer.value(_columnName);
        
        if (_errorMessage != null) {
            writer.key("error"); writer.value(_errorMessage);
        } else {
            if (!Double.isInfinite(_min) && !Double.isInfinite(_max)) {
                writer.key(MIN); writer.value(_min);
                writer.key(MAX); writer.value(_max);
                writer.key("step"); writer.value(_step);
                                
                writer.key("bins"); writer.array();
                for (int b : _bins) {
                    writer.value(b);
                }
                writer.endArray();
                
                writer.key("baseBins"); writer.array();
                for (int b : _baseBins) {
                    writer.value(b);
                }
                writer.endArray();
                
                writer.key(FROM); writer.value(_from);
                writer.key(TO); writer.value(_to);
            }
            
            writer.key("baseTimeCount"); writer.value(_baseTimeCount);
            writer.key("baseNonTimeCount"); writer.value(_baseNonTimeCount);
            writer.key("baseBlankCount"); writer.value(_baseBlankCount);
            writer.key("baseErrorCount"); writer.value(_baseErrorCount);
            
            writer.key("timeCount"); writer.value(_timeCount);
            writer.key("nonTimeCount"); writer.value(_nonTimeCount);
            writer.key("blankCount"); writer.value(_blankCount);
            writer.key("errorCount"); writer.value(_errorCount);
        }
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _expression = o.getString("expression");
        _columnName = o.getString("columnName");
        
        if (_columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
        
        if (o.has(FROM) || o.has(TO)) {
            _from = o.has(FROM) ? o.getDouble(FROM) : _min;
            _to = o.has(TO) ? o.getDouble(TO) : _max;
            _selected = true;
        }
        
        _selectTime = JSONUtilities.getBoolean(o, "selectTime", true);
        _selectNonTime = JSONUtilities.getBoolean(o, "selectNonTime", true);
        _selectBlank = JSONUtilities.getBoolean(o, "selectBlank", true);
        _selectError = JSONUtilities.getBoolean(o, "selectError", true);
        
        if (!_selectTime || !_selectNonTime || !_selectBlank || !_selectError) {
            _selected = true;
        }
    }

    public RowFilter getRowFilter(Project project) {
        if (_eval != null && _errorMessage == null && _selected) {
            return new ExpressionTimeComparisonRowFilter(
        		getRowEvaluable(project), _selectTime, _selectNonTime, _selectBlank, _selectError) {
                
                protected boolean checkValue(long t) {
                    return t >= _from && t < _to;
                };
            };
        } else {
            return null;
        }
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (_eval != null && _errorMessage == null) {
            RowEvaluable rowEvaluable = getRowEvaluable(project);
            
            Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
            String key = "time-bin:row-based:" + _expression;
            TimeBinIndex index = (TimeBinIndex) column.getPrecompute(key);
            if (index == null) {
                index = new TimeBinRowIndex(project, rowEvaluable);
                column.setPrecompute(key, index);
            }
            
            retrieveDataFromBaseBinIndex(index);
                        
            ExpressionTimeValueBinner binner = new ExpressionTimeValueBinner(rowEvaluable, index);
            
            filteredRows.accept(project, binner);
            retrieveDataFromBinner(binner);
        }
    }
    
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        if (_eval != null && _errorMessage == null) {
            RowEvaluable rowEvaluable = getRowEvaluable(project);
            
            Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
            String key = "time-bin:record-based:" + _expression;
            TimeBinIndex index = (TimeBinIndex) column.getPrecompute(key);
            if (index == null) {
                index = new TimeBinRecordIndex(project, rowEvaluable);
                column.setPrecompute(key, index);
            }
            
            retrieveDataFromBaseBinIndex(index);
            
            ExpressionTimeValueBinner binner = new ExpressionTimeValueBinner(rowEvaluable, index);
            
            filteredRecords.accept(project, binner);
            
            retrieveDataFromBinner(binner);
        }
    }
        
    protected void retrieveDataFromBaseBinIndex(TimeBinIndex index) {
        _min = index.getMin();
        _max = index.getMax();
        _step = index.getStep();
        _baseBins = index.getBins();
        
        _baseTimeCount = index.getTimeRowCount();
        _baseNonTimeCount = index.getNonTimeRowCount();
        _baseBlankCount = index.getBlankRowCount();
        _baseErrorCount = index.getErrorRowCount();
        
        if (_selected) {
            _from = Math.max(_from, _min);
            _to = Math.min(_to, _max);
        } else {
            _from = _min;
            _to = _max;
        }
    }
    
    protected void retrieveDataFromBinner(ExpressionTimeValueBinner binner) {
        _bins = binner.bins;
        _timeCount = binner.timeCount;
        _nonTimeCount = binner.nonTimeCount;
        _blankCount = binner.blankCount;
        _errorCount = binner.errorCount;
    }
}
