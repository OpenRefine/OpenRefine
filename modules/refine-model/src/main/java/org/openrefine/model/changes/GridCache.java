
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Set;

import org.openrefine.model.Grid;

/**
 * An interface for accessing some intermediate grids in a project, which are stored on disk.
 */
public interface GridCache {

    /**
     * Lists the set of history entry ids for which the resulting grid is cached.
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
    public Grid getCachedGrid(long id) throws IOException;

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
     * Caches a grid on disk.
     * 
     * @param id
     *            the id of the history entry which produced it
     * @param grid
     *            the grid to cache
     * @return the same grid read from disk instead.
     * @throws IOException
     *             if caching the grid failed
     */
    public Grid cacheGrid(long id, Grid grid) throws IOException;
}
