/*

Copyright 2010, 2025 Google Inc. & OpenRefine contributors
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

package com.google.refine.commands.workspace;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;

public class GetProjectsCommand extends Command {

    static final int DEFAULT_LIMIT = 50;

    public static class ProjectMetadataWithId {

        @JsonProperty("id")
        protected long id;

        @JsonUnwrapped
        protected ProjectMetadata metadata;

        protected ProjectMetadataWithId(long id, ProjectMetadata metadata) {
            this.id = id;
            this.metadata = metadata;
        }
    }

    public static class ProjectsResponse {

        @JsonProperty("projects")
        protected List<ProjectMetadataWithId> projects;
        @JsonProperty("total")
        protected int total;
        @JsonProperty("start")
        protected int start;
        @JsonProperty("limit")
        protected int limit;
        @JsonProperty("customMetadataColumns")
        @JsonInclude(Include.NON_NULL)
        @JsonRawValue
        protected String customMetadataColumns;

        protected ProjectsResponse(List<ProjectMetadataWithId> projects, int total, int start, int limit, String customMetadataColumns) {
            this.projects = projects;
            this.total = total;
            this.start = start;
            this.limit = limit;
            this.customMetadataColumns = customMetadataColumns;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        int start = getIntegerParameter(request, "start", 0);
        int limit = getIntegerParameter(request, "limit", DEFAULT_LIMIT);

        if (start < 0) {
            start = 0;
        }
        if (limit < 0) {
            limit = DEFAULT_LIMIT;
        }

        Map<Long, ProjectMetadata> allProjects = ProjectManager.singleton.getAllProjectMetadata();
        String userMeta = (String) ProjectManager.singleton.getPreferenceStore().get("userMetadata");

        int total = allProjects.size();
        List<ProjectMetadataWithId> page = allProjects.entrySet().stream()
                .skip(start)
                .limit(limit)
                .map(entry -> new ProjectMetadataWithId(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        respondJSON(response, new ProjectsResponse(page, total, start, limit, userMeta));
    }
}
