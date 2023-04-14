
package org.openrefine.runners.local.pll;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.util.concurrent.ListeningExecutorService;

import org.openrefine.util.CloseableIterable;

/**
 * An object holding the necessary context instances to manipulate partitioned lazy lists (PLL).
 * 
 * @author Antonin Delpeuch
 *
 */
public class PLLContext {

    private final ListeningExecutorService executorService;
    private final int defaultParallelism;
    private final long minSplitSize;
    private final long maxSplitSize;
    private final long minSplitRowCount;
    private final long maxSplitRowCount;

    private long nextPLLId;

    /**
     * Constructor.
     *
     * @param executorService
     *            the executor service to use to run the threads necessary for concurrent operations on PLLs
     * @param defaultParallelism
     *            the default number of partitions a PLL should be split in, or in other words the default number of
     *            processor cores to use for parallel operations.
     * @param minSplitSize
     *            the minimum size (in bytes) of a partition. The runner will attempt, when possible, not to create
     *            partitions smaller than that.
     * @param maxSplitSize
     *            the maximum size (in bytes) of a partition. The runner will attempt, when possible, not to create
     *            partitions bigger than that.
     * @param minSplitRowCount
     *            the minimum size (in number of rows) of a partition. Used when repartitioning an existing PLL.
     * @param maxSplitRowCount
     *            the maximum size (in number of rows) of a partition. Used when repartitioning an existing PLL.
     */
    public PLLContext(
            ListeningExecutorService executorService,
            int defaultParallelism,
            long minSplitSize,
            long maxSplitSize,
            long minSplitRowCount,
            long maxSplitRowCount) {
        this.executorService = executorService;
        this.defaultParallelism = defaultParallelism;
        this.minSplitSize = minSplitSize;
        this.maxSplitSize = maxSplitSize;
        this.minSplitRowCount = minSplitRowCount;
        this.maxSplitRowCount = maxSplitRowCount;
        this.nextPLLId = 0;
    }

    /**
     * Returns the thread pool used in this context
     */
    public ListeningExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Loads a text file as a PLL.
     *
     */
    public TextFilePLL textFile(String path, Charset encoding, boolean ignoreEarlyEOF) throws IOException {
        return new TextFilePLL(this, path, encoding, ignoreEarlyEOF);
    }

    /**
     * @throws IOException
     * 
     */
    public void shutdown() throws IOException {
        executorService.shutdown();
    }

    /**
     * Turns a regular list into a Partitioned Lazy List.
     *
     * @param numPartitions
     *            the desired number of partitions
     */
    public <T> PLL<T> parallelize(int numPartitions, List<T> rows) {
        return new InMemoryPLL<>(this, rows, numPartitions);
    }

    /**
     * Turns a closeable iterable into a Partitioned Lazy List, which has a single partition.
     *
     * @param iterable
     *            the collection of elements of the PLL.
     * @param itemCount
     *            if known, the number of elements in the collection. If not known, -1. Supplying this may avoid
     *            iterations over the collection in some cases.
     */
    public <T> PLL<T> singlePartitionPLL(CloseableIterable<T> iterable, long itemCount) {
        return new SinglePartitionPLL<>(this, iterable, itemCount);
    }

    /**
     * Returns the default number of partitions that text files should be split into
     */
    protected int getDefaultParallelism() {
        return defaultParallelism;
    }

    /**
     * The minimum size of a partition in bytes
     */
    protected long getMinSplitSize() {
        return minSplitSize;
    }

    /**
     * The maximum size of a partition in bytes
     */
    protected long getMaxSplitSize() {
        return maxSplitSize;
    }

    /**
     * The minimum size of a partition in number of elements
     */
    protected long getMinSplitRowCount() {
        return minSplitRowCount;
    }

    /**
     * The maximum size of a partition in number of elements
     */
    protected long getMaxSplitRowCount() {
        return maxSplitRowCount;
    }

    public long allocateId() {
        return ++nextPLLId;
    }

}
