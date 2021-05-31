
package org.openrefine.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.model.local.PairPLL;
import org.openrefine.process.ProgressReporter;

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

    protected void saveToFile(File file, ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter)
            throws IOException {
        grid.map(r -> {
            try {
                return (new IndexedData<T>(r.getKey(), r.getValue()).writeAsString(serializer));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        })
                .saveAsTextFile(file.getAbsolutePath(), progressReporter);
    }

    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException {
        saveToFile(file, serializer, Optional.empty());
    }

    public void saveToFile(File file, ChangeDataSerializer<T> serializer, ProgressReporter progressReporter) throws IOException {
        saveToFile(file, serializer, Optional.ofNullable(progressReporter));
    }

    public PairPLL<Long, T> getPLL() {
        return grid;
    }

}
