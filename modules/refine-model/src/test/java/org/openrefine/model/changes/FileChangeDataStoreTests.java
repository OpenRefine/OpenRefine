
package org.openrefine.model.changes;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.model.Runner;
import org.openrefine.util.TestUtils;

public class FileChangeDataStoreTests {

    Runner runner;
    MyChangeData changeData;
    MySerializer serializer;
    File dir;
    FileChangeDataStore SUT;

    @BeforeClass
    public void setUpDir() throws IOException {
        dir = TestUtils.createTempDirectory("changedatastore");
    }

    @AfterClass
    public void removeDir() throws IOException {
        FileUtils.deleteDirectory(dir);
    }

    @BeforeMethod
    public void setUp() throws IOException {
        runner = mock(Runner.class);
        changeData = mock(MyChangeData.class);
        serializer = mock(MySerializer.class);
        when(runner.loadChangeData(any(), eq(serializer))).thenReturn(changeData);
        // when(changeData.saveToFile(any(), eq(serializer), any())).
        SUT = new FileChangeDataStore(runner, dir);
    }

    @Test
    public void testStoreRetrieveAndDelete() throws IOException, InterruptedException {
        SUT.store(changeData, 123, "data", serializer, Optional.empty());
        verify(changeData, times(1)).saveToFile(any(), eq(serializer));
        Assert.assertTrue(new File(new File(dir, "123"), "data").exists());
        ChangeData<String> retrieved = SUT.retrieve(123, "data", serializer);
        Assert.assertEquals(retrieved, changeData);
        SUT.discardAll(123);
        Assert.assertFalse(new File(dir, "123").exists());
    }

    // to ease mocking

    private abstract static class MyChangeData implements ChangeData<String> {
    }

    private abstract static class MySerializer implements ChangeDataSerializer<String> {

        private static final long serialVersionUID = 8276627729632340969L;
    }
}
