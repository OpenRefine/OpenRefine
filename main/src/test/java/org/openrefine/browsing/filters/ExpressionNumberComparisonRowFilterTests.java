package org.openrefine.browsing.filters;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.openrefine.browsing.util.RowEvaluable;
import org.openrefine.expr.EvalError;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExpressionNumberComparisonRowFilterTests {

	RowEvaluable rowEvaluable = new RowEvaluable() {

		@Override
		public Object eval(long rowIndex, Row row, Properties bindings) {
			return row.getCellValue(0);
		}
		
	};
	
	private class NumberRowFilter extends ExpressionNumberComparisonRowFilter {

		private static final long serialVersionUID = 1065000677990534930L;

		public NumberRowFilter(RowEvaluable rowEvaluable, boolean selectNumeric, boolean selectNonNumeric,
				boolean selectBlank, boolean selectError) {
			super(rowEvaluable, selectNumeric, selectNonNumeric, selectBlank, selectError);
		}

		@Override
		protected boolean checkValue(double d) {
			return d > 45 && d < 53.8;
		}
	}
	
	private Row row(Object v) {
		return new Row(Collections.singletonList(new Cell((Serializable) v, null)));
	}
 	
	@Test
	public void testBlank() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, false, false, true, false)
				.filterRow(1234L, row("")));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, false, false, false, false)
				.filterRow(1234L, row("")));
	}
	
	@Test
	public void testError() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, false, false, false, true)
				.filterRow(1234L, row(new EvalError("error"))));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, false, false, false, false)
				.filterRow(1234L, row(new EvalError("error"))));
	}
	
	@Test
	public void testNonNumeric() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, false, true, false, false)
				.filterRow(1234L, row("string")));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, false, false, false, false)
				.filterRow(1234L, row("string")));
	}
	
	@Test
	public void testNumeric() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(51.2)));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(5.2)));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, false, false, false, false)
				.filterRow(1234L, row(51.2)));
	}
	
	@Test
	public void testArray() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(new Serializable[] {"foo", 51.3})));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(new Serializable[] {"foo", "bar"})));
	}
	
	@Test
	public void testCollection() {
		Assert.assertTrue(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(Arrays.asList(new Serializable[] {"foo", 51.3}))));
		Assert.assertFalse(new NumberRowFilter(rowEvaluable, true, false, false, false)
				.filterRow(1234L, row(Arrays.asList(new Serializable[] {"foo", "bar"}))));
	}
}
