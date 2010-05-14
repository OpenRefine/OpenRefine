package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.ExpressionStringComparisonRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.gel.ast.VariableExpr;
import com.metaweb.gridworks.model.Project;

public class TextSearchFacet implements Facet {
    /*
     *  Configuration
     */
    protected String     _name;
    protected String     _columnName;
    protected String     _query;
    protected String     _mode;
    protected boolean    _caseSensitive;
    
    /*
     *  Derived configuration
     */
    protected int        _cellIndex;
    protected Pattern    _pattern;
    
    public TextSearchFacet() {
    }

    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("columnName"); writer.value(_columnName);
        writer.key("query"); writer.value(_query);
        writer.key("mode"); writer.value(_mode);
        writer.key("caseSensitive"); writer.value(_caseSensitive);
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");
        _columnName = o.getString("columnName");
        
        _cellIndex = project.columnModel.getColumnByName(_columnName).getCellIndex();
        
        if (!o.isNull("query")) {
            _query = o.getString("query"); 
        }
        
        _mode = o.getString("mode");
        _caseSensitive = o.getBoolean("caseSensitive");
        if (_query != null) {
            if ("regex".equals(_mode)) {
                try {
                    _pattern = Pattern.compile(
                    		_query, 
                    		_caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } catch (java.util.regex.PatternSyntaxException e) {
                    e.printStackTrace();
                }
            } else if (!_caseSensitive) {
        		_query = _query.toLowerCase();
            }
        }
    }

    public RowFilter getRowFilter() {
        if (_query == null || _query.length() == 0) {
            return null;
        } else if ("regex".equals(_mode) && _pattern == null) {
            return null;
        }
        
        Evaluable eval = new VariableExpr("value");
        
        if ("regex".equals(_mode)) {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                protected boolean checkValue(String s) {
                    return _pattern.matcher(s).find();
                };
            };
        } else {
            return new ExpressionStringComparisonRowFilter(eval, _columnName, _cellIndex) {
                protected boolean checkValue(String s) {
                    return (_caseSensitive ? s : s.toLowerCase()).contains(_query);
                };
            };
        }        
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        // nothing to do
    }
}
