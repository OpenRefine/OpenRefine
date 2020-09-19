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

package org.openrefine.browsing.facets;

import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.HistogramState;
import org.openrefine.browsing.util.NumericFacetAggregator;
import org.openrefine.browsing.util.NumericFacetState;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RangeFacet implements Facet {
    
    public static final String ERR_NO_NUMERIC_VALUE_PRESENT = "No numeric value present.";
    protected static final String MIN = "min";
    protected static final String MAX = "max";
    protected static final String TO = "to";
    protected static final String FROM = "from";
    
    /*
     * Configuration, from the client side
     */
    public static class RangeFacetConfig implements FacetConfig {
        @JsonProperty("name")
        protected String     _name;       // name of facet
        @JsonProperty("expression")
        protected String     _expression; // expression to compute numeric value(s) per row
        @JsonProperty("columnName")
        protected String     _columnName; // column to base expression on, if any
        
        @JsonProperty(FROM)
        protected double    _from; // the numeric selection
        @JsonProperty(TO)
        protected double    _to;
        
        @JsonProperty("selectNumeric")
        protected boolean   _selectNumeric; // whether the numeric selection applies, default true
        @JsonProperty("selectNonNumeric")
        protected boolean   _selectNonNumeric;
        @JsonProperty("selectBlank")
        protected boolean   _selectBlank;
        @JsonProperty("selectError")
        protected boolean   _selectError;
        
        // derived configuration
        
        @JsonIgnore
        protected boolean    _selected; // false if we're certain that all rows will match
                        // and there isn't any filtering to do
        @JsonIgnore
        protected Evaluable  _evaluable;
        @JsonIgnore
        protected String     _errorMessage;
        
        @JsonCreator
        public RangeFacetConfig(
                @JsonProperty("name")
                String name,
                @JsonProperty("expression")
                String expression,
                @JsonProperty("columnName")
                String columnName,
                @JsonProperty(FROM)
                Double from,
                @JsonProperty(TO)
                Double to,
                @JsonProperty("selectNumeric")
                Boolean selectNumeric,
                @JsonProperty("selectNonNumeric")
                Boolean selectNonNumeric,
                @JsonProperty("selectBlank")
                Boolean selectBlank,
                @JsonProperty("selectError")
                Boolean selectError) {
            _name = name;
            _expression = expression;
            _columnName = columnName;
            _from = from == null ? 0 : from;
            _to = to == null ? 0 : to;
            _selectNumeric = selectNumeric == null ? true : selectNumeric;
            _selectNonNumeric = selectNonNumeric == null ? true : selectNonNumeric;
            _selectBlank = selectBlank == null ? true : selectBlank;
            _selectError = selectError == null ? true : selectError;
            _selected = !_selectNumeric || !_selectNonNumeric || !_selectBlank || !_selectError || from != null || to != null;
            try {
	            _evaluable = MetaParser.parse(expression);
	            _errorMessage = null;
	        } catch (ParsingException e) {
	            _errorMessage = e.getMessage();
	            _evaluable = null;
	        }
        }
        
        @Override
        public RangeFacet apply(ColumnModel columnModel) {
        	int cellIndex = columnModel.getColumnIndexByName(_columnName);
            return new RangeFacet(this, cellIndex);
        }

        @Override
        public String getJsonType() {
            return "core/range";
        }

		@Override
		public Set<String> getColumnDependencies() {
			if (_evaluable == null) {
				return null;
			}
			return _evaluable.getColumnDependencies(_columnName);
		}

		@Override
		public FacetConfig renameColumnDependencies(Map<String, String> substitutions) {
			if (_errorMessage != null) {
				return null;
			}
			Evaluable translated = _evaluable.renameColumnDependencies(substitutions);
			if (translated == null) {
				return null;
			}
			return new RangeFacetConfig(
	                _name,
	                translated.getFullSource(),
	                substitutions.getOrDefault(_columnName, _columnName),
	                _from,
	                _to,
	                _selectNumeric,
	                _selectNonNumeric,
	                _selectBlank,
	                _selectError);
		}

		@Override
		public boolean isNeutral() {
			return !_selected;
		}
    }
    RangeFacetConfig _config = null;
    
    /*
     * Derived configuration data
     */
    protected int        _cellIndex;

    public RangeFacet(
    		RangeFacetConfig config,
    		int cellIndex) {
    	_config = config;
    	_cellIndex = cellIndex;
    }

	@Override
	public FacetConfig getConfig() {
		return _config;
	}

	@Override
	public NumericFacetState getInitialFacetState() {
		HistogramState emptyHistogram = new HistogramState(0, 0, 0, 0, 0);
		return new NumericFacetState(emptyHistogram, emptyHistogram);
	}

	@Override
	public FacetAggregator<NumericFacetState> getAggregator() {
		return new NumericFacetAggregator(100,
				new ExpressionBasedRowEvaluable(_config._columnName, _cellIndex, _config._evaluable),
				_config._from,
				_config._to,
				_config._selectNumeric,
				_config._selectNonNumeric,
				_config._selectBlank,
				_config._selectError,
				false, // invert not supported yet in the frontend
				!_config.isNeutral());
	}

	@Override
	public FacetResult getFacetResult(FacetState state) {
		return new NumericFacetResult(_config, ((NumericFacetState) state).normalizeForReporting(0));
	}

    

}
