package org.openrefine.browsing.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TimeRangeStatisticsTest {
	
	TimeRangeStatistics statsA = new TimeRangeStatistics(3, 5L, 6L, 7L, new long[] { 3782948127L, 127382L, 5674873L, 0L });
	
	@Test
	public void testAddCounts() {
		TimeRangeStatistics added = statsA.addCounts(3L, 4L, 5L);
		
		Assert.assertEquals(added.getTimeCount(), statsA.getTimeCount());
		Assert.assertEquals(added.getNonTimeCount(), statsA.getNonTimeCount() + 3L);
		Assert.assertEquals(added.getBlankCount(), statsA.getBlankCount() + 4L);
		Assert.assertEquals(added.getErrorCount(), statsA.getErrorCount() + 5L);
		Assert.assertEquals(added.getRawTimeValues(), statsA.getRawTimeValues());
	}
	
	@Test
	public void testAddTimeValue() {
		TimeRangeStatistics added = statsA.addTime(123456L);
		
		Assert.assertEquals(added.getRawTimeValues(),
				new long[] {3782948127L, 127382L, 5674873L, 123456L});
		Assert.assertEquals(added.getTimeCount(), statsA.getTimeCount() + 1);
		
		added = added.addTime(67890L);
		
		// we add a bit of space behind to avoid allocating a new array at each allocation
		Assert.assertEquals(added.getRawTimeValues(),
				new long[] {3782948127L, 127382L, 5674873L, 123456L, 67890L, 0L, 0L, 0L });
		Assert.assertEquals(added.getTimeCount(), statsA.getTimeCount() + 2);
	}
	
	@Test
	public void testSum() {
		TimeRangeStatistics sum = statsA.sum(statsA);
		
		Assert.assertEquals(sum, new TimeRangeStatistics(6, 10L, 12L, 14L,
				new long[] {3782948127L, 127382L, 5674873L, 3782948127L, 127382L, 5674873L }));
	}
	
	@Test
	public void testEquals() {
		Assert.assertNotEquals(statsA, 34L);
		Assert.assertEquals(statsA, statsA);
	}
	
	@Test
	public void testToString() {
		Assert.assertTrue(statsA.toString().contains("TimelineStatistics"));
	}
}
