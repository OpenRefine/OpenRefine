package com.google.refine.tests.process;

import org.testng.annotations.Test;

import com.google.refine.process.LongRunningProcess;
import com.google.refine.tests.util.TestUtils;

public class LongRunningProcessTests {
    
    public static class LongRunningProcessStub extends LongRunningProcess {

        protected LongRunningProcessStub(String description) {
            super(description);
        }

        @Override
        protected Runnable getRunnable() {
            return null;
        }
        
    }
    @Test
    public void serializeLongRunningProcess() {
        LongRunningProcess process = new LongRunningProcessStub("some description");
        int hashCode = process.hashCode();
        TestUtils.isSerializedTo(process, "{"
                + "\"id\":"+hashCode+","
                + "\"description\":\"some description\","
                + "\"immediate\":false,"
                + "\"status\":\"pending\","
                + "\"progress\":0}");
    }
}
