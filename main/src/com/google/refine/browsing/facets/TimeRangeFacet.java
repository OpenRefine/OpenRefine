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

package com.google.refine.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.ExpressionTimeComparisonRowFilter;
import com.google.refine.browsing.util.ExpressionTimeValueBinner;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.browsing.util.TimeBinIndex;
import com.google.refine.browsing.util.TimeBinRecordIndex;
import com.google.refine.browsing.util.TimeBinRowIndex;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class TimeRangeFacet extends RangeFacet {

    protected boolean   _selectTime; // whether the time selection applies, default true
    protected boolean   _selectNonTime;
            
    protected int       _baseTimeCount;
    protected int       _baseNonTimeCount;
    
    protected int       _timeCount;
    protected int       _nonTimeCount;
    
    @Override
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

    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws JSONException {
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

    @Override
    public RowFilter getRowFilter(Project project) {
        if (_eval != null && _errorMessage == null && _selected) {
            return new ExpressionTimeComparisonRowFilter(
                    getRowEvaluable(project), _selectTime, _selectNonTime, _selectBlank, _selectError) {
                
                @Override
                protected boolean checkValue(long t) {
                    return t >= _from && t <= _to;
                };
            };
        } else {
            return null;
        }
    }

    @Override
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
    
    @Override
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
