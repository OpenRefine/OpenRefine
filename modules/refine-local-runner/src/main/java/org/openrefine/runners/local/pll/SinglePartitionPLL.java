
package org.openrefine.runners.local.pll;

import java.util.Collections;
import java.util.List;

import io.vavr.collection.Array;

import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;

/**
 * A PLL which wraps an iterable, with a single partition corresponding to the iterable itself. This is typically not
 * very efficient, as it prevents any meaningful parallelization of terminal operations run on this PLL or its
 * derivatives. However, this can be a useful start before the PLL is repartitioned (for instance while it is saved).
 */
public class SinglePartitionPLL<T> extends PLL<T> {

    protected final CloseableIterable<T> iterable;
    protected final long knownSize;

    public SinglePartitionPLL(PLLContext context, CloseableIterable<T> iterable, long knownSize) {
        super(context, "PLL from iterable");
        this.iterable = iterable;
        this.knownSize = knownSize;
    }

    @Override
    public boolean hasCachedPartitionSizes() {
        return knownSize >= 0 || super.hasCachedPartitionSizes();
    }

    @Override
    public Array<Long> computePartitionSizes() {
        if (knownSize >= 0) {
            return Array.of(knownSize);
        } else {
            return super.computePartitionSizes();
        }
    }

    @Override
    protected CloseableIterator<T> compute(Partition partition) {
        return iterable.iterator();
    }

    @Override
    public Array<? extends Partition> getPartitions() {
        return Array.of(LonePartition.instance);
    }

    @Override
    public List<PLL<?>> getParents() {
        return Collections.emptyList();
    }

    static class LonePartition implements Partition {

        static LonePartition instance = new LonePartition();

        @Override
        public int getIndex() {
            return 0;
        }

        @Override
        public Partition getParent() {
            return null;
        }
    }
}
