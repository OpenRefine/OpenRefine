package org.openrefine.operations.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;

import org.openrefine.RefineTest;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.recon.Recon.Judgment;
import org.openrefine.model.recon.Recon;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconStats;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReconStatsAggregatorTests extends RefineTest {
	
	ReconConfig reconConfig;
	
	@BeforeMethod
	public void setUpReconConfig() {
		reconConfig = mock(ReconConfig.class);
	}

	@Test
	public void testAddReconStats() {
		GridState grid = createGrid(new String[] { "foo", "bar" },
				new Serializable[][] {
			{ new Cell("a", testRecon("a", "a", Judgment.Matched)), 89L },
			{ new Cell("b", testRecon("b", "b", Judgment.New)), "unreconciled" }
		});
		ColumnModel columnModel = grid.getColumnModel().withReconConfig(1, reconConfig);
		grid = grid.withColumnModel(columnModel);
		
		ColumnModel updatedColumnModel = columnModel.withReconConfig(0, reconConfig)
				.withReconConfig(1, null)
				.withReconStats(0, ReconStats.create(2, 1, 1))
				.withReconStats(1, null);
		GridState expected = grid.withColumnModel(updatedColumnModel);
		
		assertGridEquals(ReconStatsAggregator.updateReconStats(grid, Arrays.asList(0, 1), Arrays.asList(reconConfig, reconConfig)), expected);
	}
	
    @Test
    public void testAggregator() {
    	GridState state = createGrid(new String[] {"foo", "bar"},
    			new Serializable[][] {
    		{"hello",null},
    		{1,      new Cell("recon", new Recon(0, "http://id", "http://schema").withJudgment(Judgment.New))}
    	});
        List<ReconStats> stats = state.aggregateRows(
        		new ReconStatsAggregator(Arrays.asList(0, 1)),
        		new ReconStatsAggregator.MultiReconStats(Collections.nCopies(2, ReconStats.ZERO))).stats;
        Assert.assertEquals(stats, Arrays.asList(ReconStats.create(2, 0, 0), ReconStats.create(1, 1, 0)));
    }
    
}
