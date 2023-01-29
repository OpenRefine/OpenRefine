
package org.openrefine.model.changes;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.util.TestUtils;

public class FileGridCacheTests {

    File baseDir;
    File subDir;
    Runner runner;
    FileGridCache SUT; // System Under Test

    @BeforeMethod
    public void createTestDir() throws IOException {
        baseDir = TestUtils.createTempDirectory("filecachedgridstore");
        subDir = new File(baseDir, "1234");
        subDir.mkdir();
        runner = mock(Runner.class);
        SUT = new FileGridCache(runner, baseDir);
    }

    @AfterMethod
    public void deleteTestDir() throws IOException {
        FileUtils.deleteDirectory(baseDir);
    }

    @Test
    public void testListDir() {
        // create a few more subdirectories
        new File(baseDir, "5678").mkdir();
        new File(baseDir, "9012").mkdir();

        Set<Long> expected = Arrays.asList(1234L, 5678L, 9012L).stream().collect(Collectors.toSet());

        Assert.assertEquals(SUT.listCachedGridIds(), expected);
    }

    @Test
    public void testListDirDoesNotExist() {
        SUT = new FileGridCache(runner, new File(baseDir, "does-not-exist"));

        Assert.assertEquals(SUT.listCachedGridIds(), Collections.emptySet());
    }

    @Test
    public void testDelete() throws IOException {
        SUT.uncacheGrid(1234);

        Assert.assertFalse(subDir.exists());
    }

    @Test
    public void testCache() throws IOException {
        Grid grid = mock(Grid.class);

        SUT.cacheGrid(5678, grid);

        verify(grid, times(1)).saveToFile(eq(new File(baseDir, "5678")));
    }

    @Test
    public void testGetCachedGrid() throws IOException {
        Grid grid = mock(Grid.class);
        when(runner.loadGrid(eq(new File(baseDir, "1234")))).thenReturn(grid);

        Grid returned = SUT.getCachedGrid(1234L);

        Assert.assertEquals(returned, grid);
    }
}
