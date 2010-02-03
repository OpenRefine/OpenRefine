package com.metaweb.gridworks.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.browsing.FilteredRows;
import com.metaweb.gridworks.browsing.filters.ExpressionStringComparisonRowFilter;
import com.metaweb.gridworks.browsing.filters.RowFilter;
import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.VariableExpr;
import com.metaweb.gridworks.model.Project;

public class TextSearchFacet implements Facet {
	protected String 	_name;
	protected int		_cellIndex;
	protected String 	_query;
	
	protected String	_mode;
	protected boolean	_caseSensitive;
	
	public TextSearchFacet() {
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("name"); writer.value(_name);
		writer.key("cellIndex"); writer.value(_cellIndex);
		writer.key("query"); writer.value(_query);
		writer.key("mode"); writer.value(_mode);
		writer.key("caseSensitive"); writer.value(_caseSensitive);
		writer.endObject();
	}

	@Override
	public void initializeFromJSON(JSONObject o) throws Exception {
		_name = o.getString("name");
		_cellIndex = o.getInt("cellIndex");
		_query = o.getString("query");
		_mode = o.getString("mode");
		_caseSensitive = o.getBoolean("caseSensitive");
		if (!_caseSensitive) {
			_query = _query.toLowerCase();
		}
	}

	@Override
	public RowFilter getRowFilter() {
		Evaluable eval = new VariableExpr("value");
		
		if ("regex".equals(_mode)) {
			return new ExpressionStringComparisonRowFilter(eval, _cellIndex) {
				protected boolean checkValue(String s) {
					return s.matches(_query);
				};
			};
		} else {
			return new ExpressionStringComparisonRowFilter(eval, _cellIndex) {
				protected boolean checkValue(String s) {
					return s.toLowerCase().contains(_query);
				};
			};
		}		
	}

	@Override
	public void computeChoices(Project project, FilteredRows filteredRows) {
		// nothing to do
	}
}
