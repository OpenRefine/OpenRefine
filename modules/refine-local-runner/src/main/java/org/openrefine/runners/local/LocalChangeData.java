
package org.openrefine.runners.local;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import com.google.common.collect.Streams;

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

public class LocalChangeData<T> implements ChangeData<T> {

    private final LocalRunner runner;
    private final PairPLL<Long, T> grid;
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
            PairPLL<Long, T> grid,
            List<Long> parentPartitionSizes,
            Callable<Boolean> complete,
            int maxConcurrency) {
        this.runner = runner;
        this.grid = grid;
        this.complete = complete;
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
    public Iterator<IndexedData<T>> iterator() {
        return grid
                .filter(tuple -> tuple.getValue() != null)
                .map(tuple -> new IndexedData<T>(tuple.getKey(), tuple.getValue()), "wrap as IndexedData")
                .stream()
                .iterator();
    }

    @Override
    public T get(long rowId) {
        List<T> rows = grid.get(rowId);
        if (rows.size() == 0) {
            return null;
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

        PLL<Tuple2<Long, T>> gridWithReporting;
        boolean useNativeProgressReporting = grid.hasCachedPartitionSizes() || parentPartitionFirstIndices == null;
        TaskSignalling taskSignalling = useNativeProgressReporting ? null : new TaskSignalling(parentSize);

        if (useNativeProgressReporting) {
            gridWithReporting = grid;
        } else {
            // we need to report progress but we do not know the partition sizes of our changedata object.
            // so we approximate progress by looking at the row numbers and assuming that the changedata
            // is evenly spread on the entire grid.

            gridWithReporting = grid.mapPartitions(
                    (idx, stream) -> wrapStreamWithProgressReporting(parentPartitionFirstIndices.get(idx), stream, taskSignalling),
                    "wrap stream with progress reporting", true);
        }
        PLL<String> serialized = gridWithReporting.map(r -> {
            try {
                return (new IndexedData<T>(r.getKey(), r.getValue()).writeAsString(serializer));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, "serialize");

        ProgressingFuture<Void> future = serialized
                .saveAsTextFileAsync(file.getAbsolutePath(), maxConcurrency);
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

    public PairPLL<Long, T> getPLL() {
        return grid;
    }

    protected static <T> Stream<Tuple2<Long, T>> wrapStreamWithProgressReporting(
            long startIdx,
            Stream<Tuple2<Long, T>> stream,
            TaskSignalling taskSignalling) {
        Iterator<Tuple2<Long, T>> iterator = new Iterator<>() {

            long lastSeen = startIdx;
            Iterator<Tuple2<Long, T>> parent = stream.iterator();

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

        };
        return Streams.stream(iterator).onClose(() -> stream.close());
    }

}
