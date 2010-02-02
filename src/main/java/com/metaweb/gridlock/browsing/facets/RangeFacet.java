package com.metaweb.gridlock.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.filters.ExpressionComparisonRowFilter;
import com.metaweb.gridlock.browsing.filters.RowFilter;
import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.expr.Parser;
import com.metaweb.gridlock.model.Project;

public class RangeFacet implements Facet {
	protected String 	_name;
	protected String 	_expression;
	protected int		_cellIndex;
	protected Evaluable _eval;
	
	protected String	_mode;
	protected double	_min;
	protected double	_max;
	
	public RangeFacet() {
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("name"); writer.value(_name);
		writer.key("expression"); writer.value(_expression);
		writer.key("cellIndex"); writer.value(_cellIndex);
		
		writer.key("mode"); writer.value(_mode);
		if ("min".equals(_mode)) {
			writer.key("min"); writer.value(_min);
		} else if ("max".equals(_mode)) {
			writer.key("max"); writer.value(_max);
		} else {
			writer.key("min"); writer.value(_min);
			writer.key("max"); writer.value(_max);
		}
		writer.endObject();
	}

	@Override
	public void initializeFromJSON(JSONObject o) throws Exception {
		_name = o.getString("name");
		_expression = o.getString("expression");
		_cellIndex = o.getInt("cellIndex");
		
		_eval = new Parser(_expression).getExpression();
		
		_mode = o.getString("mode");
		if ("min".equals(_mode)) {
			_min = o.getDouble("min");
		} else if ("max".equals(_mode)) {
			_max = o.getDouble("max");
		} else {
			_min = o.getDouble("min");
			_max = o.getDouble("max");
		}
	}

	@Override
	public RowFilter getRowFilter() {
		if ("min".equals(_mode)) {
			return new ExpressionComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d >= _min;
				};
			};
		} else if ("max".equals(_mode)) {
			return new ExpressionComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d <= _max;
				};
			};
		} else {
			return new ExpressionComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d >= _min && d <= _max;
				};
			};
		}		
	}

	@Override
	public void computeChoices(Project project, FilteredRows filteredRows) {
		// nothing to do
	}
}
