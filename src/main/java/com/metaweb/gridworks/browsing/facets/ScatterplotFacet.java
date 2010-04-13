package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.DualExpressionsNumberComparisonRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.MetaParser;
import com.metaweb.gridworks.expr.ParsingException;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;

public class ScatterplotFacet implements Facet {

    /*
     * Configuration, from the client side
     */
    protected String     _name;       // name of facet

    protected String     _x_expression; // expression to compute the x numeric value(s) per row
    protected String     _y_expression; // expression to compute the y numeric value(s) per row
    protected String     _x_columnName; // column to base the x expression on, if any
    protected String     _y_columnName; // column to base the y expression on, if any
    
    protected double    _x_from; // the numeric selection for the x axis
    protected double    _x_to;
    protected double    _y_from; // the numeric selection for the y axis
    protected double    _y_to;

    protected double    _x_min; 
    protected double    _x_max;
    protected double    _y_min;
    protected double    _y_max;
    
    /*
     * Derived configuration data
     */
    protected int        _x_cellIndex;
    protected int        _y_cellIndex;
    protected Evaluable  _x_eval;
    protected Evaluable  _y_eval;
    protected String     _x_errorMessage;
    protected String     _y_errorMessage;

    protected boolean    _selected; // false if we're certain that all rows will match
                                    // and there isn't any filtering to do
        
    public ScatterplotFacet() {
    }

    private static final String X_MIN = "x_min";
    private static final String X_MAX = "x_max";
    private static final String X_TO = "x_to";
    private static final String X_FROM = "x_from";
    private static final String Y_MIN = "y_min";
    private static final String Y_MAX = "y_max";
    private static final String Y_TO = "y_to";
    private static final String Y_FROM = "y_from";
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key("name"); writer.value(_name);
        writer.key("x_expression"); writer.value(_x_expression);
        writer.key("x_columnName"); writer.value(_x_columnName);
        writer.key("y_expression"); writer.value(_y_expression);
        writer.key("y_columnName"); writer.value(_y_columnName);
        
        if (_x_errorMessage != null) {
            writer.key("x_error"); writer.value(_x_errorMessage);
        } else {
            if (!Double.isInfinite(_x_min) && !Double.isInfinite(_x_max)) {
                writer.key(X_MIN); writer.value(_x_min);
                writer.key(X_MAX); writer.value(_x_max);
                writer.key(X_FROM); writer.value(_x_from);
                writer.key(X_TO); writer.value(_x_to);
            }
            if (!Double.isInfinite(_y_min) && !Double.isInfinite(_y_max)) {
                writer.key(Y_MIN); writer.value(_y_min);
                writer.key(Y_MAX); writer.value(_y_max);
                writer.key(Y_FROM); writer.value(_y_from);
                writer.key(Y_TO); writer.value(_y_to);
            }
        }
        writer.endObject();
    }

    public void initializeFromJSON(Project project, JSONObject o) throws Exception {
        _name = o.getString("name");

        _x_expression = o.getString("x_expression");
        _x_columnName = o.getString("x_columnName");
        
        if (_x_columnName.length() > 0) {
            Column x_column = project.columnModel.getColumnByName(_x_columnName);
            if (x_column != null) {
                _x_cellIndex = x_column.getCellIndex();
            } else {
                _x_errorMessage = "No column named " + _x_columnName;
            }
        } else {
            _x_cellIndex = -1;
        }
        
        try {
            _x_eval = MetaParser.parse(_x_expression);
        } catch (ParsingException e) {
            _x_errorMessage = e.getMessage();
        }
        
        if (o.has(X_FROM) && o.has(X_TO)) {
            _x_from = o.getDouble(X_FROM);
            _x_to = o.getDouble(X_TO);
            _selected = true;
        }
        
        _y_expression = o.getString("y_expression");
        _y_columnName = o.getString("y_columnName");
        
        if (_y_columnName.length() > 0) {
            Column y_column = project.columnModel.getColumnByName(_y_columnName);
            if (y_column != null) {
                _y_cellIndex = y_column.getCellIndex();
            } else {
                _y_errorMessage = "No column named " + _y_columnName;
            }
        } else {
            _y_cellIndex = -1;
        }
        
        try {
            _y_eval = MetaParser.parse(_y_expression);
        } catch (ParsingException e) {
            _y_errorMessage = e.getMessage();
        }
        
        if (o.has(Y_FROM) && o.has(Y_TO)) {
            _y_from = o.getDouble(Y_FROM);
            _y_to = o.getDouble(Y_TO);
            _selected = true;
        }
    }

    public RowFilter getRowFilter() {
        if (_selected && 
            _x_eval != null && _x_errorMessage == null && 
            _y_eval != null && _y_errorMessage == null) 
        {
            return new DualExpressionsNumberComparisonRowFilter(_x_eval, _x_columnName, _x_cellIndex, _y_eval, _y_columnName, _y_cellIndex) {
                protected boolean checkValues(double x, double y) {
                    return x >= _x_from && x < _x_to && y >= _y_from && y < _y_to;
                };
            };
        } else {
            return null;
        }
    }

    public void computeChoices(Project project, FilteredRows filteredRows) {
        if (_x_eval != null && _y_eval != null && _x_errorMessage == null && _y_errorMessage == null) {
            Column column_x = project.columnModel.getColumnByCellIndex(_x_cellIndex);
            String key_x = "numeric-bin:" + _x_expression;
            NumericBinIndex index_x = (NumericBinIndex) column_x.getPrecompute(key_x);
            if (index_x == null) {
                index_x = new NumericBinIndex(project, _x_columnName, _x_cellIndex, _x_eval);
                column_x.setPrecompute(key_x, index_x);
            }
            
            _x_min = index_x.getMin();
            _x_max = index_x.getMax();
            
            if (_selected) {
                _x_from = Math.max(_x_from, _x_min);
                _x_to = Math.min(_x_to, _x_max);
            } else {
                _x_from = _x_min;
                _x_to = _x_max;
            }
            
            Column column_y = project.columnModel.getColumnByCellIndex(_y_cellIndex);
            String key_y = "numeric-bin:" + _y_expression;
            NumericBinIndex index_y = (NumericBinIndex) column_y.getPrecompute(key_y);
            if (index_y == null) {
                index_y = new NumericBinIndex(project, _y_columnName, _y_cellIndex, _y_eval);
                column_y.setPrecompute(key_y, index_y);
            }
            
            _y_min = index_y.getMin();
            _y_max = index_y.getMax();
            
            if (_selected) {
                _y_from = Math.max(_y_from, _y_min);
                _y_to = Math.min(_y_to, _y_max);
            } else {
                _y_from = _y_min;
                _y_to = _y_max;
            }
        }
    }
}
