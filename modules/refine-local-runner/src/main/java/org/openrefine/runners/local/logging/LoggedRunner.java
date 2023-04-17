
package org.openrefine.runners.local.logging;

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
import org.openrefine.util.CloseableIterable;

public class LoggedRunner implements Runner {

    Logger logger = LoggerFactory.getLogger("runner");

    protected final Runner runner;

    public LoggedRunner(RunnerConfiguration configuration) {
        String className = configuration.getParameter("wrappedClass", null);
        Class<?> runnerClass = null;
        try {
            runnerClass = this.getClass().getClassLoader().loadClass(className);
            this.runner = (Runner) runnerClass.getConstructor(RunnerConfiguration.class).newInstance(configuration);
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

    public LoggedRunner(Runner runner) {
        this.runner = runner;
    }

    protected Grid wrap(Grid grid) {
        return new LoggedGrid(this, grid);
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
    public Grid loadGrid(File path) throws IOException {
        try {
            return wrap(exec("loadGrid", () -> {
                try {
                    return runner.loadGrid(path);
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
        try {
            return wrap(exec("loadChangeData", () -> {
                try {
                    return runner.loadChangeData(path, serializer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public Grid gridFromList(ColumnModel columnModel, List<Row> rows, Map<String, OverlayModel> overlayModels) {
        return wrap(exec("gridFromList", () -> runner.gridFromList(columnModel, rows, overlayModels)));
    }

    @Override
    public Grid gridFromIterable(ColumnModel columnModel, CloseableIterable<Row> rows, Map<String, OverlayModel> overlayModels,
            long rowCount, long recordCount) {
        return wrap(exec("gridFromIterable", () -> runner.gridFromIterable(columnModel, rows, overlayModels, rowCount, recordCount)));
    }

    @Override
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding) throws IOException {
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
    public Grid loadTextFile(String path, MultiFileReadingProgress progress, Charset encoding, long limit) throws IOException {
        return wrap(exec("loadTextFile", () -> {
            try {
                return runner.loadTextFile(path, progress, encoding, limit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public <T> ChangeData<T> changeDataFromList(List<IndexedData<T>> changeData) {
        return wrap(exec("changeDataFromList", () -> runner.changeDataFromList(changeData)));
    }

    @Override
    public <T> ChangeData<T> changeDataFromIterable(CloseableIterable<IndexedData<T>> iterable, long itemCount) {
        return wrap(exec("changeDataFromIterable", () -> runner.changeDataFromIterable(iterable, itemCount)));
    }

    @Override
    public boolean supportsProgressReporting() {
        return runner.supportsProgressReporting();
    }
}
