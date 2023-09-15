
package org.openrefine.model.local;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A PLL obtained by transforming each partition independently. The supplied function takes two arguments: the first is
 * the index of the partition to map, the second is a stream of its contents.
 * 
 * @author Antonin Delpeuch
 *
 * @param <U>
 * @param <T>
 */
public class MapPartitionsPLL<U, T> extends PLL<T> {

    protected final PLL<U> parent;
    protected final BiFunction<Integer, Stream<U>, Stream<T>> mapFunction;

    /**
     * Constructs a PLL with partitions derived one-to-one from the parent.
     * 
     * @param parent
     *            the source list
     * @param mapFunction
     *            the function to apply to each partition
     */
    public MapPartitionsPLL(PLL<U> parent, BiFunction<Integer, Stream<U>, Stream<T>> mapFunction) {
        super(parent.getContext());
        this.parent = parent;
        this.mapFunction = mapFunction;
    }

    /**
     * Constructs a PLL with partitions derived one-to-one from the parent.
     * 
     * @param parent
     *            the source list
     * @param mapFunction
     *            the function to apply to each partition
     * @param cachedPartitionSizes
     *            the list of partition sizes, if known (otherwise null)
     */
    protected MapPartitionsPLL(
            PLL<U> parent,
            BiFunction<Integer, Stream<U>, Stream<T>> mapFunction,
            List<Long> cachedPartitionSizes) {
        super(parent.getContext());
        this.parent = parent;
        this.mapFunction = mapFunction;
        this.cachedPartitionSizes = cachedPartitionSizes;
    }

    @Override
    public Stream<T> compute(Partition partition) {
        Stream<U> parentIterator = parent.iterate(partition);
        return mapFunction.apply(partition.getIndex(), parentIterator);
    }

    @Override
    public List<? extends Partition> getPartitions() {
        return parent.getPartitions();
    }

}
