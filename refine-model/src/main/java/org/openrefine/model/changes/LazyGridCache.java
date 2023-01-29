
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrefine.model.Grid;

public class LazyGridCache implements GridCache {

    protected final Map<Long, Grid> _cache = new HashMap<>();

    @Override
    public Set<Long> listCachedGridIds() {
        return _cache.keySet();
    }

    @Override
    public Grid getCachedGrid(long id) throws IOException {
        return _cache.get(id);
    }

    @Override
    public void uncacheGrid(long id) throws IOException {
        _cache.remove(id);
    }

    @Override
    public Grid cacheGrid(long id, Grid grid) throws IOException {
        _cache.put(id, grid);
        return grid;
    }

}
