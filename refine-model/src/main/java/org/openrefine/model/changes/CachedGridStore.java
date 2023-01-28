
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Set;

import org.openrefine.model.GridState;

/**
 * An interface for accessing some intermediate grid states in a project, which are stored on disk.
 */
public interface CachedGridStore {

    /**
     * Lists the set of history entry ids for which the resulting grid state is cached.
     */
    public Set<Long> listCachedGridIds();

    /**
     * Retrieves a disk-cached grid.
     * 
     * @param id
     *            the id of the history entry which produced it
     * @return the cached grid
     * @throws IOException
     *             if the grid could not be loaded from disk
     */
    public GridState getCachedGrid(long id) throws IOException;

    /**
     * Discards the on-disk cache of a grid
     * 
     * @param id
     *            the id of the history entry which produced it.
     * @throws IOException
     *             if removing the grid from the cache did not succeed
     */
    public void uncacheGrid(long id) throws IOException;

    /**
     * Caches a grid state on disk.
     * 
     * @param id
     *            the id of the history entry which produced it
     * @param grid
     *            the grid to cache
     * @return the same grid state read from disk instead.
     * @throws IOException
     *             if caching the grid failed
     */
    public GridState cacheGrid(long id, GridState grid) throws IOException;
}
