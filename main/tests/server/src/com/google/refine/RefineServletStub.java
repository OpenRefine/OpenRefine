/*

Copyright 2010 Google Inc.
Copyright 2019,2020 OpenRefine contributors
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

package com.google.refine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.commands.Command;

/**
 * Exposes protected methods of com.google.refine.RefineServlet as public for unit testing
 *
 */
public class RefineServletStub extends RefineServlet {

    private static File tempDir = null;

    // requirement of extending HttpServlet, not required for testing
    private static final long serialVersionUID = 1L;

    public void wrapService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }

    public String wrapGetCommandName(HttpServletRequest request) {
        return super.getCommandKey(request);
    }

    @Override
    public File getTempDir() {
        if (tempDir == null) {
            try {
                Path tempDirPath = Files.createTempDirectory("refine-test-dir");
                tempDir = tempDirPath.toFile();
                tempDir.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }
        }
        return tempDir;
    }

    // -------------------helper methods--------------
    /**
     * Helper method for inserting a mock object
     * 
     * @param commandName
     * @param command
     */
    public void insertCommand(String commandName, Command command) {
        registerOneCommand("core/" + commandName, command);
    }

    /**
     * Helper method for clearing up after testing
     * 
     * @param commandName
     */
    public void removeCommand(String commandName) {
        unregisterCommand("core/" + commandName);
    }
}
