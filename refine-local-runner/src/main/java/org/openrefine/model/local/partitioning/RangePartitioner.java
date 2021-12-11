
package org.openrefine.model.local.partitioning;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;

/**
 * A partitioner which assumes that the keys are sorted in increasing order.
 * 
 * @author Antonin Delpeuch
 *
 */
public class RangePartitioner<T> implements Partitioner<T> {

    protected final int numPartitions;
    protected final List<T> firstKeys;
    protected final Comparator<T> comparator;

    /**
     * Constructs a partitioner by supplying the first key value in each partition but the first one.
     * 
     * @param nbPartitions
     *            the number of partitions of the PLL this partitioner is built for
     * @param firstKeys
     *            list of (n-1) values, where n is the number of partitions. The first element of the list is the first
     *            key value in the second partition. If a partition is empty, null is used.
     * @param comparator
     *            the comparator used to compare the keys
     */
    public RangePartitioner(int nbPartitions, List<T> firstKeys, Comparator<T> comparator) {
        this.numPartitions = nbPartitions;
        this.firstKeys = firstKeys;
        this.comparator = comparator;
        Validate.isTrue(nbPartitions == 0 || firstKeys.size() == nbPartitions - 1);
    }

    @Override
    public int getPartition(T key) {
        if (numPartitions <= 1) {
            return 0;
        }

        // TODO use binary search if numPartitions > 128, as Spark does
        int idx = 0;
        int lastNonEmpty = 0;
        while (idx + 1 < numPartitions && (firstKeys.get(idx) == null ||
                comparator.compare(firstKeys.get(idx), key) <= 0)) {
            if (firstKeys.get(idx) != null) {
                lastNonEmpty = idx + 1;
            }
            idx++;
        }
        return lastNonEmpty;
    }

    @Override
    public int numPartitions() {
        return numPartitions;
    }

    public List<Optional<T>> getFirstKeys() {
        return firstKeys
                .stream()
                .map(o -> o == null ? Optional.<T> empty() : Optional.of(o))
                .collect(Collectors.toList());
    }

}
