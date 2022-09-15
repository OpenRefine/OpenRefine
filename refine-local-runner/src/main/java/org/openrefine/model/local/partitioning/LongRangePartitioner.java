
package org.openrefine.model.local.partitioning;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A range partitioner for long-keyed PLLs, for convenience.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LongRangePartitioner extends RangePartitioner<Long> implements Partitioner<Long> {

    public LongRangePartitioner(int nbPartitions, List<Long> firstKeys) {
        super(nbPartitions, firstKeys, Comparator.naturalOrder());
    }

    /**
     * Returns a new version of this partitioner assuming that all keys have been shifted by the given offset.
     * 
     * @param offset
     *            the summand applied to all keys
     */
    public LongRangePartitioner shiftKeys(long offset) {
        return new LongRangePartitioner(numPartitions(),
                firstKeys.stream().map(s -> s + offset).collect(Collectors.toList()));
    }
}
