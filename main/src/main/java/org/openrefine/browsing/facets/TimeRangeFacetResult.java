package org.openrefine.browsing.facets;

import org.openrefine.browsing.facets.TimeRangeFacet.TimeRangeFacetConfig;
import org.openrefine.browsing.util.TimeRangeFacetState;
import org.openrefine.browsing.util.TimeRangeStatistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.primitives.Longs;

public class TimeRangeFacetResult implements FacetResult {
	
    protected static long[] steps = { 
            1,                  // msec
            1000,               // sec
            1000*60,            // min
            1000*60*60,         // hour
            1000*60*60*24,      // day
            1000*60*60*24*7,    // week
            1000l*2629746l,       // month (average Gregorian year / 12)
            1000l*31556952l,      // year (average Gregorian year)
            1000l*31556952l*10l,   // decade 
            1000l*31556952l*100l,  // century 
            1000l*31556952l*1000l, // millennium 
    };
	
	protected TimeRangeFacetConfig _config;
	protected String _errorMessage;
	
    /*
     * Computed data
     */
    @JsonProperty("baseTimeCount")
    protected long       _baseTimeCount;
    @JsonProperty("baseNonTimeCount")
    protected long       _baseNonTimeCount;
    @JsonProperty("baseBlankCount")
    protected long       _baseBlankCount;
    @JsonProperty("baseErrorCount")
    protected long       _baseErrorCount;
     
    @JsonProperty("timeCount")
    protected long       _timeCount;
    @JsonProperty("nonTimeCount")
    protected long       _nonTimeCount;
    @JsonProperty("blankCount")
    protected long       _blankCount;
    @JsonProperty("errorCount")
    protected long       _errorCount;
    
    @JsonIgnore
    protected double     _min;
    @JsonIgnore
    protected double     _max;
    @JsonIgnore
    protected double     _step;
    @JsonIgnore
    protected int[]      _bins; // int because they all fit in an array anyway
    @JsonIgnore
    protected int[]      _baseBins;

    public TimeRangeFacetResult(TimeRangeFacetConfig config, String error, TimeRangeFacetState state) {
		_config = config;
		_errorMessage = error;
		TimeRangeStatistics globalStats = state.getGlobalStatistics();
		_baseTimeCount = globalStats.getTimeCount();
		_baseNonTimeCount = globalStats.getNonTimeCount();
		_baseBlankCount = globalStats.getBlankCount();
		_baseErrorCount = globalStats.getErrorCount();
		TimeRangeStatistics viewStats = state.getViewStatistics();
		_timeCount = viewStats.getTimeCount();
		_nonTimeCount = viewStats.getNonTimeCount();
		_blankCount = viewStats.getBlankCount();
		_errorCount = viewStats.getErrorCount();

        _min = Long.MAX_VALUE;
        _max = Long.MIN_VALUE;
        
        // First passes on the array to determine min and max
        long[] allValues = globalStats.getTimeValues();
        if (allValues.length > 0) {
	        _min = Longs.min(allValues);
	        _max = Longs.max(allValues);
        }
        
        if (_min >= _max) {
            _step = 1;
            _min = 0;
            _max = _step;
            _bins = new int[1];
            _baseBins = new int[1];
            
            return;
        }
        
		if (_config.isSelected()) {
            _config.setFrom(Math.max(_config.getFrom(), _min));
            _config.setTo(Math.min(_config.getTo(), _max));
        } else {
            _config.setFrom(_min);
            _config.setTo(_max);
        }
        
        long diff = (long)(_max - _min);

        for (long step : steps) {
            _step = step;
            if (diff / _step <= 100) {
                break;
            } 
        }

        _baseBins = new int[(int) (diff / _step) + 1];
        _bins = new int[(int) (diff / _step) + 1];
        for (long d : allValues) {
            int bin = (int) Math.max((d - _min) / _step,0);
            _baseBins[bin]++;
        }
        for (long d : viewStats.getTimeValues()) {
        	int bin = (int) Math.max((d - _min) / _step,0);
            _bins[bin]++;
        }
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
        return _errorMessage;
    }
    
    @JsonProperty(TimeRangeFacet.MIN)
    @JsonInclude(Include.NON_NULL)
    public Double getMin() {
        if(getError() == null) {
            return _min;
        }
        return null;
    }
    
    @JsonProperty(TimeRangeFacet.MAX)
    @JsonInclude(Include.NON_NULL)
    public Double getMax() {
        if(getError() == null) {
            return _max;
        }
        return null;
    }
    
    @JsonProperty("step")
    @JsonInclude(Include.NON_NULL)
    public Double getStep() {
    	if (getError() == null) {
    		return _step;
    	} else {
    		return 1.0;
    	}
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
    
    @JsonProperty(TimeRangeFacet.FROM)
    @JsonInclude(Include.NON_NULL)
    public Double getFrom() {
        if (getError() == null) {
            return _config.getFrom();
        }
        return null;
    }
    
    @JsonProperty(TimeRangeFacet.TO)
    @JsonInclude(Include.NON_NULL)
    public Double getTo() {
        if (getError() == null) {
            return _config.getTo();
        }
        return null;
    }
}
