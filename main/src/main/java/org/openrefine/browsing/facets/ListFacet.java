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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.util.ExpressionBasedRowEvaluable;
import org.openrefine.browsing.util.StringValuesFacetAggregator;
import org.openrefine.browsing.util.StringValuesFacetState;
import org.openrefine.expr.Evaluable;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.ParsingException;
import org.openrefine.model.ColumnModel;
import org.openrefine.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ListFacet implements Facet {
    public static final String ERR_TOO_MANY_CHOICES = "Too many choices";
    
    /**
     * Wrapper to respect the serialization format
     */
    public static class DecoratedValueWrapper {
        @JsonProperty("v")
        public final DecoratedValue value;
        @JsonCreator
        public DecoratedValueWrapper(
                @JsonProperty("v") DecoratedValue value) {
            this.value = value;
        }
    }
    
    /*
     * Configuration
     */
    public static class ListFacetConfig implements FacetConfig {
        @JsonProperty("name")
        public String     name;
        // accessors defined below
		private String     expression;
		private Evaluable  evaluable = null;
		private String     errorMessage = null;
		
        @JsonProperty("columnName")
        public String     columnName;
        @JsonProperty("invert")
        public boolean    invert;
        
        // If true, then facet won't show the blank and error choices
        @JsonProperty("omitBlank")
        public boolean omitBlank;
        @JsonProperty("omitError")
        public boolean omitError;
        
        @JsonIgnore
        public List<DecoratedValue> selection = new LinkedList<>();
        @JsonProperty("selectBlank")
        public boolean selectBlank;
        @JsonProperty("selectError")
        public boolean selectError;

        @JsonProperty("selection")
        public List<DecoratedValueWrapper> getWrappedSelection() {
            return selection.stream()
                    .map(e -> new DecoratedValueWrapper(e))
                    .collect(Collectors.toList());
        }
        
        @JsonProperty("selection")
        public void setSelection(List<DecoratedValueWrapper> wrapped) {
            selection = wrapped.stream()
                    .map(e -> e.value)
                    .collect(Collectors.toList());
        }
        
        @Override
        public ListFacet apply(ColumnModel columnModel) {
            return new ListFacet(this, columnModel);
        }

        @Override
        public String getJsonType() {
            return "core/list";
        }

		@Override
		public Set<String> getColumnDependencies() {
			if (evaluable == null) {
				return null;
			}
			return evaluable.getColumnDependencies(columnName);
		}
	    
		@Override
		public ListFacetConfig renameColumnDependencies(Map<String, String> substitutions) {
			if (errorMessage != null) {
				return null;
			}
			Evaluable translated = evaluable.renameColumnDependencies(substitutions);
			if (translated == null) {
				return null;
			}
			ListFacetConfig newConfig = new ListFacetConfig();
			newConfig.columnName = substitutions.getOrDefault(columnName, columnName);
			newConfig.setExpression(translated.getFullSource());
			newConfig.invert = invert;
			newConfig.name = name;
			newConfig.omitBlank = omitBlank;
			newConfig.omitError = omitError;
			newConfig.selectBlank = selectBlank;
			newConfig.selectError = selectError;
			newConfig.selection = selection;
			return newConfig;
		}

		@Override
		public boolean isNeutral() {
			return selection.size() == 0 && !selectBlank && !selectError;
		}

		@JsonProperty("expression")
		public String getExpression() {
			return expression;
		}
		
		@JsonProperty("expression")
		public void setExpression(String expression) {
			this.expression = expression;
			try {
	            evaluable = MetaParser.parse(expression);
	        } catch (ParsingException e) {
	            errorMessage = e.getMessage();
	        }
		}
		
		@JsonIgnore
		public Evaluable getEvaluable() {
			return evaluable;
		}
		
		@JsonIgnore
		public String getErrorMessage() {
			return errorMessage;
		}
    }
    
    final ListFacetConfig _config;
    final ColumnModel _columnModel;
    
    /*
     * Derived configuration
     */
    protected int        _cellIndex;
    protected Evaluable  _eval;
    protected String     _errorMessage;
    
    public ListFacet(ListFacetConfig config, ColumnModel model) {
    	_config = config;
    	_columnModel = model;
    	
        if (_config.columnName.length() > 0) {
            _cellIndex = _columnModel.getColumnIndexByName(_config.columnName);
            if (_cellIndex == -1) {
            	_errorMessage = "No column named " + _config.columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        _eval = _config.getEvaluable();
        if (_cellIndex != -1) {
        	_errorMessage = _config.getErrorMessage();
        }
    }
    
    protected Object[] createMatches() {
        Object[] a = new Object[_config.selection.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = _config.selection.get(i).value;
        }
        return a;
    }
    
	@Override
	public StringValuesFacetState getInitialFacetState() {
		return new StringValuesFacetState();
	}

	@Override
	public ListFacetResult getFacetResult(FacetState state) {
		if (_errorMessage == null) {
			return new ListFacetResult(_config, (StringValuesFacetState) state);
		} else {
			return new ListFacetResult(_config, _errorMessage);
		}
	}

	@Override
	public FacetAggregator<StringValuesFacetState> getAggregator() {
		if (_errorMessage == null) {
			return new StringValuesFacetAggregator(_columnModel, _cellIndex, new ExpressionBasedRowEvaluable(_config.columnName, _cellIndex, _eval),
					Arrays.stream(createMatches()).map(o -> StringUtils.toString(o))
					.collect(Collectors.toSet()), _config.selectBlank, _config.selectError, _config.invert);
		} else {
			return null;
		}
	}

	@Override
	public ListFacetConfig getConfig() {
		return _config;
	}
}
