package org.openrefine.browsing.facets;

import java.util.HashMap;
import java.util.Map;

import org.openrefine.browsing.util.StringValuesFacetState;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

public class AllFacetsStateTests {
	
	AllFacetsState SUT;
	ImmutableList<FacetState> facets;
	
	@BeforeTest
	public void setUp() {
		Map<String, Long> counts = new HashMap<>();
		counts.put("foo", 1L);
		counts.put("bar", 2L);
		facets = ImmutableList.of(new StringValuesFacetState(counts, 1, 0));
		SUT = new AllFacetsState(facets, 4L, 3L);
	}
	
	@Test
	public void testAccessors() {
		Assert.assertEquals(SUT.getAggregatedCount(), 4L);
		Assert.assertEquals(SUT.getFilteredCount(), 3L);
		Assert.assertEquals(SUT.getStates(), facets);
		Assert.assertEquals(SUT.get(0), facets.get(0));
		Assert.assertEquals(SUT.size(), 1);
	}
	
	@Test
	public void testToString() {
		Assert.assertTrue(SUT.toString().contains("AllFacetsState"));
	}
	
	@Test
	public void testEquals() {
		Assert.assertNotEquals(SUT, 43L);
		Assert.assertEquals(SUT, new AllFacetsState(facets, 4L, 3L));
	}
}
