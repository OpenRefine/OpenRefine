
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;

import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.local.PairPLL;

public class LocalChangeData<T extends Serializable> implements ChangeData<T> {

    private final LocalDatamodelRunner runner;
    private final PairPLL<Long, T> grid;

    /**
     * Constructs a change data.
     * 
     * @param runner
     * @param grid
     *            expected not to contain any null value (they should be filtered out first)
     */
    public LocalChangeData(LocalDatamodelRunner runner, PairPLL<Long, T> grid) {
        this.runner = runner;
        this.grid = grid;
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        return grid
                .filter(tuple -> tuple.getValue() != null)
                .map(tuple -> new IndexedData<T>(tuple.getKey(), tuple.getValue()))
                .stream()
                .iterator();
    }

    @Override
    public T get(long rowId) {
        List<T> rows = grid.get(rowId);
        if (rows.size() == 0) {
            return null;
        } else if (rows.size() > 1) {
            throw new IllegalStateException(String.format("Found %d change data elements at index %d", rows.size(), rowId));
        } else {
            return rows.get(0);
        }
    }

    @Override
    public DatamodelRunner getDatamodelRunner() {
        return runner;
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException {
        grid.map(r -> {
            try {
                return (new IndexedData<T>(r.getKey(), r.getValue()).writeAsString(serializer));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })
                .saveAsTextFile(file.getAbsolutePath());
    }

    public PairPLL<Long, T> getPLL() {
        return grid;
    }

}
