
package org.openrefine.runners.testing;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.process.CompletedFuture;
import org.openrefine.process.FailingFuture;
import org.openrefine.process.ProgressReporter;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.CloseableIterator;

public class TestingChangeData<T> implements ChangeData<T> {

    protected Map<Long, IndexedData<T>> data;
    protected boolean isComplete;

    public TestingChangeData(Map<Long, IndexedData<T>> data, boolean isComplete) {
        this.data = data;
        this.isComplete = isComplete;
    }

    @Override
    public IndexedData<T> get(long rowId) {
        if (data.containsKey(rowId)) {
            return data.get(rowId);
        } else {
            if (isComplete()) {
                return new IndexedData<>(rowId, null);
            } else {
                return new IndexedData<>(rowId);
            }
        }
    }

    @Override
    @JsonIgnore
    public Runner getRunner() {
        return new TestingRunner();
    }

    protected void saveToFile(File file, ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter)
            throws IOException {

        file.mkdirs();
        File partFile = new File(file, "part-00000.gz");
        try (FileOutputStream fos = new FileOutputStream(partFile);
                GZIPOutputStream gos = new GZIPOutputStream(fos);
                OutputStreamWriter writer = new OutputStreamWriter(gos);
                CloseableIterator<IndexedData<T>> iterator = iterator()) {
            for (IndexedData<T> row : iterator) {
                row.write(writer, serializer);
            }
        }

        File completionMarker = new File(file, Runner.COMPLETION_MARKER_FILE_NAME);
        try (FileOutputStream fosCompletion = new FileOutputStream(completionMarker)) {
            Writer writerCompletion = new OutputStreamWriter(fosCompletion);
            writerCompletion.close();
        }

        if (progressReporter.isPresent()) {
            progressReporter.get().reportProgress(100);
        }
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException {
        saveToFile(file, serializer, Optional.empty());
    }

    @Override
    public ProgressingFuture<Void> saveToFileAsync(File file, ChangeDataSerializer<T> serializer) {
        try {
            saveToFile(file, serializer);
        } catch (IOException e) {
            return new FailingFuture<>(e);
        }
        return new CompletedFuture<>(null);
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public CloseableIterator<IndexedData<T>> iterator() {
        List<IndexedData<T>> indexed = new ArrayList<>(data
                .values());
        indexed.sort(new Comparator<IndexedData<T>>() {

            @Override
            public int compare(IndexedData<T> arg0, IndexedData<T> arg1) {
                return (int) (arg0.getId() - arg1.getId());
            }

        });
        CloseableIterator<IndexedData<T>> originalIterator = CloseableIterator.wrapping(indexed.iterator());
        if (isComplete) {
            return originalIterator;
        } else {
            return IndexedData.completeIterator(originalIterator);
        }
    }

}
