
package org.openrefine.model.changes;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import org.openrefine.model.Grid;
import org.openrefine.model.Runner;

public class FileGridCache implements GridCache {

    private final Runner runner;
    private final File baseDir;

    public FileGridCache(Runner runner, File baseDir) {
        this.runner = runner;
        this.baseDir = baseDir;
        if (!baseDir.exists()) {
            baseDir.mkdir();
        }
    }

    protected File getGridPath(long id) {
        return new File(baseDir, Long.toString(id));
    }

    @Override
    public Set<Long> listCachedGridIds() {
        IOFileFilter fileFilter = new IOFileFilter() {

            @Override
            public boolean accept(File file) {
                return false;
            }

            @Override
            public boolean accept(File dir, String name) {
                return false;
            }

        };
        IOFileFilter dirFilter = fileFilter.negate();
        Collection<File> subDirs = FileUtils.listFilesAndDirs(baseDir, fileFilter, dirFilter);
        Set<Long> results = new HashSet<>();
        for (File subDir : subDirs) {
            try {
                results.add(Long.valueOf(subDir.getName()));
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return results;
    }

    @Override
    public Grid getCachedGrid(long id) throws IOException {
        return runner.loadGrid(getGridPath(id));
    }

    @Override
    public void uncacheGrid(long id) throws IOException {
        File directory = getGridPath(id);
        if (directory.exists()) {
            FileUtils.deleteDirectory(directory);
        }
    }

    @Override
    public Grid cacheGrid(long id, Grid grid) throws IOException {
        File directory = getGridPath(id);
        grid.saveToFile(directory);
        return runner.loadGrid(directory);
    }

}
