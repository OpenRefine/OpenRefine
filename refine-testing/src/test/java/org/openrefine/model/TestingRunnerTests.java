
package org.openrefine.model;

import java.io.Serializable;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestingRunnerTests extends RunnerTestBase {

    /**
     * Run generic datamodel tests
     */
    @Override
    public Runner getDatamodelRunner() {
        return new TestingRunner();
    }

    // Tests for the internal serialization methods

    private static class NotSerializable {

    }

    private static class WithTransientField implements Serializable {

        private static final long serialVersionUID = 6710037403976232918L;
        public transient int val;
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testEnsureSerializableFails() {
        TestingRunner.ensureSerializable(new NotSerializable());
    }

    @Test
    public void testEnsureSerializableSucceeds() {
        TestingRunner.ensureSerializable(new WithTransientField());
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testSerializeAndDeserializeFails() {
        TestingRunner.serializeAndDeserialize(new NotSerializable());
    }

    @Test
    public void testSerializeAndDeserializeSucceeds() {
        WithTransientField instance = new WithTransientField();
        instance.val = 34;
        WithTransientField copy = TestingRunner.serializeAndDeserialize(instance);
        Assert.assertEquals(copy.val, 0);
    }

}
