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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.ByteStreams;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineModel;
import org.openrefine.commands.Command;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.importing.ImportingManager;
import org.openrefine.importing.ImportingUtilities;
import org.openrefine.model.Project;
import org.openrefine.model.Runner;
import org.openrefine.util.ParsingUtilities;

public class ImportProjectCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("import-project_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasValidCSRFTokenAsGET(request)) {
            respondCSRFError(response);
            return;
        }

        try {
            Properties options = ParsingUtilities.parseUrlParameters(request);

            long projectID = internalImport(request, options);

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
        }
    }

    protected long internalImport(
            HttpServletRequest request,
            Properties options) throws Exception {

        String url = null;
        long projectID = 0;

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
                    projectID = importProject(stream, !fileName.endsWith(".tar"));
                } finally {
                    stream.close();
                }
            }
        }

        if (url != null && url.length() > 0) {
            projectID = internalImportURL(request, options, url);
        }

        return projectID;
    }

    protected long internalImportURL(
            HttpServletRequest request,
            Properties options,
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
            return importProject(inputStream, !urlString.endsWith(".tar"));
        } finally {
            inputStream.close();
        }
    }

    protected long importProject(InputStream inputStream, boolean decompress) throws IOException {

        long projectID = 0;

        // First copy the candidate project data as an importing job, to have the opportunity to check
        // its format.
        ImportingJob job = ImportingManager.createJob();

        // Save the input stream to a file in the importer job dir
        String filename = decompress ? "project.tar.gz" : "project.tar";
        File targetFile = new File(job.getRawDataDir(), filename);
        OutputStream outputStream = new FileOutputStream(targetFile);
        long size = ByteStreams.copy(inputStream, outputStream);
        outputStream.close();

        // Inspect the stream to check if it looks like a legit project
        boolean dataZipFound = false;
        boolean historyJsonFound = false;
        InputStream importedInputStream = new FileInputStream(targetFile);
        if (decompress) {
            importedInputStream = new GZIPInputStream(importedInputStream);
        }
        TarArchiveInputStream tin = new TarArchiveInputStream(importedInputStream);
        TarArchiveEntry tarEntry = null;

        while ((tarEntry = tin.getNextTarEntry()) != null) {
            String name = tarEntry.getName();
            if ("data.zip".equals(name)) {
                dataZipFound = true;
                break;
            } else if ("history.json".equals(name)) {
                historyJsonFound = true;
                break;
            }
        }
        tin.close();

        if (historyJsonFound) {
            projectID = Project.generateID();
            logger.info("Importing existing project using new ID {}", projectID);
            // This looks like a valid OR project.
            // Just untar it directly in the project directory
            InputStream fileInputStream = new FileInputStream(targetFile);
            ProjectManager.singleton.importProject(projectID, fileInputStream, decompress);
        } else if (dataZipFound) {
            // If legacy project, then use the appropriate importer instead
            logger.info("Importing legacy project");

            // Add the ImportingFileRecord to the job
            ImportingFileRecord fileRecord = new ImportingFileRecord(
                    ImportingUtilities.getRelativePath(targetFile, job.getRawDataDir()), // location
                    filename, // fileName
                    size, // size
                    "project import", // origin
                    null, // declaredMimeType
                    null, // mimeType
                    null, // URL
                    null, // encoding
                    null, // declaredEncoding
                    "openrefine", // format
                    null // archiveFileName
            );
            job.getRetrievalRecord().files = Collections.singletonList(fileRecord);
            job.setFileSelection(Collections.singletonList(0));
            List<Exception> exceptions = new ArrayList<>();
            ObjectNode options = ParsingUtilities.mapper.createObjectNode();
            Runner runner = RefineModel.getRunner();
            projectID = ImportingUtilities.createProject(job, "openrefine-legacy", options, runner, exceptions, true);
        } else {
            throw new IOException("The supplied file could not be recognized as an OpenRefine project");
        }

        return projectID;
    }

}
