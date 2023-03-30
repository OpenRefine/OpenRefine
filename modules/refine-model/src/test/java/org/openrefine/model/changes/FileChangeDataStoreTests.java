
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.openrefine.model.Runner;
import org.openrefine.process.Process;
import org.openrefine.process.ProgressingFuture;
import org.openrefine.util.TestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FileChangeDataStoreTests {

    Runner runner;
    MyChangeData changeData;
    MySerializer serializer;
    File rootDir;
    File changeDir;
    File incompleteDir;
    FileChangeDataStore SUT;
    VoidFuture future;

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
        changeData = mock(MyChangeData.class);
        serializer = mock(MySerializer.class);
        when(runner.loadChangeData(any(), eq(serializer))).thenReturn(changeData);
        future = mock(VoidFuture.class);
        when(changeData.saveToFileAsync(any(), eq(serializer))).thenReturn(future);
        SUT = new FileChangeDataStore(runner, changeDir, incompleteDir);
    }

    @Test
    public void testStoreRetrieveAndDelete() throws IOException, InterruptedException {
        ChangeDataId changeDataId = new ChangeDataId(123, "data");
        when(changeData.isComplete()).thenReturn(true);

        SUT.store(changeData, changeDataId, serializer, Optional.empty());

        verify(changeData, times(1)).saveToFileAsync(any(), eq(serializer));
        Assert.assertTrue(new File(new File(changeDir, "123"), "data").exists());
        Assert.assertFalse(SUT.needsRefreshing(123));
        ChangeData<String> retrieved = SUT.retrieve(new ChangeDataId(123, "data"), serializer);
        Assert.assertEquals(retrieved, changeData);

        SUT.discardAll(123);

        Assert.assertFalse(new File(changeDir, "123").exists());
    }

    @Test
    public void testRetrieveOrCompute() throws IOException {
        ChangeDataId changeDataId = new ChangeDataId(198, "data");
        when(changeData.isComplete()).thenReturn(false);
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> changeData);

        ChangeData<String> returnedChangeData = SUT.retrieveOrCompute(changeDataId, serializer, completionProcess, "description");

        Assert.assertTrue(SUT.needsRefreshing(198));
        Assert.assertEquals(returnedChangeData, changeData);
    }

    @Test
    public void testDiscardAll() {
        Process process = mock(Process.class);
        when(process.getChangeDataId()).thenReturn(new ChangeDataId(456, "data"));
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
        when(changeData.isComplete()).thenReturn(false);
        ChangeDataId changeDataId = new ChangeDataId(456L, "data");
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> changeData);

        SUT.retrieveOrCompute(changeDataId, serializer, completionProcess, "description");

        // A history entry with a corresponding running process needs refreshing
        Assert.assertTrue(SUT.needsRefreshing(456L));
    }

    @Test
    public void testNeedsRefreshingPausedProcess() throws IOException {
        when(changeData.isComplete()).thenReturn(false);
        ChangeDataId changeDataId = new ChangeDataId(789L, "data");
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = (oldChangeData -> changeData);

        SUT.retrieveOrCompute(changeDataId, serializer, completionProcess, "description");
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
