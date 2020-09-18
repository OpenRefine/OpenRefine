package org.openrefine.browsing.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.expr.EvalError;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class NumericFacetAggregatorTests {
	
	NumericFacetAggregator SUT;
	HistogramState empty = new HistogramState(0, 1, 2, 3, 0);
	HistogramState emptyWith4 = new HistogramState(1, 1, 2, 3, 4.0);
	HistogramState singletonA = new HistogramState(5, 1, 2, 3, 4.0);
	HistogramState singletonB = new HistogramState(5, 1, 2, 3, 4321.0);
	HistogramState rangeA = new HistogramState(7, 3, 2, 1, 0, -3, new long[] { 3, 4 });
	HistogramState rangeB = new HistogramState(9, 4, 6, 8, 0, 789, new long[] { 5, 4 });
	HistogramState rangeC = new HistogramState(3, 0, 0, 0, 1, 79, new long[] { 2, 0, 0, 0, 1 });
	
	RowEvaluable evaluable = new RowEvaluable() {

		@Override
		public Object eval(long rowIndex, Row row, Properties bindings) {
			return row.getCellValue(0);
		}
		
	};
	
	@BeforeTest
	public void setUpAggregator() {
		SUT = new NumericFacetAggregator(10, evaluable, 43.5, 89.2, true, false, true, false, true, true);
	}
	
	private Row row(Serializable value) {
		return new Row(Collections.singletonList(new Cell(value, null)));
	}
	
	@Test
	public void testWithRow() {
		NumericFacetState withRow = SUT.withRow(new NumericFacetState(empty, empty), 1234L, row(4.0));
		Assert.assertEquals(withRow, new NumericFacetState(emptyWith4, emptyWith4));
	}
	
	@Test
	public void testWithRowOutsideView() {
		NumericFacetState withRow = SUT.withRowOutsideView(new NumericFacetState(empty, empty), 1234L, row(4.0));
		Assert.assertEquals(withRow, new NumericFacetState(emptyWith4, empty));
	}
	
	@Test
	public void testWithValueFromEmpty() {
		HistogramState withRow = SUT.withValue(empty, 4.0);
		Assert.assertEquals(withRow, emptyWith4);
	}
	
	@Test
	public void testWithValueOutsideRange() {
		HistogramState withRow = SUT.withValue(rangeA, 3.1);
		Assert.assertEquals(withRow, new HistogramState(8, 3, 2, 1, 0, -3, new long[] { 3, 4, 0, 0, 0, 0, 1 }));
	}
	
	@Test
	public void testWithValueInsideRange() {
		HistogramState withRow = SUT.withValue(rangeA, -2.7);
		Assert.assertEquals(withRow, new HistogramState(8, 3, 2, 1, 0, -3, new long[] { 4, 4 }));
	}
	
	@Test
	public void testWithValueBlank() {
		HistogramState withRow = SUT.withValue(rangeA, "");
		Assert.assertEquals(withRow, new HistogramState(7, 3, 2, 2, 0, -3, new long[] { 3, 4 }));
	}
	
	@Test
	public void testWithValueError() {
		HistogramState withRow = SUT.withValue(rangeA, new EvalError("error"));
		Assert.assertEquals(withRow, new HistogramState(7, 3, 3, 1, 0, -3, new long[] { 3, 4 }));
	}
	
	@Test
	public void testWithValueNonNumeric() {
		HistogramState withRow = SUT.withRow(rangeA, "string value");
		Assert.assertEquals(withRow, new HistogramState(7, 4, 2, 1, 0, -3, new long[] { 3, 4 }));
	}
	
	@Test
	public void testWithValueArray() {
		HistogramState withRow = SUT.withRow(rangeA, new Serializable[] {-2.3, "string value"});
		Assert.assertEquals(withRow, new HistogramState(8, 4, 2, 1, 0, -3, new long[] { 4, 4 }));
	}
	
	@Test
	public void testWithValueList() {
		HistogramState withRow = SUT.withRow(rangeA, (Serializable) Arrays.asList(new Serializable[] {-2.3, "string value"}));
		Assert.assertEquals(withRow, new HistogramState(8, 4, 2, 1, 0, -3, new long[] { 4, 4 }));
	}

	@Test
	public void testSumEmptyStates() {
		HistogramState sum = SUT.sum(empty, empty);
		Assert.assertEquals(sum, new HistogramState(0, 2, 4, 6, 0));
	}
	
	@Test
	public void testSumSingletonEmpty() {
		HistogramState sum = SUT.sum(singletonA, empty);
		Assert.assertEquals(sum, new HistogramState(5, 2, 4, 6, 4.0));
	}
	
	@Test
	public void testSumIdenticalSingletons() {
		HistogramState sum = SUT.sum(singletonA, singletonA);
		Assert.assertEquals(sum, new HistogramState(10, 2, 4, 6, 4.0));
	}
	
	@Test
	public void testSumSingletonsApart() {
		HistogramState sum = SUT.sum(singletonA, singletonB);
		Assert.assertEquals(sum, new HistogramState(10, 2, 4, 6, 3, 0, new long[] { 5, 0, 0, 0, 5 }));
	}
	
	@Test
	public void testSumRangeSingleton() {
		HistogramState sum = SUT.sum(singletonA, rangeA);
		Assert.assertEquals(sum, new HistogramState(12, 4, 4, 4, 0, -3, new long[] { 3, 4, 0, 0, 0, 0, 0, 5 }));
	}
	
	@Test
	public void testSumSingletonRange() {
		HistogramState sum = SUT.sum(rangeA, singletonA);
		Assert.assertEquals(sum, new HistogramState(12, 4, 4, 4, 0, -3, new long[] { 3, 4, 0, 0, 0, 0, 0, 5 }));
	}
	
	@Test
	public void testSumRangesFarApart() {
		HistogramState expectedSum = new HistogramState(16, 7, 8, 9, 2, -1, new long[] { 7, 0, 0, 0, 0, 0, 0, 0, 9 });
		HistogramState sum = SUT.sum(rangeA, rangeB);
		Assert.assertEquals(sum, expectedSum);
	}
	
	@Test
	public void testSumOverlappingRanges() {
		HistogramState expectedSum = new HistogramState(12, 4, 6, 8, 1, 78, new long[] { 5, 6, 0, 0, 0, 1 });
		HistogramState sum = SUT.sum(rangeB, rangeC);
		Assert.assertEquals(sum, expectedSum);
	}
	
	@Test
	public void testRowFilter() {
		RowFilter filter = SUT.getRowFilter();
		Assert.assertTrue(filter.filterRow(1234L, row(57.9)));
		Assert.assertFalse(filter.filterRow(1245L, row(-34.3)));
	}
	
	@Test
	public void testRecordFilter() {
		Assert.assertTrue(SUT.getRecordFilter() instanceof AllRowsRecordFilter);
	}
}
