
package org.openrefine.runners.local.logging;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.openrefine.model.Runner;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.process.ProgressingFuture;

public class LoggedChangeData<T> implements ChangeData<T> {

    protected final ChangeData<T> changeData;
    protected final LoggedRunner runner;

    public LoggedChangeData(LoggedRunner runner, ChangeData<T> changeData) {
        this.runner = runner;
        this.changeData = changeData;
    }

    @Override
    public T get(long rowId) {
        return runner.exec("get", () -> changeData.get(rowId));
    }

    @Override
    public Runner getRunner() {
        return runner;
    }

    @Override
    public void saveToFile(File file, ChangeDataSerializer<T> serializer) throws IOException, InterruptedException {
        runner.exec("saveToFile", () -> {
            try {
                changeData.saveToFile(file, serializer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public ProgressingFuture<Void> saveToFileAsync(File file, ChangeDataSerializer<T> serializer) {
        return runner.exec("saveToFile", () -> changeData.saveToFileAsync(file, serializer));
    }

    @Override
    public boolean isComplete() {
        return changeData.isComplete();
    }

    @Override
    public Iterator<IndexedData<T>> iterator() {
        return runner.exec("iterator", () -> changeData.iterator());
    }
}
