
package org.openrefine.model.local;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

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
    protected final List<CroppedPartition> partitions;

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
            List<Long> newPartitionSizes,
            int partitionsToDrop,
            long dropItems,
            boolean atEnd) {
        super(parent.getContext());
        pll = parent;
        Validate.notNull(newPartitionSizes, "Partition sizes must be provided");
        cachedPartitionSizes = newPartitionSizes;
        this.atEnd = atEnd;
        itemsToDrop = dropItems;
        partitions = new ArrayList<CroppedPartition>(parent.numPartitions() - partitionsToDrop);
        for (Partition upstreamPartition : parent.getPartitions()) {
            if ((atEnd || upstreamPartition.getIndex() >= partitionsToDrop)
                    && (!atEnd || upstreamPartition.getIndex() < parent.numPartitions() - partitionsToDrop)) {
                partitions.add(new CroppedPartition(partitions.size(), upstreamPartition));
            }
        }
    }

    @Override
    protected Stream<T> compute(Partition partition) {
        if (!atEnd && partition.getIndex() == 0 && itemsToDrop > 0) {
            return pll.compute(partition.getParent()).skip(itemsToDrop);
        } else if (atEnd && partition.getIndex() == numPartitions() - 1 && itemsToDrop > 0) {
            return pll.compute(partition.getParent()).limit(getPartitionSizes().get(numPartitions() - 1));
        } else {
            return pll.compute(partition.getParent());
        }
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return partitions;
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
