package org.openrefine.browsing.facets;

import org.openrefine.browsing.facets.RangeFacet.RangeFacetConfig;
import org.openrefine.browsing.util.HistogramState;
import org.openrefine.browsing.util.NumericFacetState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class NumericFacetResult implements FacetResult {
	
	@JsonIgnore
    protected final double           _min;
    @JsonIgnore
    protected final double           _max;
    @JsonIgnore
    protected final double           _step;
	@JsonIgnore
	protected final RangeFacetConfig _config;
    @JsonIgnore
    protected final HistogramState   _allRowsHistogram;
    @JsonIgnore
    protected final HistogramState   _rowsInViewHistogram;
    
    public NumericFacetResult(RangeFacetConfig config, NumericFacetState state) {
    	_config = config;
    	_allRowsHistogram = state.getGlobalHistogram();
    	_rowsInViewHistogram = state.getViewHistogram();
    	_step = _allRowsHistogram.getBinSize();
    	_min = _allRowsHistogram.getMinBin() * _step;
    	_max = _allRowsHistogram.getMaxBin() * _step;
    }
	
    @JsonProperty("baseNumericCount")
    public long getBaseNumericCount() {
    	return _allRowsHistogram.getNumericCount();
    }
    
    @JsonProperty("baseNonNumericCount")
    public long getBaseNonNumericCount() {
    	return _allRowsHistogram.getNonNumericCount();
    }
    
    @JsonProperty("baseBlankCount")
    public long getBaseBlankCount() {
    	return _allRowsHistogram.getBlankCount();
    }
    
    @JsonProperty("baseErrorCount")
    public long getBaseErrorCount() {
    	return _allRowsHistogram.getErrorCount();
    }
    
    @JsonProperty("numericCount")
    public long getNumericCount() {
    	return _rowsInViewHistogram.getNumericCount();
    }
    
    @JsonProperty("nonNumericCount")
    public long getNonNumericCount() {
    	return _rowsInViewHistogram.getNonNumericCount();
    }
    
    @JsonProperty("blankCount")
    public long getBlankCount() {
    	return _rowsInViewHistogram.getBlankCount();
    }
    
    @JsonProperty("errorCount")
    protected long getErrorCount() {
    	return _rowsInViewHistogram.getErrorCount();
    }
    
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
        if (_config._errorMessage != null) {
            return _config._errorMessage;
        } else if (getBaseNumericCount() == 0) {
            return RangeFacet.ERR_NO_NUMERIC_VALUE_PRESENT;
        }
        return null;
    }
    
    @JsonIgnore
    public boolean isFiniteRange() {
        return !Double.isInfinite(_min) && !Double.isInfinite(_max);
    }
    
    @JsonProperty(RangeFacet.MIN)
    @JsonInclude(Include.NON_NULL)
    public Double getMin() {
        if (getError() == null) {
            return _min;
        }
        return null;
    }
    
    @JsonProperty(RangeFacet.MAX)
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
        if (getError() == null) {
            return _step;
        }
        return null;
    }
    
    @JsonProperty("bins")
    @JsonInclude(Include.NON_NULL)
    public long[] getBins() {
        if (getError() == null) {
            return _rowsInViewHistogram.getBins();
        }
        return null;
    }
    
    @JsonProperty("baseBins")
    @JsonInclude(Include.NON_NULL)
    public long[] getBaseBins() {
        if (getError() == null) {
            return _allRowsHistogram.getBins();
        }
        return null;
    }
    
    @JsonProperty(RangeFacet.FROM)
    @JsonInclude(Include.NON_NULL)
    public Double getFrom() {
        if (getError() == null) {
        	if (_config._selected) {
        		return _config._from;
        	} else {
        		return _min;
        	}
        }
        return null;
    }
    
    @JsonProperty(RangeFacet.TO)
    @JsonInclude(Include.NON_NULL)
    public Double getTo() {
        if (getError() == null) {
        	if (_config._selected) {
        		return _config._to;
        	} else {
        		return _max;
        	}
        }
        return null;
    }
}
