/*

Copyright 2011, Google Inc.
          2013, 2020 OpenRefine contributors
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

package com.google.refine.extension.gdata;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.DefaultImportingController;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class GDataImportingController implements ImportingController {

    private static final Logger logger = LoggerFactory.getLogger("GDataImportingController");
    protected RefineServlet servlet;

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        Properties parameters = ParsingUtilities.parseUrlParameters(request);
        String subCommand = parameters.getProperty("subCommand");
        if ("list-documents".equals(subCommand)) {
            doListDocuments(request, response, parameters);
        } else if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("parse-preview".equals(subCommand)) {
            doParsePreview(request, response, parameters);
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }
    }

    private void doListDocuments(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            writer.writeArrayFieldStart("documents");

            try {
                listSpreadsheets(GoogleAPIExtension.getDriveService(token), writer);
            } catch (Exception e) {
                logger.error("doListDocuments exception:" + ExceptionUtils.getStackTrace(e));
            } finally {
                writer.writeEndArray();
                writer.writeEndObject();
            }
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            writer.flush();
            writer.close();
            w.flush();
            w.close();
        }
    }

    private void listSpreadsheets(Drive drive, JsonGenerator writer)
            throws IOException {
        com.google.api.services.drive.Drive.Files.List files = drive.files().list();
        files.setQ("mimeType = 'application/vnd.google-apps.spreadsheet'");
        files.setFields("nextPageToken, files(id, name, webViewLink, owners, modifiedTime)");
        FileList fileList = files.execute();

        for (File entry : fileList.getFiles()) {
            writer.writeStartObject();
            writer.writeStringField("docId", entry.getId());
            writer.writeStringField("docLink", entry.getWebViewLink());
            writer.writeStringField("docSelfLink", entry.getWebViewLink());
            writer.writeStringField("title", entry.getName());

            writer.writeStringField("type", "spreadsheet");

            com.google.api.client.util.DateTime updated = entry.getModifiedTime();
            if (updated != null) {
                writer.writeStringField("updated", updated.toString());
            }

            writer.writeArrayFieldStart("authors");
            for (User user : entry.getOwners()) {
                writer.writeString(user.getDisplayName());
            }
            writer.writeEndArray();

            writer.writeEndObject();
        }
    }

    private void doInitializeParserUI(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        String token = TokenCookie.getToken(request);
        String type = parameters.getProperty("docType");
        String urlString = parameters.getProperty("docUrl");
        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();

        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, "options", options);
        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);

        if ("spreadsheet".equals(type)) {
            ArrayNode worksheets = ParsingUtilities.mapper.createArrayNode();
            // extract spreadSheetId from URL
            String spreadSheetId = GoogleAPIExtension.extractSpreadSheetId(urlString);

            JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
            JSONUtilities.safePut(options, "headerLines", 1); // number of header lines
            JSONUtilities.safePut(options, "worksheets", worksheets);

            List<Sheet> worksheetEntries = getWorksheetEntriesForDoc(token, spreadSheetId);
            int workSheetIndex = 0;
            for (Sheet sheet : worksheetEntries) {
                ObjectNode worksheetO = ParsingUtilities.mapper.createObjectNode();
                JSONUtilities.safePut(worksheetO, "name", sheet.getProperties().getTitle());
                JSONUtilities.safePut(worksheetO, "rows", sheet.getProperties().getGridProperties().getRowCount());
                JSONUtilities.safePut(worksheetO, "link",
                        "https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetId + "/values/" + sheet.getProperties().getTitle());
                JSONUtilities.safePut(worksheetO, "worksheetIndex", workSheetIndex++);

                JSONUtilities.append(worksheets, worksheetO);
            }
        } else if ("table".equals(type)) {
            // No metadata for a fusion table.
        }

        HttpUtilities.respond(response, result.toString());
    }

    private List<Sheet> getWorksheetEntriesForDoc(String token, String spreadsheetId) throws IOException {
        Sheets sheetsService = GoogleAPIExtension.getSheetsService(token);

        boolean includeGridData = true;

        Sheets.Spreadsheets.Get request = sheetsService.spreadsheets().get(spreadsheetId);
        request.setIncludeGridData(includeGridData);

        Spreadsheet response = request.execute();

        return response.getSheets();
    }

    private void doParsePreview(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        String token = TokenCookie.getToken(request);

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        job.prepareNewProject();

        GDataImporter.parse(
                token,
                job.project,
                job.metadata,
                job,
                100,
                optionObj,
                exceptions);

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            if (exceptions.size() == 0) {
                job.project.update(); // update all internal models, indexes, caches, etc.

                writer.writeStringField("status", "ok");
            } else {
                writer.writeStringField("status", "error");

                writer.writeArrayFieldStart("errors");
                DefaultImportingController.writeErrors(writer, exceptions);
                writer.writeEndArray();
            }
            writer.writeEndObject();
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            writer.flush();
            writer.close();
            w.flush();
            w.close();
        }

        job.touch();
        job.updating = false;
    }

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        final String token = TokenCookie.getToken(request);

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        final ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                request.getParameter("options"));

        final List<Exception> exceptions = new LinkedList<Exception>();

        job.setState("creating-project");

        final Project project = new Project();
        new Thread() {

            @Override
            public void run() {
                ProjectMetadata pm = new ProjectMetadata();
                pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
                pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));

                try {
                    GDataImporter.parse(
                            token,
                            project,
                            pm,
                            job,
                            -1,
                            optionObj,
                            exceptions);
                } catch (IOException e) {
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

                if (!job.canceled) {
                    if (exceptions.size() > 0) {
                        job.setError(exceptions);
                    } else {
                        project.update(); // update all internal models, indexes, caches, etc.

                        ProjectManager.singleton.registerProject(project, pm);

                        job.setState("created-project");
                        job.setProjectID(project.id);
                    }

                    job.touch();
                    job.updating = false;
                }
            }
        }.start();

        HttpUtilities.respond(response, "ok", "done");
    }
}
