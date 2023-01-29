
package org.openrefine.runners.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.process.ProgressReporter;

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
    public Runner getDatamodelRunner() {
        return new TestingRunner();
    }

    protected void saveToFile(File file, ChangeDataSerializer<T> serializer, Optional<ProgressReporter> progressReporter)
            throws IOException {

        file.mkdirs();
        File partFile = new File(file, "part-00000.gz");
        FileOutputStream fos = null;
        GZIPOutputStream gos = null;
        OutputStreamWriter writer = null;

        try {
            fos = new FileOutputStream(partFile);
            gos = new GZIPOutputStream(fos);
            writer = new OutputStreamWriter(gos);
            for (IndexedData<T> row : this) {
                row.write(writer, serializer);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
            if (gos != null) {
                gos.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
        if (progressReporter.isPresent()) {
            progressReporter.get().reportProgress(100);
        }
    }

    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException {
        saveToFile(file, serializer, Optional.empty());
    }

    public void saveToFile(File file, ChangeDataSerializer<T> serializer, ProgressReporter progressReporter) throws IOException {
        saveToFile(file, serializer, Optional.ofNullable(progressReporter));
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
