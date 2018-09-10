package com.google.refine.tests.process;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.tests.util.TestUtils;
import com.google.refine.util.JSONUtilities;

public class ProcessManagerTests {
    
    ProcessManager processManager;
    Process process;
    
    @BeforeMethod
    public void setUp() {
        processManager = new ProcessManager();
        process = new LongRunningProcessTests.LongRunningProcessStub("some description");

    }
    
    @Test
    public void serializeProcessManager() throws Exception {
        processManager.queueProcess(process);
        String processJson = JSONUtilities.serialize(process);
        TestUtils.isSerializedTo(processManager, "{"
                + "\"processes\":["+processJson+"]}");
    }
}
