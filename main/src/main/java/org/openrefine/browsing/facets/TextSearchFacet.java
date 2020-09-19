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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.browsing.filters.ExpressionStringComparisonRowFilter;
import org.openrefine.expr.Evaluable;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.RecordFilter;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.util.PatternSyntaxExceptionParser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TextSearchFacet implements Facet {
    
    /*
     *  Configuration.
     *  Because the facet does not aggregate any state, the configuration
     *  also serves as facet result.
     */
    public static class TextSearchFacetConfig implements FacetConfig, FacetResult, Serializable {  
    	
		private static final long serialVersionUID = 5974503135359500944L;
		@JsonProperty("name")
        protected final String     _name;
        @JsonProperty("columnName")
        protected final String     _columnName;
        @JsonProperty("query")
        protected final String     _query;
        @JsonProperty("mode")
        protected final String     _mode;
        @JsonProperty("caseSensitive")
        protected final boolean    _caseSensitive;
        @JsonProperty("invert")
        protected final boolean    _invert;
        
        @JsonCreator
        public TextSearchFacetConfig(
        		@JsonProperty("name")
        		String name,
        		@JsonProperty("columnName")
        		String columnName,
        		@JsonProperty("query")
        		String query,
        		@JsonProperty("mode")
        		String mode,
        		@JsonProperty("caseSensitive")
        		boolean caseSensitive,
        		@JsonProperty("invert")
        		boolean invert) {
        	_name = name;
        	_columnName = columnName;
        	_query = query;
        	_mode = mode;
        	_caseSensitive = caseSensitive;
        	_invert = invert;
        }
        
        @Override
        public TextSearchFacet apply(ColumnModel columnModel) {
        	int cellIndex = columnModel.getColumnIndexByName(_columnName);
            return new TextSearchFacet(this, cellIndex);
        }
        
        @Override
        public String getJsonType() {
            return "core/text";
        }

		@Override
		public Set<String> getColumnDependencies() {
			return Collections.singleton(_name);
		}

		@Override
		public TextSearchFacetConfig renameColumnDependencies(Map<String, String> substitutions) {
			return new TextSearchFacetConfig(
					_name,
					substitutions.getOrDefault(_columnName, _columnName),
					_query,
					_mode,
					_caseSensitive,
					_invert);
		}

		@Override
		public boolean isNeutral() {
			return _query == null || _query.length() == 0;
		}
    }
    
    protected TextSearchFacetConfig _config;
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
    protected Pattern    _pattern;
    protected String     _query; // normalized version of the query from the config
    
    public TextSearchFacet(TextSearchFacetConfig config, int cellIndex) {
    	_config = config;
    	_cellIndex = cellIndex;
    	
    	_query = _config._query;
        if (_query != null) {
            if ("regex".equals(_config._mode)) {
                try {
                    _pattern = Pattern.compile(
                            _query, 
                            _config._caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } catch (java.util.regex.PatternSyntaxException e) {
                    PatternSyntaxExceptionParser err = new PatternSyntaxExceptionParser(e);
                    throw new IllegalArgumentException(err.getUserMessage());
                }
            } else if (!_config._caseSensitive) {
                _query = _query.toLowerCase();
            }
        }
    }
    
    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }
    
    @JsonProperty("query")
    public String getQuery() {
        return _config._query;
    }
    
    @JsonProperty("mode")
    public String getMode() {
        return _config._mode;
    }
    
    @JsonProperty("caseSensitive")
    public boolean isCaseSensitive() {
        return _config._caseSensitive;
    }
    
    @JsonProperty("invert")
    public boolean isInverted() {
        return _config._invert;
    }

	@Override
	public FacetConfig getConfig() {
		return _config;
	}

	@Override
	public FacetState getInitialFacetState() {
		return new TextSearchFacetState();
	}

	@Override
	public FacetAggregator<?> getAggregator() {
		return new TextSearchAggregator(_pattern, _cellIndex, _config);
	}

	@Override
	public FacetResult getFacetResult(FacetState state) {
		return _config;
	}
	
	public static class TextSearchFacetState implements FacetState {

		private static final long serialVersionUID = 1L;
		// No state to be kept
	}
	
	public static class TextSearchAggregator implements FacetAggregator<TextSearchFacetState> {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Pattern _pattern;
		private int     _cellIndex;
		private TextSearchFacetConfig _config;
		
		public TextSearchAggregator(Pattern pattern, int cellIndex, TextSearchFacetConfig config) {
			_pattern = pattern;
			_cellIndex = cellIndex;
			_config = config;
		}

		@Override
		public TextSearchFacetState withRow(TextSearchFacetState state, long rowId, Row row) {
			return state;
		}

		@Override
		public TextSearchFacetState sum(TextSearchFacetState first, TextSearchFacetState second) {
			return first;
		}

		@Override
		public RowFilter getRowFilter() {
			if (_config.isNeutral() || _cellIndex < 0) {
	            return null;
	        } else if ("regex".equals(_config._mode) && _pattern == null) {
	            return null;
	        }
	        
	        Evaluable eval = new Evaluable() {
	        	
				private static final long serialVersionUID = 1L;

				@Override
				public Object evaluate(Properties bindings) {
					return bindings.get("value");
				}

				@Override
				public String getSource() {
					return null;
				}

				@Override
				public String getLanguagePrefix() {
					return null;
				}
	        	
	        };
	        
	        if ("regex".equals(_config._mode)) {
	            return new ExpressionStringComparisonRowFilter(eval, _config._invert, _config._columnName, _cellIndex) {
	                /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
	                protected boolean checkValue(String s) {
	                    return _pattern.matcher(s).find();
	                };
	            };
	        } else {
	            return new ExpressionStringComparisonRowFilter(eval, _config._invert, _config._columnName, _cellIndex) {
	                /**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
	                protected boolean checkValue(String s) {
	                    return (_config._caseSensitive ? s : s.toLowerCase()).contains(_config._query);
	                };
	            };
	        }
		}

		@Override
		public RecordFilter getRecordFilter() {
			RowFilter rowFilter = getRowFilter();
			if (rowFilter == null) {
				return null;
			}
			return _config._invert ? new AllRowsRecordFilter(rowFilter) : new AnyRowRecordFilter(rowFilter);
		}
		
	}
}
