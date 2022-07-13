/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.process;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

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
        // Wait for process to complete to avoid race where they serialize with
        // different values for status: running vs done
        int total = 0;
        while (processManager.hasPending() && total < 1000) {
            Thread.sleep(100);
            total += 100;
        }
        String processJson = ParsingUtilities.defaultWriter.writeValueAsString(process2);
        TestUtils.isSerializedTo(processManager, "{"
                + "\"processes\":[" + processJson + "],\n"
                + "\"exceptions\":[{\"message\":\"unexpected error\"}]"
                + "}");
    }
}
