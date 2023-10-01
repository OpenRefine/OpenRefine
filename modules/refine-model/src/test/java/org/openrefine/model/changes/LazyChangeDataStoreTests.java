
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.openrefine.history.History;
import org.openrefine.model.Runner;

public class LazyChangeDataStoreTests {

    LazyChangeDataStore SUT;
    MySerializer serializer = mock(MySerializer.class);
    Runner runner = mock(Runner.class);

    @BeforeMethod
    public void setUpSUT() {
        SUT = new LazyChangeDataStore(runner);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRetrieveNonExisting() throws IOException {
        SUT.retrieve(new ChangeDataId(123, "data"), serializer);
    }

    @Test
    public void testStoreAndRetrieve() throws IOException {
        MyChangeData changeData = mock(MyChangeData.class);
        SUT.store(changeData, new ChangeDataId(123, "data"), serializer, Optional.empty());
        Assert.assertEquals(SUT.retrieve(new ChangeDataId(123, "data"), serializer), changeData);
        Assert.assertEquals(SUT.getChangeDataIds(123L), Collections.singletonList(new ChangeDataId(123, "data")));
    }

    @Test
    public void testDrop() throws IOException {
        MyChangeData changeData = mock(MyChangeData.class);
        SUT.store(changeData, new ChangeDataId(123, "data"), serializer, Optional.empty());
        SUT.discardAll(123);
        try {
            SUT.retrieve(new ChangeDataId(123, "data"), serializer);
            Assert.fail("Expected not to find the change data after deletion");
        } catch (IllegalArgumentException e) {
            ;
        }
        Assert.assertEquals(SUT.getChangeDataIds(123L), Collections.emptyList());
    }

    @Test
    public void testRetrieveOrCompute() throws IOException {
        MyChangeData changeData = mock(MyChangeData.class);
        when(changeData.isComplete()).thenReturn(false);
        Function<Optional<ChangeData<String>>, ChangeData<String>> completionProcess = existingChangeData -> changeData;

        ChangeDataId changeDataId = new ChangeDataId(456, "data");
        ChangeData<String> returnedChangeData = SUT.retrieveOrCompute(
                changeDataId, serializer, completionProcess,
                "description", mock(History.class), 2);

        Assert.assertEquals(returnedChangeData, changeData);
        Assert.assertFalse(SUT.needsRefreshing(456));
        Assert.assertEquals(SUT.getChangeDataIds(456L), Collections.singletonList(new ChangeDataId(456L, "data")));
    }

    // to ease mocking

    private abstract static class MyChangeData implements ChangeData<String> {
    }

    private abstract static class MySerializer implements ChangeDataSerializer<String> {

        private static final long serialVersionUID = 8276627729632340969L;
    }
}
