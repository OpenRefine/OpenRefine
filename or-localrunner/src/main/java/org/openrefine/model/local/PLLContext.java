
package org.openrefine.model.local;

import java.io.IOException;
import java.util.List;

import com.google.common.util.concurrent.ListeningExecutorService;
import org.apache.hadoop.fs.FileSystem;

/**
 * An object holding the necessary context instances to manipulate partitioned lazy lists (PLL).
 * 
 * @author Antonin Delpeuch
 *
 */
public class PLLContext {

    private final ListeningExecutorService executorService;
    private final FileSystem fileSystem;
    private final int defaultParallelism;
    private final long minSplitSize;
    private final long maxSplitSize;

    public PLLContext(
            ListeningExecutorService executorService,
            FileSystem fileSystem,
            int defaultParallelism) {
        this.executorService = executorService;
        this.fileSystem = fileSystem;
        this.defaultParallelism = defaultParallelism;
        this.minSplitSize = fileSystem.getConf().getLong("mapreduce.input.fileinputformat.split.minsize", 1048576);
        this.maxSplitSize = fileSystem.getConf().getLong("mapreduce.input.fileinputformat.split.maxsize", Long.MAX_VALUE);
    }

    /**
     * Returns the thread pool used in this context
     */
    public ListeningExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Returns the Hadoop filesystem used in this context
     */
    public FileSystem getFileSystem() {
        return fileSystem;
    }

    /**
     * Loads a text file as a PLL.
     * 
     * @param path
     * @return
     * @throws IOException
     */
    public TextFilePLL textFile(String path) throws IOException {
        return new TextFilePLL(this, path);
    }

    /**
     * @throws IOException
     * 
     */
    public void shutdown() throws IOException {
        executorService.shutdown();
        fileSystem.close();
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
