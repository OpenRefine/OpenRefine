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

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.ProjectManager;
import com.google.refine.browsing.DecoratedValue;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AllRowsRecordFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionEqualRowFilter;
import com.google.refine.browsing.util.ExpressionNominalValueGrouper;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

public class ListFacet implements Facet {
    /*
     * Configuration
     */
    public static class ListFacetConfig implements FacetConfig {
        public String     name;
        public String     expression;
        public String     columnName;
        public boolean    invert;
        
        // If true, then facet won't show the blank and error choices
        public boolean omitBlank;
        public boolean omitError;
        
        public List<DecoratedValue> selection = new LinkedList<>();
        public boolean selectBlank;
        public boolean selectError;

        @Override
        public void write(JSONWriter writer, Properties options)
                throws JSONException {
            writer.object();
            writer.key("type"); writer.value("list");
            writer.key("name"); writer.value(name);
            writer.key("expression"); writer.value(expression);
            writer.key("columnName"); writer.value(columnName);
            writer.key("invert"); writer.value(invert);
            writer.key("selection"); writer.array();
            for (DecoratedValue choice : selection) {
                writer.object();
                writer.key("v");
                choice.write(writer, options);
                writer.endObject();
            }
            writer.endArray();
            writer.key("omitBlank"); writer.value(omitBlank);
            writer.key("selectBlank"); writer.value(selectBlank);
            writer.key("omitError"); writer.value(omitError);
            writer.key("selectError"); writer.value(selectError);
            writer.endObject();
        }
        
        @Override
        public void initializeFromJSON(JSONObject o) {
            name = o.getString("name");
            expression = o.getString("expression");
            columnName = o.getString("columnName");
            invert = o.has("invert") && o.getBoolean("invert");
                      
            JSONArray a = o.getJSONArray("selection");
            int length = a.length();
            
            for (int i = 0; i < length; i++) {
                JSONObject oc = a.getJSONObject(i);
                JSONObject ocv = oc.getJSONObject("v");
                
                DecoratedValue decoratedValue = new DecoratedValue(
                    ocv.get("v"), ocv.getString("l"));
                
                selection.add(decoratedValue);
            }
            
            omitBlank = JSONUtilities.getBoolean(o, "omitBlank", false);
            omitError = JSONUtilities.getBoolean(o, "omitError", false);
            
            selectBlank = JSONUtilities.getBoolean(o, "selectBlank", false);
            selectError = JSONUtilities.getBoolean(o, "selectError", false);
        }
        
        @Override
        public Facet apply(Project project) {
            ListFacet facet = new ListFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }
    }
    
    ListFacetConfig _config = new ListFacetConfig();
    
    /*
     * Derived configuration
     */
    protected int        _cellIndex;
    protected Evaluable  _eval;
    protected String     _errorMessage;
    
    /*
     * Computed results
     */
    protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
    protected int _blankCount;
    protected int _errorCount;
    
    public ListFacet() {
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_config.name);
        writer.key("expression"); writer.value(_config.expression);
        writer.key("columnName"); writer.value(_config.columnName);
        writer.key("invert"); writer.value(_config.invert);
        
        if (_errorMessage != null) {
            writer.key("error"); writer.value(_errorMessage);
        } else if (_choices.size() > getLimit()) {
            writer.key("error"); writer.value("Too many choices");
            writer.key("choiceCount"); writer.value(_choices.size());
        } else {
            writer.key("choices"); writer.array();
            for (NominalFacetChoice choice : _choices) {
                choice.write(writer, options);
            }
            writer.endArray();
            
            if (!_config.omitBlank && (_config.selectBlank || _blankCount > 0)) {
                writer.key("blankChoice");
                writer.object();
                writer.key("s"); writer.value(_config.selectBlank);
                writer.key("c"); writer.value(_blankCount);
                writer.endObject();
            }
            if (!_config.omitError && (_config.selectError || _errorCount > 0)) {
                writer.key("errorChoice");
                writer.object();
                writer.key("s"); writer.value(_config.selectError);
                writer.key("c"); writer.value(_errorCount);
                writer.endObject();
            }
        }
        
        writer.endObject();
    }
    
    protected int getLimit() {
        Object v = ProjectManager.singleton.getPreferenceStore().get("ui.browsing.listFacet.limit");
        if (v != null) {
            if (v instanceof Number) {
                return ((Number) v).intValue();
            } else {
                try {
                    int n = Integer.parseInt(v.toString());
                    return n;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return 2000;
    }
        
    public void initializeFromConfig(ListFacetConfig config, Project project) {
        _config = config;
        if (_config.columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_config.columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _config.columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_config.expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        return 
            _eval == null || 
            _errorMessage != null ||
            (_config.selection.size() == 0 && !_config.selectBlank && !_config.selectError) ? 
                null :
                new ExpressionEqualRowFilter(
                    _eval, 
                    _config.columnName,
                    _cellIndex, 
                    createMatches(), 
                    _config.selectBlank, 
                    _config.selectError,
                    _config.invert);
    }
    
    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null :
            (_config.invert ?
                    new AllRowsRecordFilter(rowFilter) :
                        new AnyRowRecordFilter(rowFilter));
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (_eval != null && _errorMessage == null) {
            ExpressionNominalValueGrouper grouper = 
                new ExpressionNominalValueGrouper(_eval, _config.columnName, _cellIndex);
            
            filteredRows.accept(project, grouper);
            
            postProcessGrouper(grouper);
        }
    }
    
    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        if (_eval != null && _errorMessage == null) {
            ExpressionNominalValueGrouper grouper = 
                new ExpressionNominalValueGrouper(_eval, _config.columnName, _cellIndex);
            
            filteredRecords.accept(project, grouper);
            
            postProcessGrouper(grouper);
        }
    }
    
    protected void postProcessGrouper(ExpressionNominalValueGrouper grouper) {
        _choices.clear();
        _choices.addAll(grouper.choices.values());
        
        for (DecoratedValue decoratedValue : _config.selection) {
            String valueString = decoratedValue.value.toString();
            
            if (grouper.choices.containsKey(valueString)) {
                grouper.choices.get(valueString).selected = true;
            } else {
                /*
                 *  A selected choice can have zero count if it is selected together
                 *  with other choices, and some other facets' constraints eliminate
                 *  all rows projected to this choice altogether. For example, if you
                 *  select both "car" and "bicycle" in the "type of vehicle" facet, and
                 *  then constrain the "wheels" facet to more than 2, then the "bicycle"
                 *  choice now has zero count even if it's still selected. The grouper 
                 *  won't be able to detect the "bicycle" choice, so we need to inject
                 *  that choice into the choice list ourselves.
                 */
                NominalFacetChoice choice = new NominalFacetChoice(decoratedValue);
                choice.count = 0;
                _choices.add(choice);
            }
        }
        
        _blankCount = grouper.blankCount;
        _errorCount = grouper.errorCount;
    }
    
    protected Object[] createMatches() {
        Object[] a = new Object[_config.selection.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = _config.selection.get(i).value;
        }
        return a;
    }
}
