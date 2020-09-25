package org.openrefine.browsing.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.openrefine.browsing.facets.FacetResult;
import org.openrefine.browsing.facets.FacetState;
import org.openrefine.browsing.filters.AllRowsRecordFilter;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.model.Cell;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ExpressionValueFacetAggregatorTest {
	
	protected static class DummyState implements FacetState, FacetResult {

		private static final long serialVersionUID = 1L;
		
		// List of aggregated values
		public List<Object> _values;
		
		public DummyState() {
			_values = Collections.emptyList();
		}
		
		public DummyState(List<Object> values) {
			_values = values;
		}
		
		public DummyState addElement(Object element) {
			List<Object> newValues = new ArrayList<>(_values);
			newValues.add(element);
			return new DummyState(newValues);
		}
	}
	
	protected static class AggregatorStub extends ExpressionValueFacetAggregator<DummyState> {

		private static final long serialVersionUID = 1L;

		public AggregatorStub(boolean invert, RowEvaluable eval) {
			super(invert, eval);
		}

		@Override
		public RowFilter getRowFilter() {
			return RowFilter.ANY_ROW;
		}

		@Override
		public DummyState sum(DummyState first, DummyState second) {
			List<Object> newValues = new ArrayList<>(first._values);
			newValues.addAll(second._values);
			return new DummyState(newValues);
		}

		@Override
		protected DummyState withValue(DummyState state, Object value, boolean inView) {
			if (inView) {
				return state.addElement(value);
			} else {
				return state;
			}
		}
		
	}
	
	ExpressionValueFacetAggregator<DummyState> SUT;
	RowEvaluable eval;
	
	@BeforeTest
	public void setUpAggregator() {
		eval = new RowEvaluable() {

			private static final long serialVersionUID = 7910046832222123009L;

			@Override
			public Object eval(long rowIndex, Row row, Properties bindings) {
				return row.getCellValue(0);
			}
			
		};
		SUT = new AggregatorStub(false, eval);
	}

	@Test
	public void testAggregatePlainValue() {
		DummyState state = SUT.withRow(new DummyState(), 1234L, new Row(Collections.singletonList(new Cell("foo", null))));
		
		Assert.assertEquals(state._values, Collections.<Object>singletonList("foo"));
	}
	
	@Test
	public void testAggregateArray() {
		// This is somewhat incorrect since at the moment arrays cannot be stored in cells directly by users
		// If we enforce this in the Cell class, this test should be adapted to just make the evaluator return an array instead
		DummyState state = SUT.withRow(new DummyState(), 1234L, new Row(Collections.singletonList(new Cell(new Object[] {"foo", 12345L}, null))));
		
		Assert.assertEquals(state._values, Arrays.asList("foo", 12345L));
	}
	
	@Test
	public void testAggregateList() {
		// This is somewhat incorrect since at the moment arrays cannot be stored in cells directly by users
		// If we enforce this in the Cell class, this test should be adapted to just make the evaluator return an array instead
		DummyState state = SUT.withRow(new DummyState(Collections.singletonList("hey")), 1234L, new Row(Collections.singletonList(
				new Cell((Serializable)Arrays.<Object>asList("foo", 12345L), null))));
		
		Assert.assertEquals(state._values, Arrays.asList("hey", "foo", 12345L));
	}
	
	@Test
	public void testRecordFilter() {
		Assert.assertTrue(SUT.getRecordFilter() instanceof AnyRowRecordFilter);
	}
	
	@Test
	public void testRecordFilterInvert() {
		Assert.assertTrue(new AggregatorStub(true, eval).getRecordFilter() instanceof AllRowsRecordFilter);
	}
}
