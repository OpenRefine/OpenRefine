package org.openrefine.browsing.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NumericFacetStateTests {
	
	HistogramState histogramA = new HistogramState(
			12, 3, 4, 5, -1, -3,
			new long[] { 3, 4, 0, 0, 5 }
			);
	HistogramState histogramB = new HistogramState(
			20, 3, 4, 5, 0, -1,
			new long[] { 12, 8 }
			);
	HistogramState emptyState = new HistogramState(
			0, 3, 4, 5, 0.0);
	HistogramState singleValueState = new HistogramState(
			2, 3, 4, 5, 3456.7);
	

	@Test
	public void testRescale() {
		NumericFacetState state = new NumericFacetState(histogramB, histogramA);
		
		Assert.assertEquals(state.getGlobalHistogram(), histogramB);
		Assert.assertEquals(state.getViewHistogram(), histogramA.rescale(0));
	}
	
	@Test
	public void testNoRescale() {
		NumericFacetState state = new NumericFacetState(singleValueState, emptyState);
		
		Assert.assertEquals(state.getGlobalHistogram(), singleValueState);
		Assert.assertEquals(state.getViewHistogram(), emptyState);
	}
	
	@Test
	public void testNormalizeForReporting() {
		NumericFacetState state = new NumericFacetState(histogramA, emptyState);
		
		NumericFacetState normalized = state.normalizeForReporting(0);
		
		Assert.assertEquals(normalized.getGlobalHistogram(), state.getGlobalHistogram());
		Assert.assertEquals(normalized.getViewHistogram(), new HistogramState(0, 3, 4, 5, -1, -3, new long[5]));
	}
	
	@Test
	public void testNormalizeForReportingExtend() {
		NumericFacetState state = new NumericFacetState(histogramA, new HistogramState(1, 2, 3, 4, -0.17));
		
		NumericFacetState normalized = state.normalizeForReporting(0);
		
		Assert.assertEquals(normalized.getGlobalHistogram(), histogramA);
		Assert.assertEquals(normalized.getViewHistogram(), new HistogramState(1, 2, 3, 4, -1, -3, new long[] { 0, 1, 0, 0, 0 }));
	}
}
