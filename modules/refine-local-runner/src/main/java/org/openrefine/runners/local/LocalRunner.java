
package org.openrefine.runners.local;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.*;
import org.openrefine.model.Grid.Metadata;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.runners.local.pll.PLL;
import org.openrefine.runners.local.pll.PLLContext;
import org.openrefine.runners.local.pll.PairPLL;
import org.openrefine.runners.local.pll.TextFilePLL;
import org.openrefine.runners.local.pll.Tuple2;
import org.openrefine.runners.local.pll.partitioning.LongRangePartitioner;
import org.openrefine.util.CloseableIterable;
import org.openrefine.util.CloseableIterator;
import org.openrefine.util.ParsingUtilities;

/**
 * The default implementation of the {@link Runner} interface. It is optimized for local (single machine) use, with
 * Grids and ChangeData being read off disk lazily. Those objects can be partitioned, allowing for concurrent processing
 * via threads.
 */
@JsonIgnoreType
public class LocalRunner implements Runner {

    final static private Logger logger = LoggerFactory.getLogger(LocalRunner.class);

    final static protected String METADATA_PATH = "metadata.json";
    final static protected String GRID_PATH = "grid";

    protected final PLLContext pllContext;

    // Partitioning strategy settings
    protected int defaultParallelism;
    protected long minSplitSize;
    protected long maxSplitSize;
    protected long minSplitRowCount;
    protected long maxSplitRowCount;

    // Caching cost estimation parameters

    // Those costs were approximated experimentally, on some sample datasets:
    // https://github.com/wetneb/refine-memory-benchmark
    protected int reconciledCellCost = 146;
    protected int unreconciledCellCost = 78;

    public LocalRunner(RunnerConfiguration configuration) {
        defaultParallelism = configuration.getIntParameter("defaultParallelism", 4);
        minSplitSize = configuration.getLongParameter("minSplitSize", 4096L);
        maxSplitSize = configuration.getLongParameter("maxSplitSize", 16777216L);
        minSplitRowCount = configuration.getLongParameter("minSplitRowCount", 32L);
        maxSplitRowCount = configuration.getLongParameter("maxSplitRowCount", 61536L);
        reconciledCellCost = configuration.getIntParameter("reconciledCellCost", 146);
        unreconciledCellCost = configuration.getIntParameter("unreconciledCellCost", 78);

        pllContext = new PLLContext(MoreExecutors.listeningDecorator(
                Executors.newCachedThreadPool()),
                defaultParallelism, minSplitSize, maxSplitSize, minSplitRowCount, maxSplitRowCount);
    }

    public LocalRunner() {
        this(RunnerConfiguration.empty);
    }

    public PLLContext getPLLContext() {
        return pllContext;
    }

    @Override
    public Grid loadGrid(File path) throws IOException {
        File metadataFile = new File(path, METADATA_PATH);
        File gridFile = new File(path, GRID_PATH);
        File completionMarker = new File(gridFile, Runner.COMPLETION_MARKER_FILE_NAME);
        if (!completionMarker.exists()) {
            throw new IOException("the grid to load is incomplete (missing completion marker)");
        }

        Metadata metadata = ParsingUtilities.mapper.readValue(metadataFile, Metadata.class);
        PairPLL<Long, Row> rows = pllContext
                .textFile(gridFile.getAbsolutePath(), GRID_ENCODING, false)
                .mapToPair(s -> parseIndexedRow(s), "parse row from JSON");
        rows = PairPLL.assumeIndexed(rows, metadata.rowCount);
        return new LocalGrid(this, rows, metadata.columnModel, metadata.overlayModels, metadata.recordCount);
    }

    protected static Tuple2<Long, Row> parseIndexedRow(String source) {
        IndexedRow id;
        try {
            id = ParsingUtilities.mapper.readValue(source, IndexedRow.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Tuple2<Long, Row>(id.getIndex(), id.getRow());
    }

    @Override
    public Grid gridFromList(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        // the call to zipWithIndex is efficient as the first PLL is in memory already
        PairPLL<Long, Row> pll = pllContext.parallelize(defaultParallelism, rows)
                .zipWithIndex();
        return new LocalGrid(this, pll, columnModel, overlayModels, -1);
    }

    @Override
    public Grid gridFromIterable(ColumnModel columnModel, CloseableIterable<Row> rows, Map<String, OverlayModel> overlayModels,
            long rowCount, long recordCount) {
        // the call to zipWithIndex is free because the PLL has a single partition
        PairPLL<Long, Row> pll = pllContext.singlePartitionPLL(rows, rowCount)
                .zipWithIndex();
        return new LocalGrid(this, pll, columnModel, overlayModels, recordCount);
    }

    @Override
    public <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer)
            throws IOException {
        File completionMarker = new File(path, Runner.COMPLETION_MARKER_FILE_NAME);
        Callable<Boolean> isComplete = () -> completionMarker.exists();
        boolean alreadyComplete = completionMarker.exists();
        PairPLL<Long, IndexedData<T>> pll = pllContext
                .textFile(path.getAbsolutePath(), GRID_ENCODING, !alreadyComplete)
                .map(line -> {
                    try {
                        return IndexedData.<T> read(line, serializer);
                    } catch (IOException e) {
                        if (alreadyComplete) {
                            throw new UncheckedIOException(e);
                        } else {
                            return null;
                        }
                    }
                }, "deserialize")
                .mapPartitions((index, partition) -> partition.takeWhile(indexedData -> indexedData != null),
                        "filter out incomplete records", false)
                .mapToPair(indexedData -> Tuple2.of(indexedData.getId(), indexedData), "indexed data to Tuple2");
        pll = PairPLL.assumeSorted(pll);

        if (!alreadyComplete) {
            if (pll.getPartitions().isEmpty()) {
                return emptyChangeData();
            } else {
                // we know the partitioner is present because we just sorted the pll above
                LongRangePartitioner partitioner = (LongRangePartitioner) pll.getPartitioner().get();
                // Compute up to which index we should fill up the PLL with pending records
                List<Optional<Long>> firstKeys = partitioner.getFirstKeys();
                List<Long> upperBounds = incompleteUpperBounds(firstKeys);
                pll = pll
                        .mapPartitions(
                                (idx, iterator) -> fillWithIncompleteIndexedData(iterator, idx == 0 ? 0L : -1L, upperBounds.get(idx)),
                                "add pending records to change data", false)
                        .mapToPair(tuple -> tuple, "bureaucratic map to pair")
                        .withPartitioner(Optional.of(partitioner));
            }
        }

        return new LocalChangeData<T>(this, pll, null, isComplete, 0);
    }

    /**
     * Given a list of first keys found in the last (n-1) partitions, where n is the total number of partitions of a
     * ChangeData object, compute a list of size n which contains for each partition up to which upper bound it should
     * be filled up with pending change data objects.
     */
    protected static List<Long> incompleteUpperBounds(List<Optional<Long>> firstKeys) {
        List<Long> upperBounds = new ArrayList<>(firstKeys.size());
        upperBounds.add(Long.MAX_VALUE); // the last partition is always unbounded
        long lastBound = Long.MAX_VALUE;
        for (int i = firstKeys.size() - 1; i >= 0; i--) {
            Optional<Long> firstKey = firstKeys.get(i);
            if (firstKey.isPresent()) {
                lastBound = firstKey.get();
            }
            upperBounds.add(lastBound);
        }
        Collections.reverse(upperBounds);
        return upperBounds;
    }

    /**
     * Utility function used to pad a stream of elements read from an incomplete change data with "virtual", pending
     * IndexedData elements to represent those which may be computed later.
     *
     * @param upperBound
     *            a strict upper bound on the indices to enumerate, or Long.MAX_VALUE if unbounded
     */
    protected static <T> CloseableIterator<Tuple2<Long, IndexedData<T>>> fillWithIncompleteIndexedData(
            CloseableIterator<Tuple2<Long, IndexedData<T>>> originalIterator, long initialIndex, long upperBound) {
        return new CloseableIterator<>() {

            long nextIndex = initialIndex;

            @Override
            public boolean hasNext() {
                // if the parent iterator is empty, then we cannot extrapolate with pending elements because
                // we have not seen any element.
                // Otherwise, we are generating an infinite stream, so we always have a next element.
                return originalIterator.hasNext() || (nextIndex != -1L && nextIndex < upperBound);
            }

            @Override
            public Tuple2<Long, IndexedData<T>> next() {
                if (originalIterator.hasNext()) {
                    Tuple2<Long, IndexedData<T>> next = originalIterator.next();
                    nextIndex = next.getKey() + 1;
                    return next;
                } else if (nextIndex != -1L && nextIndex < upperBound) {
                    IndexedData<T> pendingIndexedData = new IndexedData<>(nextIndex++);
                    return Tuple2.of(pendingIndexedData.getId(), pendingIndexedData);
                } else {
                    throw new IllegalStateException("Calling next() whereas hasNext() is false");
                }
            }

            @Override
            public void close() {
                originalIterator.close();
            }
        };
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException {
        return loadTextFile(path, progress, encoding, -1);
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException {
        TextFilePLL textPLL = pllContext.textFile(path, encoding, false);
        textPLL.setProgressHandler(progress);
        PLL<Row> rows = textPLL
                .map(s -> new Row(Arrays.asList(new Cell(s, null))), "wrap as row with single cell");
        if (limit >= 0) {
            // this generally leaves more rows than necessary, but is the best thing
            // we can do so far without reading the dataset to add row indices
            rows = rows.limitPartitions(limit);
        }
        PairPLL<Long, Row> pll = rows
                .zipWithIndex();
        long rowCount = pll.count(); // this is already known thanks to zipWithIndex
        if (limit >= 0 && rowCount > limit) {
            // enforce limit properly by removing any rows from the following partitions
            // that exceed the desired row count
            pll = pll.dropLastElements(rowCount - limit);
        }
        return new LocalGrid(
                this,
                pll,
                new ColumnModel(Collections.singletonList(new ColumnMetadata("Column"))),
                Collections.emptyMap(), -1);
    }

    @Override
    public <T> ChangeData<T> changeDataFromList(List<IndexedData<T>> changeData) {
        // We do this filtering on the list itself rather than on the PLL
        // so that the PLL has known partition sizes
        List<IndexedData<T>> withoutNulls = changeData
                .stream()
                .filter(id -> id.getData() != null)
                .collect(Collectors.toList());

        PairPLL<Long, IndexedData<T>> pll = pllContext
                .parallelize(defaultParallelism, withoutNulls)
                .mapToPair(indexedData -> Tuple2.of(indexedData.getId(), indexedData), "indexed data to Tuple2");
        pll = PairPLL.assumeSorted(pll);
        // no need for parent partition sizes, since pll has cached ones
        return new LocalChangeData<T>(this, pll, null, () -> true, 0);
    }

    @Override
    public <T> ChangeData<T> changeDataFromIterable(CloseableIterable<IndexedData<T>> iterable, long itemCount) {
        return changeDataFromIterable(iterable, itemCount, true);
    }

    protected <T> ChangeData<T> changeDataFromIterable(CloseableIterable<IndexedData<T>> iterable, long itemCount, boolean isComplete) {
        // the call to zipWithIndex is free because the PLL has a single partition
        PairPLL<Long, IndexedData<T>> pll = pllContext.singlePartitionPLL(iterable, itemCount)
                .mapToPair(indexedData -> Tuple2.of(indexedData.getId(), indexedData), "indexed data to Tuple2");
        pll = PairPLL.assumeSorted(pll);
        return new LocalChangeData<>(this, pll, null, () -> isComplete, 0);
    }

    @Override
    public <T> ChangeData<T> emptyChangeData() {
        CloseableIterable<IndexedData<T>> iterable = () -> CloseableIterator.from(0L).map(idx -> new IndexedData<>(idx));
        return changeDataFromIterable(iterable, -1L, false);
    }

    @Override
    public boolean supportsProgressReporting() {
        return true;
    }

    /**
     * Returns the predicted memory cost of a reconciled cell, when caching grids
     * 
     * @return
     */
    public int getReconciledCellCost() {
        return reconciledCellCost;
    }

    public int getUnreconciledCellCost() {
        return unreconciledCellCost;
    }

}
