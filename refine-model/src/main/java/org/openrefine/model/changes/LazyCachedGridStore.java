
package org.openrefine.model.changes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.openrefine.model.GridState;

public class LazyCachedGridStore implements CachedGridStore {

    protected final Map<Long, GridState> _cache = new HashMap<>();

    @Override
    public Set<Long> listCachedGridIds() {
        return _cache.keySet();
    }

    @Override
    public GridState getCachedGrid(long id) throws IOException {
        return _cache.get(id);
    }

    @Override
    public void uncacheGrid(long id) throws IOException {
        _cache.remove(id);
    }

    @Override
    public GridState cacheGrid(long id, GridState grid) throws IOException {
        _cache.put(id, grid);
        return grid;
    }

}
