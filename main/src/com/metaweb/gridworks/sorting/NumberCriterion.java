package com.metaweb.gridworks.sorting;

import java.util.Calendar;
import java.util.Date;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;

public class NumberCriterion extends Criterion {
	
	final static protected EvalError s_error = new EvalError("Not a number");
	
	@Override
	public KeyMaker createKeyMaker() {
		return new KeyMaker() {
			@Override
			protected Object makeKey(Object value) {
				if (ExpressionUtils.isNonBlankData(value)) {
					if (value instanceof Number) {
						return value;
					} else if (value instanceof Boolean) {
						return ((Boolean) value).booleanValue() ? 1 : 0;
					} else if (value instanceof Date) {
						return ((Date) value).getTime();
					} else if (value instanceof Calendar) {
						return ((Calendar) value).getTime().getTime();
					} else if (value instanceof String) {
						try {
							double d = Double.parseDouble((String) value);
							if (!Double.isNaN(d)) {
								return d;
							}
						} catch (NumberFormatException e) {
							// fall through
						}
					}
					return s_error;
				}
				return value;
			}
			
			@Override
			public int compareKeys(Object key1, Object key2) {
				double d1 = ((Number) key1).doubleValue();
				double d2 = ((Number) key2).doubleValue();
				return d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
			}
		};
	}
}
