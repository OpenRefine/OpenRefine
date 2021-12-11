package org.openrefine.browsing.util;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ScatterplotFacetStateTests {
	
	ScatterplotFacetState SUT;
	
	@BeforeTest
	public void setUp() {
		SUT = new ScatterplotFacetState(
				new double[] { -2, 7.8, 1.54, 0 },
				new double[] { 3, -7,  2.3, 0 },
				new boolean[] {true, false, true, false},
				3);
	}
	
	@Test
	public void testAddValues() {
		Assert.assertEquals(SUT.addValue(4.5, 6.7, true), new ScatterplotFacetState(
				new double[] { -2, 7.8, 1.54, 4.5 },
				new double[] { 3, -7, 2.3, 6.7 },
				new boolean[] { true, false, true, true },
				4));
	}
	
	@Test
	public void testGetters() {
		Assert.assertEquals(SUT.getValuesX(), new double[] { -2, 7.8, 1.54 });
		Assert.assertEquals(SUT.getValuesY(), new double[] { 3, -7, 2.3 });
		Assert.assertEquals(SUT.getInView(), new boolean[] { true, false, true });
		Assert.assertEquals(SUT.getValuesCount(), 3);
	}
	
	@Test
	public void testEquals() {
		Assert.assertNotEquals(SUT, 34);
		Assert.assertEquals(SUT, SUT);
	}
	
	@Test
	public void testToString() {
		Assert.assertTrue(SUT.toString().contains("ScatterplotFacetState"));
	}
}
