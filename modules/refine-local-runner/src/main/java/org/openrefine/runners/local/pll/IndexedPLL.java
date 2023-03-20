
package org.openrefine.runners.local.pll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

import org.openrefine.runners.local.pll.partitioning.LongRangePartitioner;
import org.openrefine.runners.local.pll.partitioning.Partitioner;

/**
 * A PLL indexed in sequential order. For each entry, the key is the index of the element in the list. This comes with a
 * partitioner which makes it more efficient to retrieve elements by index than scanning the entire collection.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public class IndexedPLL<T> extends PLL<Tuple2<Long, T>> {

    /**
     * Create an indexed PLL by indexing an existing PLL. This triggers a task to count the number of elements in all
     * partitions but the last one.
     * 
     * @param <T>
     * @param pll
     * @return
     */
    public static <T> PairPLL<Long, T> index(PLL<T> pll) {
        // Compute the number of elements per partition
        List<? extends Partition> partitions = pll.getPartitions();

        // compute the number of elements, which might explicitly enumerate each partition if these counts have not been
        // cached yet
        List<Long> numElements = pll.getPartitionSizes().stream().limit(partitions.size() - 1).collect(Collectors.toList());

        PLL<Tuple2<Long, T>> zippedPLL = new IndexedPLL<T>(pll);

        long offset = 0;
        List<Long> firstKeys = new ArrayList<>(numElements.size());
        for (int i = 0; i != numElements.size(); i++) {
            offset += numElements.get(i);
            firstKeys.add(offset);
        }
        Partitioner<Long> partitioner = new LongRangePartitioner(pll.numPartitions(), firstKeys);
        return new PairPLL<Long, T>(zippedPLL, Optional.of(partitioner), pll.getPartitionSizes());
    }

    private final List<IndexedPLL.IndexedPartition> partitions;
    private final PLL<T> parent;

    protected IndexedPLL(PLL<T> parent) {
        super(parent.getContext(), "Add indices");
        this.parent = parent;
        List<Long> numElements = parent.getPartitionSizes();
        List<? extends Partition> parentPartitions = parent.getPartitions();
        partitions = new ArrayList<>(parent.numPartitions());
        long offset = 0;
        for (int i = 0; i != parent.numPartitions(); i++) {
            partitions.add(new IndexedPartition(i, offset, parentPartitions.get(i)));
            if (i != parent.numPartitions() - 1) {
                offset += numElements.get(i);
            }
        }
    }

    @Override
    protected Stream<Tuple2<Long, T>> compute(Partition partition) {
        IndexedPartition indexedPartition = (IndexedPartition) partition;
        Stream<T> upstream = parent.compute(indexedPartition.parent);
        return Streams.mapWithIndex(upstream, (t, i) -> Tuple2.of(indexedPartition.offset + i, t));
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.singletonList(parent);
    }

    protected static class IndexedPartition implements Partition {

        protected final int partitionIndex;
        protected final long offset;
        protected final Partition parent;

        protected IndexedPartition(int partitionIndex, long offset, Partition parent) {
            this.partitionIndex = partitionIndex;
            this.offset = offset;
            this.parent = parent;
        }

        @Override
        public int getIndex() {
            return partitionIndex;
        }

        @Override
        public Partition getParent() {
            return parent;
        }
    }

}
