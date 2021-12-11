
package org.openrefine.model.changes;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Optional;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LazyChangeDataStoreTests {

    LazyChangeDataStore SUT;
    MySerializer serializer = mock(MySerializer.class);

    @BeforeMethod
    public void setUpSUT() {
        SUT = new LazyChangeDataStore();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRetrieveNonExisting() throws IOException {
        SUT.retrieve(123, "data", serializer);
    }

    @Test
    public void testStoreAndRetrieve() throws IOException {
        MyChangeData changeData = mock(MyChangeData.class);
        SUT.store(changeData, 123, "data", serializer, Optional.empty());
        Assert.assertEquals(SUT.retrieve(123, "data", serializer), changeData);
    }

    @Test
    public void testDrop() throws IOException {
        MyChangeData changeData = mock(MyChangeData.class);
        SUT.store(changeData, 123, "data", serializer, Optional.empty());
        SUT.discardAll(123);
        try {
            SUT.retrieve(123, "data", serializer);
            Assert.fail("Expected not to find the change data after deletion");
        } catch (IllegalArgumentException e) {
            ;
        }
    }

    // to ease mocking

    private abstract static class MyChangeData implements ChangeData<String> {
    }

    private abstract static class MySerializer implements ChangeDataSerializer<String> {

        private static final long serialVersionUID = 8276627729632340969L;
    }
}
