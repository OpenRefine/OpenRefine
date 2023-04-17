
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vavr.collection.Array;

import org.openrefine.util.CloseableIterator;

/**
 * A PLL obtained by dropping some partitions from the parent PLL. This PLL may also reorder the partitions of the
 * parent PLL. It is primarily used when sampling from large PLLs where we do not want to sample from every partition
 * (for instance because the desired sample size is smaller than the number of partitions).
 */
public class DropPartitionsPLL<T> extends PLL<T> {

    protected final Array<Partition> partitions;
    protected final PLL<T> parent;

    /**
     * Constructor.
     *
     * @param parent
     *            the parent PLL to drop partitions of
     * @param retainedPartitionIds
     *            the list of partition indices to retain. The partitions will be ordered according to this list of
     *            indices.
     */
    public DropPartitionsPLL(PLL<T> parent, List<Integer> retainedPartitionIds) {
        super(parent.getContext(),
                String.format("Keep %d partitions out of %d", retainedPartitionIds.size(), parent.getPartitions().size()));
        this.parent = parent;
        List<Partition> newPartitions = new ArrayList(retainedPartitionIds.size());
        Array<? extends Partition> originalPartitions = parent.getPartitions();
        for (Integer originalIndex : retainedPartitionIds) {
            if (originalIndex < 0 || originalIndex >= originalPartitions.size()) {
                throw new IllegalArgumentException(
                        String.format("Illegal partition id %d out of %d parent partitions", originalIndex, originalPartitions.size()));
            }
            newPartitions.add(new PartitionLink(newPartitions.size(), originalPartitions.get(originalIndex)));
        }
        this.partitions = Array.ofAll(newPartitions);
    }

    @Override
    protected CloseableIterator<T> compute(Partition partition) {
        return parent.compute(partition.getParent());
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.singletonList(parent);
    }

    protected static class PartitionLink implements Partition {

        protected final int index;
        protected final Partition originalPartition;

        protected PartitionLink(int index, Partition originalPartition) {
            this.index = index;
            this.originalPartition = originalPartition;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return originalPartition;
        }
    }
}
