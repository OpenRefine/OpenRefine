
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.vavr.collection.Array;

import org.openrefine.util.CloseableIterator;

/**
 * A PLL which represents the concatenation of two others.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class UnionPLL<T> extends PLL<T> {

    private final PLL<T> first;
    private final PLL<T> second;
    private final List<Partition> partitions;

    /**
     * Constructor.
     * 
     * @param first
     *            the PLL whose elements should come first
     * @param second
     *            the PLL whose elements should come last
     */
    public UnionPLL(PLL<T> first, PLL<T> second) {
        super(first.getContext(), "Union");
        this.first = first;
        this.second = second;
        Array<? extends Partition> firstPartitions = first.getPartitions();
        Array<? extends Partition> secondPartitions = second.getPartitions();
        partitions = new ArrayList<>(firstPartitions.size() + secondPartitions.size());
        partitions.addAll(firstPartitions
                .map(p -> new UnionPartition(p.getIndex(), true, p))
                .collect(Collectors.toList()));
        partitions.addAll(secondPartitions
                .map(p -> new UnionPartition(p.getIndex() + firstPartitions.size(), false, p))
                .collect(Collectors.toList()));
    }

    @Override
    protected CloseableIterator<T> compute(Partition partition) {
        UnionPartition unionPartition = (UnionPartition) partition;
        if (unionPartition.left) {
            return first.compute(unionPartition.parent);
        } else {
            return second.compute(unionPartition.parent);
        }
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return Array.ofAll(partitions);
    }

    @Override
    protected Array<Long> computePartitionSizes() {
        return first.getPartitionSizes().appendAll(second.getPartitionSizes());
    }

    @Override
    public boolean hasCachedPartitionSizes() {
        return (first.hasCachedPartitionSizes() && second.hasCachedPartitionSizes()) || super.hasCachedPartitionSizes();
    }

    @Override
    public List<PLL<?>> getParents() {
        return Arrays.asList(first, second);
    }

    protected static class UnionPartition implements Partition {

        protected final int index;
        protected final boolean left;
        protected final Partition parent;

        protected UnionPartition(int index, boolean left, Partition parent) {
            this.index = index;
            this.left = left;
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
