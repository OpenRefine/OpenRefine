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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class ImportProjectCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("import-project_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFTokenAsGET(request)) {
            respondCSRFError(response);
            return;
        }

        ProjectManager.singleton.setBusy(true);
        try {
            Properties options = ParsingUtilities.parseUrlParameters(request);

            long projectID = Project.generateID();
            logger.info("Importing existing project using new ID {}", projectID);

            internalImport(request, options, projectID);

            ProjectManager.singleton.loadProjectMetadata(projectID);

            ProjectMetadata pm = ProjectManager.singleton.getProjectMetadata(projectID);
            if (pm != null) {
                if (options.containsKey("project-name")) {
                    String projectName = options.getProperty("project-name");
                    if (projectName != null && projectName.length() > 0) {
                        pm.setName(projectName);
                    }
                }

                redirect(response, "/project?project=" + projectID);
            } else {
                respondWithErrorPage(request, response, "Failed to import project. Reason unknown.", null);
            }
        } catch (Exception e) {
            respondWithErrorPage(request, response, "Failed to import project", e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }

    protected void internalImport(
            HttpServletRequest request,
            Properties options,
            long projectID) throws Exception {

        String url = null;

        ServletFileUpload upload = new ServletFileUpload();

        FileItemIterator iter = upload.getItemIterator(request);
        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            String name = item.getFieldName().toLowerCase();
            InputStream stream = item.openStream();
            if (item.isFormField()) {
                if (name.equals("url")) {
                    url = Streams.asString(stream);
                } else {
                    options.put(name, Streams.asString(stream));
                }
            } else {
                String fileName = item.getName().toLowerCase();
                try {
                    ProjectManager.singleton.importProject(projectID, stream, !fileName.endsWith(".tar"));
                } finally {
                    stream.close();
                }
            }
        }

        if (url != null && url.length() > 0) {
            internalImportURL(request, options, projectID, url);
        }
    }

    protected void internalImportURL(
            HttpServletRequest request,
            Properties options,
            long projectID,
            String urlString) throws Exception {
        URL url = new URL(urlString);
        URLConnection connection = null;

        try {
            connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
        } catch (Exception e) {
            throw new Exception("Cannot connect to " + urlString, e);
        }

        InputStream inputStream = null;
        try {
            inputStream = connection.getInputStream();
        } catch (Exception e) {
            throw new Exception("Cannot retrieve content from " + url, e);
        }

        try {
            ProjectManager.singleton.importProject(projectID, inputStream, !urlString.endsWith(".tar"));
        } finally {
            inputStream.close();
        }
    }

}
