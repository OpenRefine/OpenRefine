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
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionStringComparisonRowFilter;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.ast.VariableExpr;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class TextSearchFacet implements Facet {
    /*
     *  Configuration
     */
    protected String     _name;
    protected String     _columnName;
    protected String     _query;
    protected String     _mode;
    protected boolean    _caseSensitive;
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
    protected Pattern    _pattern;
    
    public TextSearchFacet() {
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("columnName"); writer.value(_columnName);
        writer.key("query"); writer.value(_query);
        writer.key("mode"); writer.value(_mode);
        writer.key("caseSensitive"); writer.value(_caseSensitive);
        writer.endObject();
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws JSONException {
        _name = o.getString("name");
        _columnName = o.getString("columnName");
        
        Column column = project.columnModel.getColumnByName(_columnName);
        _cellIndex = column != null ? column.getCellIndex() : -1;
        
        if (!o.isNull("query")) {
            _query = o.getString("query"); 
        }
        
        _mode = o.getString("mode");
        _caseSensitive = o.getBoolean("caseSensitive");
        if (_query != null) {
            if ("regex".equals(_mode)) {
                try {
                    _pattern = Pattern.compile(
                            _query, 
                            _caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } catch (java.util.regex.PatternSyntaxException e) {
                    throw new JSONException(e);
                }
            } else if (!_caseSensitive) {
                _query = _query.toLowerCase();
            }
        }
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        if (_query == null || _query.length() == 0 || _cellIndex < 0) {
            return null;
        } else if ("regex".equals(_mode) && _pattern == null) {
            return null;
        }
        
        Evaluable eval = new VariableExpr("value");
        
        if ("regex".equals(_mode)) {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                @Override
                protected boolean checkValue(String s) {
                    return _pattern.matcher(s).find();
                };
            };
        } else {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                @Override
                protected boolean checkValue(String s) {
                    return (_caseSensitive ? s : s.toLowerCase()).contains(_query);
                };
            };
        }        
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        // nothing to do
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        // nothing to do
    }
}
