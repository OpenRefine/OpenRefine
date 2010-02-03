package com.metaweb.gridlock.browsing.facets;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridlock.browsing.FilteredRows;
import com.metaweb.gridlock.browsing.filters.ExpressionNumberComparisonRowFilter;
import com.metaweb.gridlock.browsing.filters.RowFilter;
import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.expr.Parser;
import com.metaweb.gridlock.model.Column;
import com.metaweb.gridlock.model.Project;

public class RangeFacet implements Facet {
	protected String 	_name;
	protected String 	_expression;
	protected int		_cellIndex;
	protected Evaluable _eval;
	
	protected String	_mode;
	protected double	_min;
	protected double	_max;
	protected double	_step;
	protected int[]		_bins;
	
	protected double	_from;
	protected double	_to;
	
	public RangeFacet() {
	}

	@Override
	public void write(JSONWriter writer, Properties options)
			throws JSONException {
		
		writer.object();
		writer.key("name"); writer.value(_name);
		writer.key("expression"); writer.value(_expression);
		writer.key("cellIndex"); writer.value(_cellIndex);
		writer.key("min"); writer.value(_min);
		writer.key("max"); writer.value(_max);
		writer.key("step"); writer.value(_step);
		
		writer.key("bins"); writer.array();
		for (int b : _bins) {
			writer.value(b);
		}
		writer.endArray();
		
		writer.key("mode"); writer.value(_mode);
		if ("min".equals(_mode)) {
			writer.key("from"); writer.value(_from);
		} else if ("max".equals(_mode)) {
			writer.key("to"); writer.value(_to);
		} else {
			writer.key("from"); writer.value(_from);
			writer.key("to"); writer.value(_to);
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
			_from = o.getDouble("from");
		} else if ("max".equals(_mode)) {
			_to = o.getDouble("to");
		} else {
			_from = o.getDouble("from");
			_to = o.getDouble("to");
		}
	}

	@Override
	public RowFilter getRowFilter() {
		if ("min".equals(_mode)) {
			return new ExpressionNumberComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d >= _from;
				};
			};
		} else if ("max".equals(_mode)) {
			return new ExpressionNumberComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d <= _to;
				};
			};
		} else {
			return new ExpressionNumberComparisonRowFilter(_eval, _cellIndex) {
				protected boolean checkValue(double d) {
					return d >= _from && d <= _to;
				};
			};
		}		
	}

	@Override
	public void computeChoices(Project project, FilteredRows filteredRows) {
		Column column = project.columnModel.getColumnByCellIndex(_cellIndex);
		
		String key = "numeric-bin:" + _expression;
		NumericBinIndex index = (NumericBinIndex) column.getPrecompute(key);
		if (index == null) {
			index = new NumericBinIndex(project, _cellIndex, _eval);
			column.setPrecompute(key, index);
		}
		
		_min = index.min;
		_max = index.max;
		_step = index.step;
		_bins = index.bins;
	}
}
