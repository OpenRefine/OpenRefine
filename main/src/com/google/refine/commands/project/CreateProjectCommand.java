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

package com.google.refine.commands.project;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class CreateProjectCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("create-project_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProjectManager.singleton.setBusy(true);
        try {

            /*
             * The uploaded file is in the POST body as a "file part". If
             * we call request.getParameter() then the POST body will get
             * read and we won't have a chance to parse the body ourselves.
             * This is why we have to parse the URL for parameters ourselves.
             * Don't call request.getParameter() before calling internalImport().
             */
            Properties options = ParsingUtilities.parseUrlParameters(request);

            Project project = new Project();
            ProjectMetadata pm = new ProjectMetadata();

            //internalImport(request, project, pm, options);

            /*
             * The import process above populates options with parameters
             * in the POST body. That's why we're constructing the project
             * metadata object after calling internalImport().
             */
            pm.setName(options.getProperty("project-name"));
            pm.setPassword(options.getProperty("project-password"));
            pm.setEncoding(options.getProperty("encoding"));
            pm.setEncodingConfidence(options.getProperty("encoding_confidence"));
            ProjectManager.singleton.registerProject(project, pm);

            project.update();

            HttpUtilities.redirect(response, "/project?project=" + project.id);
        } catch (Exception e) {
            respondWithErrorPage(request, response, "Failed to import file", e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }
}
