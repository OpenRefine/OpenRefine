package org.openrefine.operations.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Row;
import org.openrefine.model.recon.ReconConfig;
import org.openrefine.model.recon.ReconStats;

/**
 * Helper to update the recon statistics on a set of columns.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ReconStatsAggregator implements RowAggregator<ReconStatsAggregator.MultiReconStats> {
	
	/**
	 * Updates the recon statistics on the columns designated by the supplied indices.
	 * 
	 * Only the columns that have at least one reconciled cell are updated. Their recon config
	 * is also updated to the supplied one.
	 * Other columns have their recon stats and config cleared.
	 */
	public static GridState updateReconStats(GridState grid, List<Integer> columnIndices, List<ReconConfig> reconConfigs) {
		ReconStatsAggregator aggregator = new ReconStatsAggregator(columnIndices);
		List<ReconStats> reconStats = grid.aggregateRows(aggregator, new MultiReconStats(Collections.nCopies(columnIndices.size(), ReconStats.ZERO))).stats;
		ColumnModel columnModel = grid.getColumnModel();
		for (int i = 0; i != columnIndices.size(); i++) {
			int columnIndex = columnIndices.get(i);
			ReconStats currentReconStats = reconStats.get(i);
			ReconConfig config = reconConfigs.get(i);
			if (currentReconStats.getMatchedTopics() + currentReconStats.getNewTopics() == 0L) {
				currentReconStats = null;
				config = null;
			}
			columnModel = columnModel
					.withReconStats(columnIndex, currentReconStats)
					.withReconConfig(columnIndex, config);
		}
		return grid.withColumnModel(columnModel);
	}

	/**
	 * Wrapper introduced to satisfy the type bound of aggregateRows
	 * (List<ReconStats> is not recognized as serializable on its own).
	 * @author Antonin Delpeuch
	 *
	 */
	protected static class MultiReconStats implements Serializable {
		private static final long serialVersionUID = -5709289822943713622L;
		public final List<ReconStats> stats;
		public MultiReconStats(List<ReconStats> stats) {
			this.stats = stats;
		}
	}

	private static final long serialVersionUID = -3030069835741440171L;
	private final List<Integer> columnIds;

	protected ReconStatsAggregator(List<Integer> columnIds) {
		this.columnIds = columnIds;
	}

	@Override
	public MultiReconStats sum(MultiReconStats first, MultiReconStats second) {
		List<ReconStats> sum = new ArrayList<>(first.stats.size());
		for(int i = 0; i != first.stats.size(); i++) {
			sum.add(first.stats.get(i).sum(second.stats.get(i)));
		}
		return new MultiReconStats(sum);
	}

	@Override
	public MultiReconStats withRow(MultiReconStats state, long rowId, Row row) {
		List<ReconStats> sum = new ArrayList<>(state.stats.size());
		for(int i = 0; i != state.stats.size(); i++) {
			ReconStats stats = state.stats.get(i);
			sum.add(stats.withRow(row, columnIds.get(i)));
		}
		return new MultiReconStats(sum);
	}
}