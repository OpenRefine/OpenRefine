
package org.openrefine.model.local;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.util.concurrent.ListeningExecutorService;

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

    public PLLContext(
            ListeningExecutorService executorService,
            int defaultParallelism,
            long minSplitSize,
            long maxSplitSize) {
        this.executorService = executorService;
        this.defaultParallelism = defaultParallelism;
        this.minSplitSize = minSplitSize;
        this.maxSplitSize = maxSplitSize;
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
     * @param path
     * @param encoding
     * @return
     * @throws IOException
     */
    public TextFilePLL textFile(String path, Charset encoding) throws IOException {
        return new TextFilePLL(this, path, encoding);
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
     * @param <T>
     * @param numPartitions
     *            the desired number of partitions
     * @param rows
     * @return
     */
    public <T> PLL<T> parallelize(int numPartitions, List<T> rows) {
        return new InMemoryPLL<T>(this, rows, numPartitions);
    }

    /**
     * Returns the default number of partitions that text files should be split into
     */
    protected int getDefaultParallelism() {
        return defaultParallelism;
    }

    /**
     * Returns the minimum size of a partition in bytes
     */
    protected long getMinSplitSize() {
        return minSplitSize;
    }

    /**
     * Returns the maximum size of a partition in bytes
     * 
     * @return
     */
    protected long getMaxSplitSize() {
        return maxSplitSize;
    }
}
