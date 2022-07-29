/*

Copyright 2010, Google Inc.
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

package com.google.refine.commands;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.RefineServlet;

public class GetVersionCommand extends Command {

    protected class VersionResponse {

        @JsonProperty("version")
        public String version = RefineServlet.VERSION;
        @JsonProperty("revision")
        public String revision = RefineServlet.REVISION;
        @JsonProperty("full_version")
        public String full_version = RefineServlet.FULL_VERSION;
        @JsonProperty("full_name")
        public String full_name = RefineServlet.FULLNAME;
        @JsonProperty("java_vm_name")
        public String java_vm_name = System.getProperty("java.vm.name", "?");
        @JsonProperty("java_vm_version")
        public String java_vm_version = System.getProperty("java.vm.version", "?");
        @JsonProperty("java_runtime_name")
        public String java_runtime_name = System.getProperty("java.runtime.name", "?");
        @JsonProperty("java_runtime_version")
        // public String java_runtime_version = Runtime.getRuntime().version(); // Java 9 or later
        public String java_runtime_version = System.getProperty("java.runtime.version", "?");
        @JsonProperty("display_new_version_notice")
        public String display_new_version_notice = System.getProperty("refine.display.new.version.notice");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        respondJSON(response, new VersionResponse());
    }
}
