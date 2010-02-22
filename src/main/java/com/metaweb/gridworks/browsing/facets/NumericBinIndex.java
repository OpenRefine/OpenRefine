package com.metaweb.gridworks.browsing.facets;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.expr.Evaluable;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class NumericBinIndex {
	private double _min;
	private double _max;
	private double _step;
	private int[]  _bins;
	
	public NumericBinIndex(Project project, int cellIndex, Evaluable eval) {
        Properties bindings = ExpressionUtils.createBindings(project);
		
		_min = Double.POSITIVE_INFINITY;
		_max = Double.NEGATIVE_INFINITY;
		
		List<Double> allValues = new ArrayList<Double>();
		for (int i = 0; i < project.rows.size(); i++) {
			Row row = project.rows.get(i);
			Cell cell = row.getCell(cellIndex);

            ExpressionUtils.bind(bindings, row, i, cell);
			
			Object value = eval.evaluate(bindings);
			if (value != null) {
				if (value.getClass().isArray()) {
					Object[] a = (Object[]) value;
					for (Object v : a) {
						if (v instanceof Number) {
							processValue(((Number) v).doubleValue(), allValues);
						}
					}
				} else if (value instanceof Number) {
					processValue(((Number) value).doubleValue(), allValues);
				}
			}
		}
		
		if (_min >= _max) {
			_step = 0;
			_bins = new int[1];
			return;
		}
		
		double diff = getMax() - getMin();
		_step = 1;
		if (diff > 10) {
			while (getStep() * 100 < diff) {
				_step *= 10;
			}
		} else {
			while (getStep() * 100 > diff) {
				_step /= 10;
			}
		}
		
		_min = (Math.floor(_min / _step) * _step);
		_max = (Math.ceil(_max / _step) * _step);
		
		int binCount = 1 + (int) Math.ceil((getMax() - getMin()) / getStep());
		if (binCount > 100) {
			_step *= 2;
			binCount = Math.round((1 + binCount) / 2);
		}
		
		_bins = new int[binCount];
		for (double d : allValues) {
			int bin = (int) Math.round((d - _min) / _step);
			_bins[bin]++;
		}
	}
	
	public double getMin() {
		return _min;
	}

	public double getMax() {
		return _max;
	}

	public double getStep() {
		return _step;
	}

	public int[] getBins() {
		return _bins;
	}

	protected void processValue(double v, List<Double> allValues) {
		_min = Math.min(getMin(), v);
		_max = Math.max(getMax(), v);
		allValues.add(v);
	}
}
