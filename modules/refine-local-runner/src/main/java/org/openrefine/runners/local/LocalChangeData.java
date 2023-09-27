
package org.openrefine.runners.local;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

import io.vavr.collection.Array;

import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.runners.local.pll.PLL;
import org.openrefine.runners.local.pll.PairPLL;
import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.local.pll.util.ProgressingFutureWrapper;
import org.openrefine.runners.local.pll.util.TaskSignalling;
import org.openrefine.util.CloseableIterator;

public class LocalChangeData<T> implements ChangeData<T> {

    private final LocalRunner runner;
    private final PairPLL<Long, IndexedData<T>> grid;
    private final List<Long> parentPartitionFirstIndices;
    private final Long parentSize;
    private final Callable<Boolean> complete;
    private final int maxConcurrency;

    /**
     * Constructs a change data.
     *
     * @param grid
     *            expected not to contain any null value (they should be filtered out first)
     * @param parentPartitionSizes
     *            the size of each partition in the grid this change data was generated from (can be null if not
     *            available). This is used to compute progress as a percentage of the original grid swept through. This
     *            is more efficient than counting the number of elements in each partition of the change data.
     * @param maxConcurrency
     *            the maximum number of concurrent calls to the underlying resource (fetcher). This is respected when
     *            saving the change data to a file.
     */
    public LocalChangeData(
            LocalRunner runner,
            PairPLL<Long, IndexedData<T>> grid,
            Array<Long> parentPartitionSizes,
            Callable<Boolean> complete,
            int maxConcurrency) {
        this.runner = runner;
        this.grid = grid;
        this.complete = complete; // TODO should just be a boolean, because if it's not complete when creating it we
        // should not assume that it gets complete later on (if we have empty/missing partitions when creating it they
        // will
        // not appear afterwards).
        this.maxConcurrency = maxConcurrency;
        if (parentPartitionSizes == null) {
            parentPartitionFirstIndices = null;
            parentSize = null;
        } else {
            parentPartitionFirstIndices = new ArrayList<>(parentPartitionSizes.size());
            long currentIndex = 0;
            for (Long partitionSize : parentPartitionSizes) {
                parentPartitionFirstIndices.add(currentIndex);
                currentIndex += partitionSize;
            }
            parentSize = currentIndex;
        }
    }

    @Override
    public CloseableIterator<IndexedData<T>> iterator() {
        return grid
                .values()
                .iterator();
    }

    @Override
    public IndexedData<T> get(long rowId) {
        Array<IndexedData<T>> rows = grid.get(rowId);
        if (rows.size() == 0) {
            return isComplete() ? new IndexedData<>(rowId, null) : new IndexedData<>(rowId);
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d change data elements at index %d", rows.size(), rowId));
        } else {
            return rows.get(0);
        }
    }

    @Override
    public Runner getRunner() {
        return runner;
    }

    @Override
    public ProgressingFuture<Void> saveToFileAsync(File file, ChangeDataSerializer<T> serializer) {

        PLL<Tuple2<Long, IndexedData<T>>> gridWithReporting;
        boolean useNativeProgressReporting = grid.hasCachedPartitionSizes() || parentPartitionFirstIndices == null;
        TaskSignalling taskSignalling = useNativeProgressReporting ? null : new TaskSignalling(parentSize);

        if (useNativeProgressReporting) {
            gridWithReporting = grid;
        } else {
            // we need to report progress, but we do not know the partition sizes of our changedata object.
            // so we approximate progress by looking at the row numbers and assuming that the changedata
            // is evenly spread on the entire grid.
            gridWithReporting = grid.mapPartitions(
                    (idx, stream) -> wrapStreamWithProgressReporting(parentPartitionFirstIndices.get(idx), stream, taskSignalling),
                    "wrap stream with progress reporting", true);
        }
        PLL<String> serialized = gridWithReporting
                .filter(tuple -> tuple.getValue().getData() != null)
                .map(r -> {
                    try {
                        return r.getValue().writeAsString(serializer);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, "serialize")
                .mapPartitions((index, iterator) -> iterator.concat(CloseableIterator.of(ChangeData.partitionEndMarker)),
                        "add end marker", false);

        // we do not want to repartition while saving because the partitions should ideally correspond exactly
        // to those of the parent grid, for efficient joining.
        ProgressingFuture<Void> future = serialized
                .saveAsTextFileAsync(file.getAbsolutePath(), maxConcurrency, false, true);
        if (useNativeProgressReporting) {
            return future;
        } else {
            // override the progress of the existing future with approximated progress from the iteration of the stream
            return new ProgressingFutureWrapper<>(future, taskSignalling, true);
        }
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException, InterruptedException {
        try {
            saveToFileAsync(file, serializer).get();
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    @Override
    public boolean isComplete() {
        try {
            return complete.call();
        } catch (Exception e) {
            return false;
        }
    }

    public PairPLL<Long, IndexedData<T>> getPLL() {
        return grid;
    }

    protected static <T> CloseableIterator<Tuple2<Long, T>> wrapStreamWithProgressReporting(
            long startIdx,
            CloseableIterator<Tuple2<Long, T>> iterator,
            TaskSignalling taskSignalling) {
        return new CloseableIterator<>() {

            long lastSeen = startIdx;
            final Iterator<Tuple2<Long, T>> parent = iterator.iterator();

            @Override
            public boolean hasNext() {
                try {
                    taskSignalling.yield();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return parent.hasNext();
            }

            @Override
            public Tuple2<Long, T> next() {
                Tuple2<Long, T> element = parent.next();
                taskSignalling.addProcessedElements(element.getKey() - lastSeen);
                lastSeen = element.getKey();
                return element;
            }

            @Override
            public void close() {
                iterator.close();
            }

        };
    }

}
