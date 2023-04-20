
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.collection.Array;

import org.openrefine.util.CloseableIterator;

/**
 * A PLL which represents the concatenation of a sequence of other PLLs
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class UnionPLL<T> extends PLL<T> {

    private final List<PLL<T>> parents;
    private final List<Partition> partitions;

    /**
     * Constructor.
     * 
     * @param parents
     *            the PLLs whose elements should be concatenated, in that order. The list must be non-empty.
     */
    public UnionPLL(List<PLL<T>> parents) {
        super(parents.get(0).getContext(), "Union");
        this.parents = parents;

        partitions = new ArrayList<>();
        int index = 0;
        for (PLL<T> parent : parents) {
            int finalIndex = index;
            partitions.addAll(parent.getPartitions()
                    .map(p -> new UnionPartition(p.getIndex(), finalIndex, p))
                    .collect(Collectors.toList()));
            index++;
        }
    }

    @Override
    protected CloseableIterator<T> compute(Partition partition) {
        UnionPartition unionPartition = (UnionPartition) partition;
        PLL<T> parent = parents.get(unionPartition.parentIndex);
        return parent.compute(unionPartition.parent);
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return Array.ofAll(partitions);
    }

    @Override
    protected Array<Long> computePartitionSizes() {
        Array<Long> partitionSizes = Array.empty();
        for (PLL<T> parent : parents) {
            partitionSizes = partitionSizes.appendAll(parent.getPartitionSizes());
        }
        return partitionSizes;
    }

    @Override
    public boolean hasCachedPartitionSizes() {
        return parents.stream().allMatch(PLL::hasCachedPartitionSizes) || super.hasCachedPartitionSizes();
    }

    @Override
    public List<PLL<?>> getParents() {
        return new ArrayList<>(parents);
    }

    protected static class UnionPartition implements Partition {

        protected final int index;
        protected final int parentIndex;
        protected final Partition parent;

        protected UnionPartition(int index, int parentIndex, Partition parent) {
            this.index = index;
            this.parentIndex = parentIndex;
            this.parent = parent;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return parent;
        }

    }

}
