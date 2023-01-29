
package org.openrefine.runners.local.pll.partitioning;

import java.util.List;

/**
 * A partitionner for a PLL which was amputed from some partitions.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class CroppedPartitioner<T> implements Partitioner<T> {

    private final Partitioner<T> parent;
    private final int partitionsToDrop;
    private final boolean atEnd;

    /**
     * Constructs the partitioner based on the partitioner for the uncropped PLL.
     * 
     * @param parent
     *            the partitioner for the original PLL
     * @param partitionsToDrop
     *            the number of partitions which were dropped from that PLL
     * @param atEnd
     *            true if the partitions were dropped at the end, false if at the beginning
     * @see #crop(Partitioner, int, boolean) which attempts to preserve the partioner type
     */
    protected CroppedPartitioner(Partitioner<T> parent, int partitionsToDrop, boolean atEnd) {
        this.parent = parent;
        this.partitionsToDrop = partitionsToDrop;
        this.atEnd = atEnd;
    }

    @Override
    public int getPartition(T key) {
        int parentPartition = parent.getPartition(key);
        if (!atEnd && parentPartition < partitionsToDrop) {
            return 0;
        } else if (atEnd && parentPartition >= numPartitions()) {
            return numPartitions() - 1;
        } else {
            return atEnd ? parentPartition : parentPartition - partitionsToDrop;
        }
    }

    @Override
    public int numPartitions() {
        return parent.numPartitions() - partitionsToDrop;
    }

    /**
     * Constructs the partitioner based on the partitioner for the uncropped PLL.
     * 
     * @param partitioner
     *            the partitioner for the original PLL
     * @param partitionsToDrop
     *            the number of partitions which were dropped from that PLL
     * @param atEnd
     *            true if the partitions were dropped at the end, false if at the beginning
     */
    @SuppressWarnings("unchecked")
    public static <T> Partitioner<T> crop(Partitioner<T> partitioner, int partitionsToDrop, boolean atEnd) {
        if (partitioner instanceof LongRangePartitioner) {
            List<Long> firstKeys = ((LongRangePartitioner) partitioner).firstKeys;
            return (Partitioner<T>) new LongRangePartitioner(partitioner.numPartitions() - partitionsToDrop,
                    atEnd ? firstKeys.subList(0, firstKeys.size() - partitionsToDrop)
                            : firstKeys.subList(partitionsToDrop, firstKeys.size()));
        }
        return new CroppedPartitioner<T>(partitioner, partitionsToDrop, atEnd);
    }

}
