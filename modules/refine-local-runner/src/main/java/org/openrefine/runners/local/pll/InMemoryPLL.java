
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.vavr.collection.Array;

import org.openrefine.process.ProgressingFuture;
import org.openrefine.process.ProgressingFutures;
import org.openrefine.util.CloseableIterator;

/**
 * A PLL which is created out of a regular Java collection. The collection is split into contiguous partitions which can
 * be enumerated from independently.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class InMemoryPLL<T> extends PLL<T> {

    protected final ArrayList<T> list;
    protected final Array<InMemoryPartition> partitions;

    public InMemoryPLL(PLLContext context, Collection<T> elements, int nbPartitions) {
        super(context, String.format("Load %d elements into %d partitions", elements.size(), nbPartitions));
        list = elements instanceof ArrayList ? (ArrayList<T>) elements : new ArrayList<T>(elements);
        partitions = createPartitions(list.size(), nbPartitions);
        cachedPartitions = partitions
                .map(p -> Array.ofAll(list.subList(p.offset, p.offset + p.length)));
    }

    @Override
    public CloseableIterator<T> compute(Partition partition) {
        InMemoryPartition imPartition = (InMemoryPartition) partition;

        return CloseableIterator.wrapping(list.subList(imPartition.offset, imPartition.offset + imPartition.length)
                .iterator());
    }

    @Override
    protected Array<Long> computePartitionSizes() {
        return partitions.map(p -> (long) p.length);
    }

    @Override
    public boolean hasCachedPartitionSizes() {
        return true;
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public ProgressingFuture<Void> cacheAsync() {
        return ProgressingFutures.immediate(null);
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public void uncache() {
        ; // does not do anything
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.emptyList();
    }

    protected static class InMemoryPartition implements Partition {

        protected int offset;
        protected int index;
        protected int length;

        protected InMemoryPartition(int index, int offset, int length) {
            this.index = index;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public Partition getParent() {
            return null;
        }
    }

    /**
     * Computes the list of partitions given the size of the collection and the desired number of partitions.
     * 
     * @param size
     * @param nbPartitions
     * @return
     */
    protected static Array<InMemoryPartition> createPartitions(int size, int nbPartitions) {
        if (nbPartitions < 0) {
            throw new IllegalArgumentException("The number of partitions cannot be negative");
        }
        if (nbPartitions == 0) {
            if (size == 0) {
                return Array.empty();
            } else {
                throw new IllegalArgumentException("At least one partition is required to represent a non-empty list");
            }
        }
        int partitionSize = size / nbPartitions;
        int extraElements = size - nbPartitions * partitionSize;
        int offset = 0;
        List<InMemoryPartition> partitions = new ArrayList<>();
        for (int i = 0; i != nbPartitions; i++) {
            int currentPartitionSize = partitionSize;
            if (extraElements > 0) {
                currentPartitionSize++;
                extraElements--;
            }
            partitions.add(new InMemoryPartition(i, offset, currentPartitionSize));
            offset += currentPartitionSize;
        }
        return Array.ofAll(partitions);
    }

}
