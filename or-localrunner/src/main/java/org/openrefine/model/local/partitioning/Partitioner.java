
package org.openrefine.model.local.partitioning;

/**
 * Locates an element in a partition based on its key.
 * 
 * @author Antonin Delpeuch
 *
 * @param <T>
 */
public interface Partitioner<T> {

    /**
     * Retrieves the partition id given a key.
     * 
     * @param key
     * @return
     */
    public int getPartition(T key);

    /**
     * Returns the number of partitions assumed by this partitioner.
     */
    public int numPartitions();
}
