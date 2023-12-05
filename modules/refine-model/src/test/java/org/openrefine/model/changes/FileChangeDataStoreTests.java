
package org.openrefine.model.changes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.history.History;
import org.openrefine.model.Grid;
import org.openrefine.model.Runner;
import org.openrefine.process.Process;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.TestUtils;

public class FileChangeDataStoreTests {

    Runner runner;
    Grid baseGrid;
    MyChangeData changeData;
    MyChangeData emptyChangeData;
    MySerializer serializer;
    File rootDir;
    File changeDir;
    File incompleteDir;
    FileChangeDataStore SUT;
    VoidFuture future;
    History history;

    @BeforeClass
    public void setUpDir() throws IOException {
        rootDir = TestUtils.createTempDirectory("changedatastore");
        changeDir = new File(rootDir, "changes");
        incompleteDir = new File(rootDir, "incomplete_changes");
    }

    @AfterClass
    public void removeDir() throws IOException {
        FileUtils.deleteDirectory(changeDir);
    }

    @BeforeMethod
    public void setUp() throws IOException {
        runner = mock(Runner.class);
        baseGrid = mock(Grid.class);
        changeData = mock(MyChangeData.class);
        emptyChangeData = mock(MyChangeData.class);
        serializer = mock(MySerializer.class);
        history = mock(History.class);

        when(baseGrid.<String> emptyChangeData()).thenReturn(emptyChangeData);
        future = mock(VoidFuture.class);
        when(changeData.saveToFileAsync(any(), eq(serializer))).thenReturn(future);
        SUT = new FileChangeDataStore(runner, changeDir, incompleteDir);
    }

    @Test
    public void testStoreRetrieveAndDelete() throws IOException, InterruptedException {
        ChangeDataId changeDataId = new ChangeDataId(123, "data");
        when(runner.loadChangeData(eq(new File(changeDir, "123" + File.separator + "data")), eq(serializer), eq(false)))
                .thenReturn(changeData);
        when(changeData.isComplete()).thenReturn(true);

        SUT.store(changeData, changeDataId, serializer, Optional.empty());

        verify(changeData, times(1)).saveToFileAsync(any(), eq(serializer));
        Assert.assertTrue(new File(new File(changeDir, "123"), "data").exists());
        Assert.assertFalse(SUT.needsRefreshing(123));
        Assert.assertEquals(SUT.getChangeDataIds(123L), Collections.singletonList(changeDataId));
        ChangeData<String> retrieved = SUT.retrieve(new ChangeDataId(123, "data"), serializer);
        Assert.assertEquals(retrieved, changeData);

        SUT.discardAll(123);

        Assert.assertFalse(new File(changeDir, "123").exists());
        Assert.assertEquals(SUT.getChangeDataIds(123L), Collections.emptyList());
    }

    @Test
    public void testRetrieveOrCompute() throws IOException {
        ChangeDataId changeDataId = new ChangeDataId(198, "data");

        // set up original change data to be recovered
        File originalChangeDataLocation = new File(changeDir, "198" + File.separator + "data");
        originalChangeDataLocation.mkdirs();
        when(runner.loadChangeData(eq(originalChangeDataLocation), eq(serializer), eq(false)))
                .thenReturn(changeData);
        when(changeData.isComplete()).thenReturn(false);

        // set up new change data, moved to its temporary location
        File newChangeDataLocation = new File(incompleteDir, "198" + File.separator + "data");
        ChangeData<String> movedChangeData = mock(MyChangeData.class);
        when(runner.loadChangeData(eq(newChangeDataLocation), eq(serializer), eq(true)))
                .thenReturn(movedChangeData);
        when(movedChangeData.isComplete()).thenReturn(false);
        when(movedChangeData.saveToFileAsync(any(), eq(serializer))).thenReturn(future);

        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> oldChangeData.get());

        ChangeData<String> returnedChangeData = SUT.retrieveOrCompute(changeDataId, serializer, baseGrid, completionProcess, "description",
                history, 2);

        Assert.assertTrue(SUT.needsRefreshing(198));
        Assert.assertTrue(newChangeDataLocation.exists());
        Assert.assertEquals(returnedChangeData, changeData);
        verify(runner, times(2)).loadChangeData(eq(originalChangeDataLocation), eq(serializer), eq(false));
        Assert.assertEquals(SUT.getChangeDataIds(198L), Collections.singletonList(changeDataId));
    }

    @Test
    public void testDiscardAll() {
        Process process = mock(Process.class);
        when(process.getChangeDataId()).thenReturn(new ChangeDataId(456, "data"));
        when(process.getState()).thenReturn(Process.State.RUNNING);
        SUT.getProcessManager().queueProcess(process);

        SUT.discardAll(456);

        verify(process, times(1)).cancel();
    }

    @Test
    public void testNeedsRefreshingNoProcess() {
        // A history entry id for which we have no change data does not need refreshing
        Assert.assertFalse(SUT.needsRefreshing(12345L));
    }

    @Test
    public void testNeedsRefreshingRunningProcess() throws IOException {
        when(runner.loadChangeData(eq(new File(changeDir, "456" + File.separator + "data")), eq(serializer), eq(false))).thenReturn(changeData);
        when(changeData.isComplete()).thenReturn(false);

        ChangeDataId changeDataId = new ChangeDataId(456L, "data");
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> changeData);

        SUT.retrieveOrCompute(changeDataId, serializer, baseGrid, completionProcess, "description", history, 2);

        // A history entry with a corresponding running process needs refreshing
        Assert.assertTrue(SUT.needsRefreshing(456L));
    }

    /*
     * TODO We could try to avoid refreshes when the process is paused.
     */
    @Test(enabled = false)
    public void testNeedsRefreshingPausedProcess() throws IOException {
        when(runner.loadChangeData(eq(new File(changeDir, "789" + File.separator + "data")), eq(serializer), eq(false))).thenReturn(changeData);
        when(changeData.isComplete()).thenReturn(false);
        ChangeDataId changeDataId = new ChangeDataId(789L, "data");
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> changeData);

        SUT.retrieveOrCompute(changeDataId, serializer, baseGrid, completionProcess, "description", history, 2);
        // artificially pause the process
        when(future.isPaused()).thenReturn(true);

        // because the process is paused, we do not need to refresh
        Assert.assertFalse(SUT.needsRefreshing(789L));
    }

    // to ease mocking

    private abstract static class MyChangeData implements ChangeData<String> {
    }

    private abstract static class MySerializer implements ChangeDataSerializer<String> {

        private static final long serialVersionUID = 8276627729632340969L;
    }

    private abstract static class VoidFuture implements ProgressingFuture<Void> {

    }
}
