/*

Copyright 2026, OpenRefine contributors
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.util.logging;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class IndentingLayoutTest {

    @DataProvider
    public Object[][] levels() {
        return new Object[][] { { Level.TRACE }, { Level.DEBUG }, { Level.INFO }, { Level.WARN }, { Level.ERROR },
                { Level.FATAL } };
    }

    private String format(IndentingLayout layout, Level level, String message, Throwable thrown) {
        return layout.toSerializable(Log4jLogEvent.newBuilder().setLoggerName("refine").setLevel(level)
                .setMessage(new SimpleMessage(message)).setTimeMillis(1000).setThrown(thrown).build());
    }

    @Test(dataProvider = "levels")
    public void includesLevel(Level level) {
        IndentingLayout layout = IndentingLayout.createLayout(StandardCharsets.UTF_8);
        String output = format(layout, level, "A log message", null);
        Assert.assertTrue(output.contains(" " + level + " ["), output);
        Assert.assertTrue(output.endsWith("A log message (0ms)\n"), output);
    }

    @Test
    public void includesLevelForShortMessages() {
        IndentingLayout layout = IndentingLayout.createLayout(StandardCharsets.UTF_8);
        for (String message : new String[] { "", "x" }) {
            String output = format(layout, Level.WARN, message, null);
            Assert.assertTrue(output.contains(" WARN ["), output);
            Assert.assertTrue(output.endsWith(message + " (0ms)\n"), output);
        }
    }

    @Test
    public void preservesIndentationAndThrowable() {
        IndentingLayout layout = IndentingLayout.createLayout(StandardCharsets.UTF_8);
        format(layout, Level.DEBUG, "> enter", null);
        String nested = format(layout, Level.ERROR, "Failed", new IllegalStateException("detail"));
        Assert.assertTrue(nested.contains("]  Failed (0ms)\n"), nested);
        Assert.assertTrue(nested.contains("java.lang.IllegalStateException: detail"), nested);
        String exited = format(layout, Level.DEBUG, "< exit", null);
        Assert.assertTrue(exited.contains("] < exit (0ms)\n"), exited);
    }
}
