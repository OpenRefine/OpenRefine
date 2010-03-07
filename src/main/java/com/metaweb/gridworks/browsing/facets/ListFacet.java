package com.metaweb.gridworks.browsing.facets;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.DecoratedValue;
import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.ExpressionEqualRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.JSONUtilities;

public class ListFacet implements Facet {
    protected List<NominalFacetChoice> _selection = new LinkedList<NominalFacetChoice>();
    
    // If true, then facet won't show the blank and error choices
    protected boolean _omitBlank;
    protected boolean _omitError;
    
    protected boolean _selectBlank;
    protected boolean _selectError;
    
    protected String     _name;
    protected String     _expression;
    protected String     _columnName;
    protected int        _cellIndex;
    protected Evaluable  _eval;
    
    // computed
    protected List<NominalFacetChoice> _choices = new LinkedList<NominalFacetChoice>();
    protected int _blankCount;
    protected int _errorCount;
    
    public ListFacet() {
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("expression"); writer.value(_expression);
        writer.key("columnName"); writer.value(_columnName);
        
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
        
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _expression = o.getString("expression");
        _columnName = o.getString("columnName");
        
        if (_columnName.length() > 0) {
        	_cellIndex = project.columnModel.getColumnByName(_columnName).getCellIndex();
        } else {
        	_cellIndex = -1;
        }
        
        _eval = MetaParser.parse(_expression);
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

    public RowFilter getRowFilter() {
        return _selection.size() == 0 && !_selectBlank && !_selectError ? null :
            new ExpressionEqualRowFilter(_eval, _cellIndex, createMatches(), _selectBlank, _selectError);
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        ExpressionNominalRowGrouper grouper = 
            new ExpressionNominalRowGrouper(_eval, _cellIndex);
        
        filteredRows.accept(project, grouper);
        
        _choices.clear();
        _choices.addAll(grouper.choices.values());
        
        for (NominalFacetChoice choice : _selection) {
            String valueString = choice.decoratedValue.value.toString();
            if (grouper.choices.containsKey(valueString)) {
                grouper.choices.get(valueString).selected = true;
            } else {
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
