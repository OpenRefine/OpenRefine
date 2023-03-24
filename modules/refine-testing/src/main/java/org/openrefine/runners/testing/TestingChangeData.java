
package org.openrefine.runners.testing;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
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

public class TestingChangeData<T> implements ChangeData<T> {

    private Map<Long, T> data;

    public TestingChangeData(Map<Long, T> data) {
        this.data = data;
    }

    @Override
    public T get(long rowId) {
        return data.get(rowId);
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
                OutputStreamWriter writer = new OutputStreamWriter(gos);) {
            for (IndexedData<T> row : this) {
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
        return true;
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        List<IndexedData<T>> indexed = data
                .entrySet()
                .stream()
                .map(e -> new IndexedData<T>(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        indexed.sort(new Comparator<IndexedData<T>>() {

            @Override
            public int compare(IndexedData<T> arg0, IndexedData<T> arg1) {
                return (int) (arg0.getId() - arg1.getId());
            }

        });
        return indexed.iterator();
    }

}
