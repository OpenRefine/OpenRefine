package com.metaweb.gridworks.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.FilteredRecords;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.RecordFilter;
import com.metaweb.gridworks.browsing.RowFilter;
import com.metaweb.gridworks.browsing.filters.AnyRowRecordFilter;
import com.metaweb.gridworks.browsing.filters.ExpressionEqualRowFilter;
import com.metaweb.gridworks.browsing.util.ExpressionNominalValueGrouper;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.JSONUtilities;

public class ListFacet implements Facet {
    /*
     * Configuration
     */
    protected String     _name;
    protected String     _expression;
    protected String     _columnName;
    protected boolean    _invert;
    
    // If true, then facet won't show the blank and error choices
    protected boolean _omitBlank;
    protected boolean _omitError;
    
    protected List<NominalFacetChoice> _selection = new LinkedList<NominalFacetChoice>();
    protected boolean _selectBlank;
    protected boolean _selectError;
    
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
        writer.key("name"); writer.value(_name);
        writer.key("expression"); writer.value(_expression);
        writer.key("columnName"); writer.value(_columnName);
        writer.key("invert"); writer.value(_invert);
        
        if (_errorMessage != null) {
            writer.key("error"); writer.value(_errorMessage);
        } else if (_choices.size() > 2000) {
            writer.key("error"); writer.value("Too many choices");
        } else {
            writer.key("choices"); writer.array();
            for (NominalFacetChoice choice : _choices) {
                choice.write(writer, options);
            }
            writer.endArray();
            
            if (!_omitBlank && (_selectBlank || _blankCount > 0)) {
                writer.key("blankChoice");
                writer.object();
                writer.key("s"); writer.value(_selectBlank);
                writer.key("c"); writer.value(_blankCount);
                writer.endObject();
            }
            if (!_omitError && (_selectError || _errorCount > 0)) {
                writer.key("errorChoice");
                writer.object();
                writer.key("s"); writer.value(_selectError);
                writer.key("c"); writer.value(_errorCount);
                writer.endObject();
            }
        }
        
        writer.endObject();
    }

    @Override
    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _expression = o.getString("expression");
        _columnName = o.getString("columnName");
        _invert = o.has("invert") && o.getBoolean("invert");
        
        if (_columnName.length() > 0) {
            Column column = project.columnModel.getColumnByName(_columnName);
            if (column != null) {
                _cellIndex = column.getCellIndex();
            } else {
                _errorMessage = "No column named " + _columnName;
            }
        } else {
            _cellIndex = -1;
        }
        
        try {
            _eval = MetaParser.parse(_expression);
        } catch (ParsingException e) {
            _errorMessage = e.getMessage();
        }
        
        _selection.clear();
        
        JSONArray a = o.getJSONArray("selection");
        int length = a.length();
        
        for (int i = 0; i < length; i++) {
            JSONObject oc = a.getJSONObject(i);
            JSONObject ocv = oc.getJSONObject("v");
            
            DecoratedValue decoratedValue = new DecoratedValue(
                ocv.get("v"), ocv.getString("l"));
            
            NominalFacetChoice nominalFacetChoice = new NominalFacetChoice(decoratedValue);
            nominalFacetChoice.selected = true;
            
            _selection.add(nominalFacetChoice);
        }
        
        _omitBlank = JSONUtilities.getBoolean(o, "omitBlank", false);
        _omitError = JSONUtilities.getBoolean(o, "omitError", false);
        
        _selectBlank = JSONUtilities.getBoolean(o, "selectBlank", false);
        _selectError = JSONUtilities.getBoolean(o, "selectError", false);
    }

    @Override
    public RowFilter getRowFilter(Project project) {
        return 
            _eval == null || 
            _errorMessage != null ||
            (_selection.size() == 0 && !_selectBlank && !_selectError) ? 
                null :
                new ExpressionEqualRowFilter(
                    _eval, 
                    _columnName,
                    _cellIndex, 
                    createMatches(), 
                    _selectBlank, 
                    _selectError,
                    _invert);
    }
    
    @Override
    public RecordFilter getRecordFilter(Project project) {
    	RowFilter rowFilter = getRowFilter(project);
    	return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (_eval != null && _errorMessage == null) {
            ExpressionNominalValueGrouper grouper = 
                new ExpressionNominalValueGrouper(_eval, _columnName, _cellIndex);
            
            filteredRows.accept(project, grouper);
            
            postProcessGrouper(grouper);
        }
    }
    
    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        if (_eval != null && _errorMessage == null) {
            ExpressionNominalValueGrouper grouper = 
                new ExpressionNominalValueGrouper(_eval, _columnName, _cellIndex);
            
            filteredRecords.accept(project, grouper);
            
            postProcessGrouper(grouper);
        }
    }
    
    protected void postProcessGrouper(ExpressionNominalValueGrouper grouper) {
        _choices.clear();
        _choices.addAll(grouper.choices.values());
        
        for (NominalFacetChoice choice : _selection) {
            String valueString = choice.decoratedValue.value.toString();
            
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
                choice.count = 0;
                _choices.add(choice);
            }
        }
        
        _blankCount = grouper.blankCount;
        _errorCount = grouper.errorCount;
    }
    
    protected Object[] createMatches() {
        Object[] a = new Object[_selection.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = _selection.get(i).decoratedValue.value;
        }
        return a;
    }
}
