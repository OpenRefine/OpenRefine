
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vavr.collection.Array;
import org.apache.commons.lang3.Validate;

import org.openrefine.util.CloseableIterator;

/**
 * A PLL obtained by removing some rows at the beginning or the end of a PLL.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class CroppedPLL<T> extends PLL<T> {

    protected final PLL<T> pll;
    protected final long itemsToDrop;
    protected final boolean atEnd;
    protected final Array<CroppedPartition> partitions;
    protected final Array<Long> partitionSizes;

    /**
     * Constructs a cropped PLL by removing rows at the beginning or the end of a PLL.
     * 
     * @param parent
     *            the PLL which should be cropped
     * @param newPartitionSizes
     *            the resulting partition sizes after the cropping. This must be provided.
     * @param partitionsToDrop
     *            the number of partitions to be dropped entirely
     * @param dropItems
     *            the number of items to drop in the first partition that is not dropped
     * @param atEnd
     *            false if the partitions and items should be dropped at the beginning, true if at the end
     */
    public CroppedPLL(PLL<T> parent,
            Array<Long> newPartitionSizes,
            int partitionsToDrop,
            long dropItems,
            boolean atEnd) {
        super(parent.getContext(),
                String.format("Drop %d partitions and %d elements ", partitionsToDrop, dropItems)
                        + (atEnd ? "at the end" : "at the beginning"));
        pll = parent;
        Validate.notNull(newPartitionSizes, "Partition sizes must be provided");
        partitionSizes = newPartitionSizes;
        this.atEnd = atEnd;
        itemsToDrop = dropItems;
        List<CroppedPartition> partitionsList = new ArrayList<>(parent.numPartitions() - partitionsToDrop);
        for (Partition upstreamPartition : parent.getPartitions()) {
            if ((atEnd || upstreamPartition.getIndex() >= partitionsToDrop)
                    && (!atEnd || upstreamPartition.getIndex() < parent.numPartitions() - partitionsToDrop)) {
                partitionsList.add(new CroppedPartition(partitionsList.size(), upstreamPartition));
            }
        }
        partitions = Array.ofAll(partitionsList);
    }

    @Override
    protected CloseableIterator<T> compute(Partition partition) {
        if (!atEnd && partition.getIndex() == 0 && itemsToDrop > 0) {
            return pll.compute(partition.getParent()).drop((int) itemsToDrop);
        } else if (atEnd && partition.getIndex() == numPartitions() - 1 && itemsToDrop > 0) {
            return pll.compute(partition.getParent()).take(getPartitionSizes().get(numPartitions() - 1).intValue());
        } else {
            return pll.compute(partition.getParent());
        }
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.singletonList(pll);
    }

    @Override
    public boolean hasCachedPartitionSizes() {
        return true;
    }

    @Override
    public Array<Long> computePartitionSizes() {
        return partitionSizes;
    }

    /**
     * The difference between the parents' number of partitions and the new number of partitions in this PLL.
     */
    public int getDroppedPartitions() {
        return pll.numPartitions() - numPartitions();
    }

    protected static class CroppedPartition implements Partition {

        private final int _index;
        private final Partition _parent;

        protected CroppedPartition(int index, Partition parent) {
            _index = index;
            _parent = parent;
        }

        @Override
        public int getIndex() {
            return _index;
        }

        @Override
        public Partition getParent() {
            return _parent;
        }

    }

}
