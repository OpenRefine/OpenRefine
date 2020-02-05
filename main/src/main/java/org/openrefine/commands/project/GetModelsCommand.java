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

package org.openrefine.commands.project;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.commands.Command;
import org.openrefine.commands.HttpHeadersSupport;
import org.openrefine.commands.HttpHeadersSupport.HttpHeaderInfo;
import org.openrefine.expr.MetaParser;
import org.openrefine.expr.MetaParser.LanguageInfo;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Project;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ParsingUtilities;

public class GetModelsCommand extends Command {

    /**
     * This command uses POST but is left CSRF-unprotected as it does not incur a state change.
     */

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        internalRespond(request, response);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        internalRespond(request, response);
    }

    protected static class ModelsResponse {

        @JsonProperty("columnModel")
        protected ColumnModel columnModel;
        /*
         * TODO reintroduce RecordModel
         */
        @JsonProperty("recordModel")
        protected ObjectNode recordModel;
        @JsonProperty("overlayModels")
        protected Map<String, OverlayModel> overlayModels;
        @JsonProperty("scripting")
        protected Map<String, LanguageInfo> scripting;
        @JsonProperty("httpHeaders")
        protected Map<String, HttpHeaderInfo> httpHeaders;

        protected ModelsResponse(
                ColumnModel columns,
                // RecordModel records,
                Map<String, OverlayModel> overlays,
                Map<String, LanguageInfo> languageInfos,
                Map<String, HttpHeaderInfo> headers) {
            columnModel = columns;
            // recordModel = records;
            try {
                recordModel = ParsingUtilities.mapper.readValue("{\"hasRecords\":false}", ObjectNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            overlayModels = overlays;
            scripting = languageInfos;
            httpHeaders = headers;
        }
    }

    protected void internalRespond(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Project project = null;

        // This command also supports retrieving rows for an importing job.
        String importingJobID = request.getParameter("importingJobID");
        if (importingJobID != null) {
            long jobID = Long.parseLong(importingJobID);
            ImportingJob job = ImportingManager.getJob(jobID);
            if (job != null) {
                project = job.getProject();
            }
        }
        if (project == null) {
            project = getProject(request);
        }

        response.setHeader("Cache-Control", "no-cache");

        Map<String, LanguageInfo> prefixesMap = new HashMap<>();
        for (String languagePrefix : MetaParser.getLanguagePrefixes()) {
            LanguageInfo info = MetaParser.getLanguageInfo(languagePrefix);
            prefixesMap.put(languagePrefix, info);
        }

        Map<String, HttpHeaderInfo> headersMap = new HashMap<>();
        for (String headerLabel : HttpHeadersSupport.getHttpHeaderLabels()) {
            HttpHeaderInfo info = HttpHeadersSupport.getHttpHeaderInfo(headerLabel);
            headersMap.put(headerLabel, info);
        }

        respondJSON(response, new ModelsResponse(
                project.getColumnModel(),
                // project.recordModel,
                project.getOverlayModels(),
                prefixesMap,
                headersMap));
    }

}
