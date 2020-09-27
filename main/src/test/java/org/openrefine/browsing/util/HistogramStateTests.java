package org.openrefine.browsing.util;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class HistogramStateTests {

	HistogramState multiValuesState;
	HistogramState emptyState;
	HistogramState singleValueState;
	
	@BeforeClass
	public void setUpFacetState() {
		multiValuesState = new HistogramState(
				12, 3, 4, 5, -1, -3,
				new long[] { 3, 4, 0, 0, 5 }
				);
		emptyState = new HistogramState(
				0, 3, 4, 5, 0.0);
		singleValueState = new HistogramState(
				2, 3, 4, 5, 3456.7);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testRescaleFiner() {
		multiValuesState.rescale(-2);
	}
	
	@Test
	public void testRescaleIdentical() {
		Assert.assertEquals(multiValuesState.rescale(-1), multiValuesState);
	}
	
	@Test
	public void testRescaleCoarser() {
		HistogramState rescaled = multiValuesState.rescale(0);
		
		Assert.assertEquals(rescaled.getLogBinSize(), 0);
		Assert.assertEquals(rescaled.getBlankCount(), multiValuesState.getBlankCount());
		Assert.assertEquals(rescaled.getErrorCount(), multiValuesState.getErrorCount());
		Assert.assertEquals(rescaled.getNumericCount(), multiValuesState.getNumericCount());
		Assert.assertEquals(rescaled.getNonNumericCount(), multiValuesState.getNonNumericCount());
		Assert.assertEquals(rescaled.getMinBin(), -1);
		Assert.assertEquals(rescaled.getBins(), new long[] { 7, 5 });
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testExtendWrongBounds() {
		multiValuesState.extend(0, 1);
	}
	
	@Test(expectedExceptions = IllegalStateException.class)
	public void testExtendSingleValueState() {
		singleValueState.extend(234, 12034);
	}
	
	@Test
	public void testExtendSuccessfully() {
		HistogramState extended = multiValuesState.extend(-5, 3);
		
		Assert.assertEquals(extended.getLogBinSize(), -1);
		Assert.assertEquals(extended.getBlankCount(), multiValuesState.getBlankCount());
		Assert.assertEquals(extended.getErrorCount(), multiValuesState.getErrorCount());
		Assert.assertEquals(extended.getNumericCount(), multiValuesState.getNumericCount());
		Assert.assertEquals(extended.getNonNumericCount(), multiValuesState.getNonNumericCount());
		Assert.assertEquals(extended.getMinBin(), -5);
		Assert.assertEquals(extended.getBins(), new long[] { 0, 0, 3, 4, 0, 0, 5, 0 });
	}
	
	@Test
	public void testEmptyState() {
		Assert.assertNull(emptyState.getBins());
		
		// rescaling an empty facet creates a single bin
		HistogramState rescaled = emptyState.rescale(3);
		
		Assert.assertEquals(rescaled.getLogBinSize(), 0);
		Assert.assertNull(rescaled.getBins());
	}
	
	@Test
	public void testSingleValueState() {
		Assert.assertNull(singleValueState.getBins());
		
		// rescaling a facet with at least one value creates explicit bins
		HistogramState rescaled = singleValueState.rescale(3);
		
		Assert.assertEquals(rescaled.getLogBinSize(), 3);
		Assert.assertEquals(rescaled.getBins(), new long[] { 2 });
		Assert.assertEquals(rescaled.getMinBin(), 3);
		Assert.assertEquals(rescaled.getNumericCount(), 2);
		
		// it does so even if the new scale is 0 (internally the default)
		rescaled = singleValueState.rescale(0);
		Assert.assertEquals(rescaled.getLogBinSize(), 0);
		Assert.assertEquals(rescaled.getBins(), new long[] { 2 });
		Assert.assertEquals(rescaled.getMinBin(), 3456);
		Assert.assertEquals(rescaled.getNumericCount(), 2);
	}
	
	@Test
	public void testIncrementNonNumeric() {
		HistogramState incremented = emptyState.addCounts(1, 0, 0);
		Assert.assertEquals(incremented.getNonNumericCount(), emptyState.getNonNumericCount() + 1);
		Assert.assertNull(incremented.getBins());
		
		incremented = multiValuesState.addCounts(1, 0, 0);
		Assert.assertEquals(incremented.getNonNumericCount(), multiValuesState.getNonNumericCount() + 1);
		Assert.assertNotNull(incremented.getBins());
	}
	
	@Test
	public void testIncrementError() {
		HistogramState incremented = emptyState.addCounts(0, 1, 0);
		Assert.assertEquals(incremented.getErrorCount(), emptyState.getErrorCount() + 1);
		Assert.assertNull(incremented.getBins());
		
		incremented = multiValuesState.addCounts(0, 1, 0);
		Assert.assertEquals(incremented.getErrorCount(), multiValuesState.getErrorCount() + 1);
		Assert.assertNotNull(incremented.getBins());
	}
	
	@Test
	public void testIncrementBlank() {
		HistogramState incremented = emptyState.addCounts(0, 0, 1);
		Assert.assertEquals(incremented.getBlankCount(), emptyState.getBlankCount() + 1);
		Assert.assertNull(incremented.getBins());
		
		incremented = multiValuesState.addCounts(0, 0, 1);
		Assert.assertEquals(incremented.getBlankCount(), multiValuesState.getBlankCount() + 1);
		Assert.assertNotNull(incremented.getBins());
	}
	
	@Test
	public void testEquals() {
		Assert.assertFalse(emptyState.equals(67));
		Assert.assertFalse(emptyState.equals(multiValuesState));
		Assert.assertTrue(emptyState.equals(emptyState));
		Assert.assertEquals(emptyState.hashCode(), emptyState.hashCode());
	}
	
	@Test
	public void testToString() {
		Assert.assertTrue(emptyState.toString().contains("numeric"));
	}
}
