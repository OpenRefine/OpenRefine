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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.TimeRangeFacetAggregator;
import org.openrefine.browsing.util.TimeRangeFacetState;
import org.openrefine.browsing.util.TimeRangeStatistics;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeRangeFacet implements Facet {

	private static int _maxBinCount = 100; // not configurable so far
    protected static final String MIN = "min";
    protected static final String MAX = "max";
    protected static final String TO = "to";
    protected static final String FROM = "from";
    
    /*
     * Configuration, from the client side
     */
    public static class TimeRangeFacetConfig implements FacetConfig, Serializable {

		private static final long serialVersionUID = 2274104024064447248L;
		@JsonProperty("name")
        protected String     _name;       // name of facet
        @JsonProperty("expression")
        protected String     _expression; // expression to compute numeric value(s) per row
        @JsonProperty("columnName")
        protected String     _columnName; // column to base expression on, if any
        
        private double      _from = 0; // the numeric selection
		private double      _to = 0;
        
		private boolean   _selectTime; // whether the time selection applies, default true
        private boolean   _selectNonTime;
        private boolean   _selectBlank;
        private boolean   _selectError;
        @JsonIgnore
        protected Evaluable _evaluable;
        @JsonIgnore
        protected String _errorMessage;
        
        @JsonCreator
        public TimeRangeFacetConfig(
        		@JsonProperty("name")
        		String name,
        		@JsonProperty("expression")
        		String expression,
        		@JsonProperty("columnName")
        		String columnName,
        		@JsonProperty(FROM)
        		double from,
        		@JsonProperty(TO)
        		double to,
        		@JsonProperty("selectTime")
        		boolean selectTime,
        		@JsonProperty("selectNonTime")
        		boolean selectNonTime,
        		@JsonProperty("selectBlank")
        		boolean selectBlank,
        		@JsonProperty("selectError")
        		boolean selectError) {
        	_name = name;
        	_expression = expression;
        	_columnName = columnName;
        	_from = from;
        	setTo(to);
        	_selectTime = selectTime;
        	_selectNonTime = selectNonTime;
        	_selectBlank = selectBlank;
        	_selectError = selectError;
        	try {
	            _evaluable = MetaParser.parse(expression);
	            _errorMessage = null;
	        } catch (ParsingException e) {
	            _errorMessage = e.getMessage();
	            _evaluable = null;
	        }
        }
        
        // false if we're certain that all rows will match
        // and there isn't any filtering to do
        // @todo inline
        @JsonIgnore
        protected boolean isSelected() {
            return !isNeutral();
        }; 
        
        @Override
        public TimeRangeFacet apply(ColumnModel columnModel) {
        	int cellIndex = columnModel.getColumnIndexByName(_columnName);
            return new TimeRangeFacet(this, cellIndex);
        }

        @Override
        public String getJsonType() {
            return "core/timerange";
        }

		@Override
		public Set<String> getColumnDependencies() {
			if (_evaluable == null) {
				return null;
			}
			return _evaluable.getColumnDependencies(_columnName);
		}

		@Override
		public TimeRangeFacetConfig renameColumnDependencies(Map<String, String> substitutions) {
			if (_errorMessage != null) {
				return null;
			}
			Evaluable translated = _evaluable.renameColumnDependencies(substitutions);
			if (translated == null) {
				return null;
			}
			return new TimeRangeFacetConfig(
					_name,
					translated.getFullSource(),
					substitutions.getOrDefault(_columnName, _columnName),
					getFrom(),
					getTo(),
					getSelectTime(),
					getSelectNonTime(),
					getSelectBlank(),
					getSelectError());
		}

		@Override
		public boolean isNeutral() {
			return _errorMessage != null ||
					(getFrom() == 0 && getTo() == 0 && getSelectTime() && getSelectNonTime() && getSelectBlank() && _selectError);
		}

		@JsonProperty("selectTime")
		public boolean getSelectTime() {
			return _selectTime;
		}
		
		@JsonProperty("selectNonTime")
		public boolean getSelectNonTime() {
			return _selectNonTime;
		}

		@JsonProperty("selectBlank")
		public boolean getSelectBlank() {
			return _selectBlank;
		}
		
		@JsonProperty("selectError")
		public boolean getSelectError() {
			return _selectError;
		}

		@JsonProperty(FROM)
		public double getFrom() {
			return _from;
		}
		
		public void setFrom(double from) {
			_from = from;
		}
		
		@JsonProperty(TO)
		public double getTo() {
			return _to;
		}

		public void setTo(double to) {
			_to = to;
		}
		
    }
    protected TimeRangeFacetConfig _config;
    
    /*
     * Derived configuration data
     */
    protected int        _cellIndex;
    protected String     _errorMessage;
    
    public TimeRangeFacet(TimeRangeFacetConfig config, int cellIndex) {
    	_config = config;
    	_cellIndex = cellIndex;
    	_errorMessage = cellIndex == -1 ? "No column named " + _config._columnName : config._errorMessage;
    }

	@Override
	public FacetConfig getConfig() {
		return _config;
	}

	@Override
	public FacetState getInitialFacetState() {
		return new TimeRangeFacetState(
				new TimeRangeStatistics(0, 0, 0, 0, new long[] {}),
				new TimeRangeStatistics(0, 0, 0, 0, new long[] {}));
	}

	@Override
	public FacetAggregator<TimeRangeFacetState> getAggregator() {
		if (_errorMessage == null) {
			return new TimeRangeFacetAggregator(
					_config,
					false,
					new ExpressionBasedRowEvaluable(_config._columnName, _cellIndex, _config._evaluable));
		} else {
			return null;
		}
	}

	@Override
	public FacetResult getFacetResult(FacetState state) {
		return new TimeRangeFacetResult(_config, _errorMessage, (TimeRangeFacetState) state);
	}

}
