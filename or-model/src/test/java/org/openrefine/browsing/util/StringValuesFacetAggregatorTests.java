package org.openrefine.browsing.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.openrefine.expr.EvalError;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Row;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class StringValuesFacetAggregatorTests {
	ColumnModel cm;
	StringValuesFacetState initial;
	RowEvaluable dummy = new RowEvaluable() {

        private static final long serialVersionUID = -5844679178529098676L;

        @Override
        public Object eval(long rowIndex, Row row, Properties bindings) {
            return row.getCellValue(0);   
        }
	};
	
	StringValuesFacetAggregator SUT;
	
	@BeforeTest
	public void setUpState() {
		cm = new ColumnModel(Arrays.asList(new ColumnMetadata("mycolumn")));
		SUT = new StringValuesFacetAggregator(cm, 0, dummy, Collections.singleton("foo"), false, false, false);
		initial = new StringValuesFacetState(Collections.emptyMap(), 0L, 0L);
	}
	
	@Test
	public void testSum() {
		StringValuesFacetState first = new StringValuesFacetState(
				Collections.<String,Long>singletonMap("foo", 3L), 4L, 5L);
		StringValuesFacetState second = new StringValuesFacetState(
				Collections.<String,Long>singletonMap("foo", 4L), 10L, 11L);
		StringValuesFacetState combined = SUT.sum(first, second);
		
		Assert.assertEquals(combined.getBlankCount(), 16L);
		Assert.assertEquals(combined.getErrorCount(), 14L);
		Assert.assertEquals(combined.getCounts(), Collections.singletonMap("foo", 7L));
	}
	
	@Test
	public void testEvaluate() {
		Row row = new Row(Arrays.asList(new Cell("foo", null)));
		StringValuesFacetState first = SUT.withRow(initial, 1L, row);
		
		Assert.assertEquals(first.getBlankCount(), 0L);
		Assert.assertEquals(first.getErrorCount(), 0L);
		Assert.assertEquals((Object)first.getCounts().get("foo"), 1L);
		
		StringValuesFacetState second = SUT.withRow(first, 2L, row);
		
		Assert.assertEquals(second.getBlankCount(), 0L);
		Assert.assertEquals(second.getErrorCount(), 0L);
		Assert.assertEquals((Object)second.getCounts().get("foo"), 2L);
	}
	
	@Test
	public void testEvaluateBlank() {
		Row row = new Row(Arrays.asList(new Cell("", null)));
		StringValuesFacetState first = SUT.withRow(initial, 1L, row);
		
		Assert.assertEquals(first.getBlankCount(), 1L);
		Assert.assertEquals(first.getErrorCount(), 0L);
		Assert.assertNull(first.getCounts().get("foo"));
	}
	
	@Test
	public void testEvaluateError() {
		Row row = new Row(Arrays.asList(new Cell(new EvalError("error"), null)));
		StringValuesFacetState first = SUT.withRow(initial, 1L, row);
		
		Assert.assertEquals(first.getBlankCount(), 0L);
		Assert.assertEquals(first.getErrorCount(), 1L);
		Assert.assertNull(first.getCounts().get("foo"));
	}
}
