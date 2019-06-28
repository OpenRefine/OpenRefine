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
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
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

public class TextListFacet extends ListFacet {
    
    public static class TextListFacetConfig extends ListFacetConfig {
        @JsonProperty("selectString")
        public boolean selectString;
        @JsonProperty("selectNumeric")
        public boolean selectNumeric;
        @JsonProperty("selectDateTime")
        public boolean selectDateTime;
        @JsonProperty("selectBoolean")
        public boolean selectBoolean;
        
        @Override
        public String getJsonType() {
            return "textlist";
        }
    }
    
    /*
     * Computed results
     */
    protected int _stringCount;
    protected int _numericCount;
    protected int _datetimeCount;
    protected int _booleanCount;
    
    TextListFacetConfig _config = new TextListFacetConfig();
    
    public TextListFacet() {
    }
    
    @Override
    public RowFilter getRowFilter(Project project) {
        return 
            _eval == null || 
            _errorMessage != null ||
            (_config.selection.size() == 0 && !_config.selectBlank && !_config.selectError && !_config.selectString && !_config.selectNumeric && !_config.selectDateTime && !_config.selectBoolean) ? 
                null :
                new ExpressionEqualRowFilter(
                    _eval, 
                    _config.columnName,
                    _cellIndex, 
                    createMatches(),
                    _config.selectString,
                    _config.selectNumeric,
                    _config.selectDateTime,
                    _config.selectBoolean,
                    _config.selectBlank, 
                    _config.selectError,
                    _config.invert);
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
    
    @Override
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
                choice.selected = true;
                _choices.add(choice);
            }
        }
        
        _stringCount = grouper.stringCount;
        _numericCount = grouper.numericCount;
        _datetimeCount = grouper.datetimeCount;
        _booleanCount = grouper.booleanCount;
        _blankCount = grouper.blankCount;
        _errorCount = grouper.errorCount;
    }
}
