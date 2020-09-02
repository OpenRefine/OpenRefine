package org.openrefine.model;
import java.io.Serializable;

import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.DatamodelRunnerTestBase;
import org.openrefine.model.TestingDatamodelRunner;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestingDatamodelRunnerTests extends DatamodelRunnerTestBase {
    
    /**
     * Run generic datamodel tests
     */
    @Override
    public DatamodelRunner getDatamodelRunner() {
        return new TestingDatamodelRunner();
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
        TestingDatamodelRunner.ensureSerializable(new NotSerializable());
    }
    
    @Test
    public void testEnsureSerializableSucceeds() {
        TestingDatamodelRunner.ensureSerializable(new WithTransientField());
    }
    
    @Test(expectedExceptions = AssertionError.class)
    public void testSerializeAndDeserializeFails() {
        TestingDatamodelRunner.serializeAndDeserialize(new NotSerializable());
    }
    
    @Test
    public void testSerializeAndDeserializeSucceeds() {
        WithTransientField instance = new WithTransientField();
        instance.val = 34;
        WithTransientField copy = TestingDatamodelRunner.serializeAndDeserialize(instance);
        Assert.assertEquals(copy.val, 0);
    }

}
