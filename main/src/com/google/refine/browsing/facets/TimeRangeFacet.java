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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.FilteredRecords;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RecordFilter;
import com.google.refine.browsing.RowFilter;
import com.google.refine.browsing.filters.AnyRowRecordFilter;
import com.google.refine.browsing.filters.ExpressionTimeComparisonRowFilter;
import com.google.refine.browsing.util.ExpressionBasedRowEvaluable;
import com.google.refine.browsing.util.ExpressionTimeValueBinner;
import com.google.refine.browsing.util.RowEvaluable;
import com.google.refine.browsing.util.TimeBinIndex;
import com.google.refine.browsing.util.TimeBinRecordIndex;
import com.google.refine.browsing.util.TimeBinRowIndex;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Column;
import com.google.refine.model.Project;

public class TimeRangeFacet implements Facet {

    /*
     * Configuration, from the client side
     */
    public static class TimeRangeFacetConfig implements FacetConfig {

        @JsonProperty("name")
        protected String _name; // name of facet
        @JsonProperty("expression")
        protected String _expression; // expression to compute numeric value(s) per row
        @JsonProperty("columnName")
        protected String _columnName; // column to base expression on, if any

        @JsonProperty(FROM)
        protected double _from = 0; // the numeric selection
        @JsonProperty(TO)
        protected double _to = 0;

        @JsonProperty("selectTime")
        protected boolean _selectTime; // whether the time selection applies, default true
        @JsonProperty("selectNonTime")
        protected boolean _selectNonTime;
        @JsonProperty("selectBlank")
        protected boolean _selectBlank;
        @JsonProperty("selectError")
        protected boolean _selectError;

        // false if we're certain that all rows will match
        // and there isn't any filtering to do
        @JsonIgnore
        protected boolean isSelected() {
            return _from != 0 || _to != 0 || !_selectTime || !_selectNonTime || !_selectBlank || !_selectError;
        };

        @Override
        public TimeRangeFacet apply(Project project) {
            TimeRangeFacet facet = new TimeRangeFacet();
            facet.initializeFromConfig(this, project);
            return facet;
        }

        @Override
        public String getJsonType() {
            return "timerange";
        }
    }

    protected TimeRangeFacetConfig _config;

    /*
     * Derived configuration data
     */
    protected int _cellIndex;
    protected Evaluable _eval;
    protected String _errorMessage;

    protected double _min;
    protected double _max;
    protected double _step;
    protected int[] _baseBins;
    protected int[] _bins;

    /*
     * Computed data
     */
    @JsonProperty("baseTimeCount")
    protected int _baseTimeCount;
    @JsonProperty("baseNonTimeCount")
    protected int _baseNonTimeCount;
    @JsonProperty("baseBlankCount")
    protected int _baseBlankCount;
    @JsonProperty("baseErrorCount")
    protected int _baseErrorCount;

    @JsonProperty("timeCount")
    protected int _timeCount;
    @JsonProperty("nonTimeCount")
    protected int _nonTimeCount;
    @JsonProperty("blankCount")
    protected int _blankCount;
    @JsonProperty("errorCount")
    protected int _errorCount;

    protected static final String MIN = "min";
    protected static final String MAX = "max";
    protected static final String TO = "to";
    protected static final String FROM = "from";

    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }

    @JsonProperty("expression")
    public String getExpression() {
        return _config._expression;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }

    @JsonProperty("error")
    @JsonInclude(Include.NON_NULL)
    public String getError() {
        return _errorMessage;
    }

    @JsonProperty(MIN)
    @JsonInclude(Include.NON_NULL)
    public Double getMin() {
        if (getError() == null) {
            return _min;
        }
        return null;
    }

    @JsonProperty(MAX)
    @JsonInclude(Include.NON_NULL)
    public Double getMax() {
        if (getError() == null) {
            return _max;
        }
        return null;
    }

    @JsonProperty("step")
    @JsonInclude(Include.NON_NULL)
    public Double getStep() {
        return _step;
    }

    @JsonProperty("bins")
    @JsonInclude(Include.NON_NULL)
    public int[] getBins() {
        if (getError() == null) {
            return _bins;
        }
        return null;
    }

    @JsonProperty("baseBins")
    @JsonInclude(Include.NON_NULL)
    public int[] getBaseBins() {
        if (getError() == null) {
            return _baseBins;
        }
        return null;
    }

    @JsonProperty(FROM)
    @JsonInclude(Include.NON_NULL)
    public Double getFrom() {
        if (getError() == null) {
            return _config._from;
        }
        return null;
    }

    @JsonProperty(TO)
    @JsonInclude(Include.NON_NULL)
    public Double getTo() {
        if (getError() == null) {
            return _config._to;
        }
        return null;
    }

    public void initializeFromConfig(TimeRangeFacetConfig config, Project project) {
        _config = config;
        if (_config._columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_config._columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _config._columnName;
            }
        } else {
            _cellIndex = -1;
        }

        try {
            _eval = MetaParser.parse(_config._expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        if (_eval != null && _errorMessage == null && _config.isSelected()) {
            return new ExpressionTimeComparisonRowFilter(
                    getRowEvaluable(project), _config._selectTime, _config._selectNonTime, _config._selectBlank, _config._selectError) {

                @Override
                protected boolean checkValue(long t) {
                    return t >= _config._from && t <= _config._to;
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
            String key = "time-bin:row-based:" + _config._expression;
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
            String key = "time-bin:record-based:" + _config._expression;
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

        if (_config.isSelected()) {
            _config._from = Math.max(_config._from, _min);
            _config._to = Math.min(_config._to, _max);
        } else {
            _config._from = _min;
            _config._to = _max;
        }
    }

    protected void retrieveDataFromBinner(ExpressionTimeValueBinner binner) {
        _bins = binner.bins;
        _timeCount = binner.timeCount;
        _nonTimeCount = binner.nonTimeCount;
        _blankCount = binner.blankCount;
        _errorCount = binner.errorCount;
    }

    @Override
    public RecordFilter getRecordFilter(Project project) {
        RowFilter rowFilter = getRowFilter(project);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    protected RowEvaluable getRowEvaluable(Project project) {
        return new ExpressionBasedRowEvaluable(_config._columnName, _cellIndex, _eval);
    }
}
