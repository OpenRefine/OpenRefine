package com.metaweb.gridworks.sorting;

import java.util.Calendar;
import java.util.Date;

import com.metaweb.gridworks.expr.EvalError;
import com.metaweb.gridworks.expr.ExpressionUtils;

public class DateCriterion extends Criterion {
	final static protected EvalError s_error = new EvalError("Not a date");
	
	@Override
	public KeyMaker createKeyMaker() {
		return new KeyMaker() {
			@Override
			protected Object makeKey(Object value) {
				if (ExpressionUtils.isNonBlankData(value)) {
					if (value instanceof Date) {
						return value;
					} else if (value instanceof Calendar) {
						return ((Calendar) value).getTime();
					} else {
						return s_error;
					}
				}
				return value;
			}
			
			@Override
			public int compareKeys(Object key1, Object key2) {
				return ((Date) key1).compareTo((Date) key2);
			}
		};
	}
}
