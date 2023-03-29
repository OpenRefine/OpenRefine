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

import java.util.concurrent.Callable;

import org.testng.annotations.Test;

import org.openrefine.model.changes.ChangeDataId;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ProcessTests {

    public static class ProcessStub extends Process {

        Callable<ProgressingFuture<Void>> future;
        ChangeDataId changeDataId;

        protected ProcessStub(String description, ChangeDataId changeDataId, Callable<ProgressingFuture<Void>> future) {
            super(description);
            this.future = future;
            this.changeDataId = changeDataId;
        }

        @Override
        protected ProgressingFuture<Void> getFuture() {
            try {
                return future.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ChangeDataId getChangeDataId() {
            return changeDataId;
        }
    }

    @Test
    public void serializeLongRunningProcess() {
        Process process = new ProcessStub("some description", new ChangeDataId(1234L, "recon"), () -> null);
        int hashCode = process.hashCode();
        TestUtils.isSerializedTo(process, "{"
                + "\"id\":" + hashCode + ","
                + "\"description\":\"some description\","
                + "\"changeDataId\":{\"historyEntryId\":1234,\"subDirectory\":\"recon\"},"
                + "\"status\":\"pending\","
                + "\"done\": false,"
                + "\"running\": false,"
                + "\"canceled\": false,"
                + "\"paused\": false,"
                + "\"progress\":0}", ParsingUtilities.defaultWriter);
    }
}
