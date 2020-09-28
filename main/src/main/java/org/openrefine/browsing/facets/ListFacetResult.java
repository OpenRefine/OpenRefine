package org.openrefine.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.ProjectManager;
import org.openrefine.browsing.DecoratedValue;
import org.openrefine.browsing.facets.ListFacet.ListFacetConfig;
import org.openrefine.browsing.util.StringValuesFacetState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON representation of a list facet configuration bundled
 * up with facet statistics
 *
 */
public class ListFacetResult implements FacetResult {
	
	/**
	 * Wrapper class for choice counts and selection status for blank and error
	 */
	public static class OtherChoice {
	    @JsonProperty("s")
	    boolean selected;
	    @JsonProperty("c")
	    long count;
	    public OtherChoice(
	            @JsonProperty("s") boolean selected,
	            @JsonProperty("c") long count) {
	        this.selected = selected;
	        this.count = count;
	    }
	}

	protected ListFacetConfig _config;
	protected String _errorMessage;
	
    /*
     * Computed results
     */
    protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
    protected long _blankCount;
    protected long _errorCount;
	
	public ListFacetResult(ListFacetConfig config, StringValuesFacetState state) {
		_config = config;
		_errorMessage = null;
		
		// Populate choices
		Set<String> selectedValues = _config.getWrappedSelection().stream()
				.map(d -> d.value.label)
				.collect(Collectors.toSet());
		for(Map.Entry<String,Long> entry : state.getCounts().entrySet()) {
			DecoratedValue decoratedValue = new DecoratedValue(entry.getKey(), entry.getKey());
			boolean selected = selectedValues.contains(entry.getKey());
			NominalFacetChoice choice = new NominalFacetChoice(decoratedValue, entry.getValue(), selected);
			_choices.add(choice);
			selectedValues.remove(entry.getKey());
		}
		
		for(String missingValue : selectedValues) {
			DecoratedValue decoratedValue = new DecoratedValue(missingValue, missingValue);
			NominalFacetChoice choice = new NominalFacetChoice(decoratedValue, 0, true);
			_choices.add(choice);
		}
		
		_blankCount = state.getBlankCount();
		_errorCount = state.getErrorCount();
	}
	
	public ListFacetResult(ListFacetConfig config, String errorMessage) {
		_config = config;
		_errorMessage = errorMessage;
	}
	
    @JsonProperty("name")
    public String getName() {
        return _config.name;
    }
    
    @JsonProperty("columnName")
    public String getColumnName() {
        return _config.columnName;
    }
    
    @JsonProperty("expression")
    public String getExpression() {
        return _config.getExpression();
    }
    
    @JsonProperty("invert")
    public boolean getInvert() {
        return _config.invert;
    }
    
    @JsonProperty("error")
    @JsonInclude(Include.NON_NULL)
    public String getError() {
        if (_errorMessage == null && _choices.size() > getLimit()) {
            return ListFacet.ERR_TOO_MANY_CHOICES;
        }
        return _errorMessage;
    }
    
    @JsonProperty("choiceCount")
    @JsonInclude(Include.NON_NULL)
    public Integer getChoiceCount() {
        if (_errorMessage == null && _choices.size() > getLimit()) {
            return _choices.size();
        }
        return null;
    }
    
    @JsonProperty("choices")
    @JsonInclude(Include.NON_NULL)
    public List<NominalFacetChoice> getChoices() {
        if (getError() == null) {
            return _choices;
        }
        return null;
    }
    
    @JsonProperty("blankChoice")
    @JsonInclude(Include.NON_NULL)
    public OtherChoice getBlankChoice() {
        if (getError() == null && !_config.omitBlank && (_config.selectBlank || _blankCount > 0)) {
            return new OtherChoice(_config.selectBlank, _blankCount);
        }
        return null;
    }
    
    @JsonProperty("errorChoice")
    @JsonInclude(Include.NON_NULL)
    public ListFacetResult.OtherChoice getErrorChoice() {
        if (getError() == null && !_config.omitError && (_config.selectError || _errorCount > 0)) {
            return new OtherChoice(_config.selectError, _errorCount);
        }
        return null;
    }
    
    @JsonIgnore
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
}
