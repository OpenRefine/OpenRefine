package com.metaweb.gridlock.browsing.facets;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridlock.expr.Evaluable;
import com.metaweb.gridlock.model.Cell;
import com.metaweb.gridlock.model.Project;
import com.metaweb.gridlock.model.Row;

public class NumericBinIndex {
	public double min;
	public double max;
	public double step;
	public int[]  bins;
	
	public NumericBinIndex(Project project, int cellIndex, Evaluable eval) {
		Properties bindings = new Properties();
		
		min = Double.POSITIVE_INFINITY;
		max = Double.NEGATIVE_INFINITY;
		
		List<Double> allValues = new ArrayList<Double>();
		for (int i = 0; i < project.rows.size(); i++) {
			Row row = project.rows.get(i);
			
			if (cellIndex < row.cells.size()) {
				Cell cell = row.cells.get(cellIndex);
				if (cell != null) {
					bindings.put("project", project);
					bindings.put("cell", cell);
					bindings.put("value", cell.value);
					
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
			}
		}
		
		if (min >= max) {
			step = 0;
			bins = new int[0];
			return;
		}
		
		double diff = max - min;
		if (diff > 10) {
			step = 1;
			while (step * 100 < diff) {
				step *= 10;
			}
		} else {
			step = 1;
			while (step * 100 > diff) {
				step /= 10;
			}
		}
		
		min = Math.floor(min / step) * step;
		max = Math.ceil(max / step) * step;
		
		int binCount = 1 + (int) Math.ceil((max - min) / step);
		
		bins = new int[binCount];
		for (double d : allValues) {
			int bin = (int) Math.round((d - min) / step);
			bins[bin]++;
		}
	}
	
	protected void processValue(double v, List<Double> allValues) {
		min = Math.min(min, v);
		max = Math.max(max, v);
		allValues.add(v);
	}
}
