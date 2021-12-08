
package org.openrefine.model.recon;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.GridState.PartialAggregation;

public class LazyReconStatsTests {

    @Test
    public void testCreateLazyReconStats() {
        GridState grid = mock(GridState.class);
        ColumnModel columnModel = mock(ColumnModel.class);
        when(grid.aggregateRowsApprox(any(), eq(ReconStats.ZERO), eq(ReconStats.SAMPLING_SIZE)))
                .thenReturn(new PartialAggregation<ReconStats>(new ReconStatsImpl(1L, 2L, 3L), 80L, false));
        when(grid.getColumnModel()).thenReturn(columnModel);
        when(columnModel.getColumnIndexByName("foo")).thenReturn(2);

        LazyReconStats reconStats = new LazyReconStats(grid, "foo");

        // initially nothing is computed
        verify(grid, times(0)).aggregateRows(any(), eq(ReconStats.ZERO));

        // then we access the stats
        Assert.assertEquals(reconStats.getNonBlanks(), 1L);
        Assert.assertEquals(reconStats.getNewTopics(), 2L);
        Assert.assertEquals(reconStats.getMatchedTopics(), 3L);
        Assert.assertEquals(reconStats, new ReconStatsImpl(1L, 2L, 3L));

        // the aggregation was done only once
        verify(grid, times(1)).aggregateRowsApprox(any(), eq(ReconStats.ZERO), eq(ReconStats.SAMPLING_SIZE));
    }
}
