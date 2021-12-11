
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.Set;

import org.openrefine.model.GridState;

/**
 * An interface for accessing some intermediate grid states in a project, which are stored on disk.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface CachedGridStore {

    /**
     * Lists the set of history entry ids for which the resulting grid state is cached.
     */
    public Set<Long> listCachedGridIds();

    /**
     * Retrieves a disk-cached grid by providing the id of the history entry which produced it.
     * 
     * @param id
     * @return
     * @throws IOException
     */
    public GridState getCachedGrid(long id) throws IOException;

    /**
     * Discards the on-disk cache of a grid by providing the id of the history entry which produced it.
     * 
     * @param id
     * @throws IOException
     */
    public void uncacheGrid(long id) throws IOException;

    /**
     * Caches a grid state on disk, indexed by the id of the history entry which produced it.
     * 
     * @param id
     * @param grid
     * @return the same grid state read from disk instead.
     * @throws IOException
     */
    public GridState cacheGrid(long id, GridState grid) throws IOException;
}
