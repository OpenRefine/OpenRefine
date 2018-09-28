package com.google.refine.tests.process;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.JSONUtilities;

public class ProcessManagerTests {
    
    ProcessManager processManager;
    Process process1, process2;
    
    @BeforeMethod
    public void setUp() {
        processManager = new ProcessManager();
        process1 = new LongRunningProcessTests.LongRunningProcessStub("some description");
        process2 = new LongRunningProcessTests.LongRunningProcessStub("some other description");
    }
    
    @Test
    public void serializeProcessManager() throws Exception {
        processManager.queueProcess(process1);
        processManager.queueProcess(process2);
        processManager.onFailedProcess(process1, new IllegalArgumentException("unexpected error"));
        String processJson = JSONUtilities.serialize(process2);
        TestUtils.isSerializedTo(processManager, "{"
                + "\"processes\":["+processJson+"],\n"
                + "\"exceptions\":[{\"message\":\"unexpected error\"}]"
                + "}");
    }
}
