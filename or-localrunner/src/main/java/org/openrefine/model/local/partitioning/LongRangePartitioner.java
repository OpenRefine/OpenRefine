
package org.openrefine.model.local.partitioning;

import java.util.Comparator;
import java.util.List;

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

}
