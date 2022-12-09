
package org.openrefine.model.local.util.logging;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.importers.MultiFileReadingProgress;
import org.openrefine.model.*;
import org.openrefine.model.changes.ChangeData;
import org.openrefine.model.changes.ChangeDataSerializer;
import org.openrefine.model.changes.IndexedData;
import org.openrefine.overlay.OverlayModel;

public class LoggedDatamodelRunner implements DatamodelRunner {

    Logger logger = LoggerFactory.getLogger("datamodel runner");

    protected final DatamodelRunner runner;

    public LoggedDatamodelRunner(RunnerConfiguration configuration) {
        String className = configuration.getParameter("wrappedClass", null);
        Class<?> runnerClass = null;
        try {
            runnerClass = this.getClass().getClassLoader().loadClass(className);
            this.runner = (DatamodelRunner) runnerClass.getConstructor(RunnerConfiguration.class).newInstance(configuration);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public LoggedDatamodelRunner(DatamodelRunner runner) {
        this.runner = runner;
    }

    protected GridState wrap(GridState grid) {
        return new LoggedGridState(this, grid);
    }

    protected <T> ChangeData<T> wrap(ChangeData<T> changeData) {
        return new LoggedChangeData<T>(this, changeData);
    }

    protected <T> T exec(String name, Supplier<T> action) {
        long start = Instant.now().toEpochMilli();
        T result = action.get();
        long duration = Instant.now().toEpochMilli() - start;
        logger.info(String.format("%s [%d ms]", name, duration));
        return result;
    }

    protected void exec(String name, Runnable action) {
        long start = Instant.now().toEpochMilli();
        action.run();
        long duration = Instant.now().toEpochMilli() - start;
        logger.info(String.format("%s [%d ms]", name, duration));
    }

    @Override
    public GridState loadGridState(File path) throws IOException {
        try {
            return wrap(exec("loadGridState", () -> {
                try {
                    return runner.loadGridState(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> ChangeData<T> loadChangeData(File path, ChangeDataSerializer<T> serializer) throws IOException {
        return wrap(exec("loadChangeData", () -> {
            try {
                return runner.loadChangeData(path, serializer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public GridState create(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        return wrap(exec("create", () -> runner.create(columnModel, rows, overlayModels)));
    }

    @Override
    public GridState loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException {
        try {
            return wrap(exec("loadTextFile", () -> {
                try {
                    return runner.loadTextFile(path, progress, encoding);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public GridState loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException {
        return wrap(exec("loadTextFile", () -> {
            try {
                return runner.loadTextFile(path, progress, encoding, limit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public <T> ChangeData<T> create(List<IndexedData<T>> changeData) {
        return wrap(exec("create", () -> runner.create(changeData)));
    }

    @Override
    public boolean supportsProgressReporting() {
        return runner.supportsProgressReporting();
    }
}
