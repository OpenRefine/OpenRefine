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

package org.openrefine.process;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.operations.exceptions.ChangeDataFetchingException;
import org.openrefine.process.Process.State;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProcessManagerTests {

    ProcessManager processManager;
    Process firstProcess, secondProcess, erroringProcess;

    @BeforeMethod
    public void setUp() {
        processManager = new ProcessManager();
        ProgressingFuture<Void> firstFuture = mock(VoidProgressingFuture.class);
        ProgressingFuture<Void> secondFuture = mock(VoidProgressingFuture.class);
        firstProcess = new ProcessStub("first process", new ChangeDataId(123L, "data"), () -> firstFuture);
        secondProcess = new ProcessStub("second process", new ChangeDataId(456L, "data"), () -> secondFuture);
        erroringProcess = new ProcessStub("erroring process", new ChangeDataId(789L, "data"), () -> {
            throw new ChangeDataFetchingException("some problem occured!", true);
        });
    }

    @AfterMethod
    public void tearDown() {
        processManager.shutdown();
    }

    @Test
    public void testLifeCycle() {
        assertEquals(processManager.getProcesses(), Collections.emptyList());
        assertFalse(processManager.hasPending());

        processManager.queueProcess(firstProcess);

        assertEquals(processManager.getProcesses(), Collections.singletonList(firstProcess));
        assertTrue(processManager.hasPending());
        assertEquals(firstProcess.getState(), State.RUNNING);

        processManager.queueProcess(secondProcess);

        assertEquals(processManager.getProcesses(), Arrays.asList(firstProcess, secondProcess));
        assertTrue(processManager.hasPending());
        assertEquals(firstProcess.getState(), State.RUNNING);
        assertEquals(secondProcess.getState(), State.PENDING);
    }

    @Test
    public void testErroringProcess() {
        assertEquals(processManager.getProcesses(), Collections.emptyList());
        assertFalse(processManager.hasPending());

        processManager.queueProcess(erroringProcess);

        assertEquals(processManager.getProcesses(), Collections.singletonList(erroringProcess));
        assertTrue(processManager.hasPending());
        assertEquals(erroringProcess.getState(), State.FAILED);
        assertEquals(erroringProcess.getErrorMessage(), "some problem occured!");

        processManager.queueProcess(secondProcess);

        assertEquals(processManager.getProcesses(), Arrays.asList(erroringProcess, secondProcess));
        assertTrue(processManager.hasPending());
        assertEquals(erroringProcess.getState(), State.FAILED);
        assertEquals(secondProcess.getState(), State.PENDING);
    }

    @Test
    public void testSerialize() throws Exception {
        processManager.queueProcess(firstProcess);
        processManager.queueProcess(secondProcess);

        String expectedJson = "{"
                + "  \"processes\" : [ {"
                + "    \"changeDataId\" : {"
                + "      \"historyEntryId\" : 123,"
                + "      \"subDirectory\" : \"data\""
                + "    },"
                + "    \"description\" : \"first process\","
                + "    \"id\" : " + firstProcess.getId() + ","
                + "    \"progress\" : 0,"
                + "    \"state\" : \"running\""
                + "  }, {"
                + "    \"changeDataId\" : {"
                + "      \"historyEntryId\" : 456,"
                + "      \"subDirectory\" : \"data\""
                + "    },"
                + "    \"description\" : \"second process\","
                + "    \"id\" : " + secondProcess.getId() + ","
                + "    \"progress\" : 0,"
                + "    \"state\" : \"pending\""
                + "  } ]"
                + "}";
        TestUtils.isSerializedTo(processManager, expectedJson, ParsingUtilities.defaultWriter);
    }

    private static interface VoidProgressingFuture extends ProgressingFuture<Void> {

    }

}
