/*

Copyright 2011, Google Inc.
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
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.gdata.client.docs.DocsService;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Person;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class GDataImportingController implements ImportingController {

    protected RefineServlet servlet;
    
    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

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
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("documents");
            writer.array();
            
            try {
                DocsService service = getDocsService(token);
                
                URL metafeedUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
                SpreadsheetFeed feed = service.getFeed(metafeedUrl, SpreadsheetFeed.class);
                for (SpreadsheetEntry entry : feed.getEntries()) {
                    writer.object();
                    writer.key("docId"); writer.value(entry.getId());
                    writer.key("docLink"); writer.value(entry.getHtmlLink().getHref());
                    writer.key("docSelfLink"); writer.value(entry.getSelfLink().getHref());
                    writer.key("title"); writer.value(entry.getTitle().getPlainText());
                    
                    DateTime updated = entry.getUpdated();
                    if (updated != null) {
                        writer.key("updated"); writer.value(updated.toStringRfc822());
                    }
                    
                    writer.key("authors"); writer.array();
                    for (Person person : entry.getAuthors()) {
                        writer.value(person.getName());
                    }
                    writer.endArray();
                    
                    writer.endObject();
                }
            } catch (ServiceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            writer.endArray();
            writer.endObject();
        } catch (JSONException e) {
            throw new ServletException(e);
        } finally {
            w.flush();
            w.close();
        }
    }
    
    private void doInitializeParserUI(
        HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
    
        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }
        
        SpreadsheetService service = getSpreadsheetService(token);
        try {
            JSONObject result = new JSONObject();
            JSONObject options = new JSONObject();
            JSONUtilities.safePut(result, "status", "ok");
            JSONUtilities.safePut(result, "options", options);
            
            JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
            JSONUtilities.safePut(options, "headerLines", 1); // number of header lines
            JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
            JSONUtilities.safePut(options, "storeBlankRows", true);
            JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
            
            JSONArray worksheets = new JSONArray();
            JSONUtilities.safePut(options, "worksheets", worksheets);
            
            String urlString = parameters.getProperty("docUrl");
            URL url = new URL(urlString);
            
            SpreadsheetEntry spreadsheetEntry = service.getEntry(url, SpreadsheetEntry.class);
            for (WorksheetEntry worksheetEntry : spreadsheetEntry.getWorksheets()) {
                JSONObject worksheetO = new JSONObject();
                JSONUtilities.safePut(worksheetO, "name", worksheetEntry.getTitle().getPlainText());
                JSONUtilities.safePut(worksheetO, "rows", worksheetEntry.getRowCount());
                JSONUtilities.safePut(worksheetO, "link", worksheetEntry.getSelfLink().getHref());
                
                JSONUtilities.append(worksheets, worksheetO);
            }
            HttpUtilities.respond(response, result.toString());
        } catch (ServiceException e) {
            e.printStackTrace();
            HttpUtilities.respond(response, "error", "Internal error: " + e.getLocalizedMessage());
        }
    }
    
    private void doParsePreview(
        HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        
        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }
        
        SpreadsheetService service = getSpreadsheetService(token);

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }
    
        try {
            // This is for setting progress during the parsing process.
            job.config = new JSONObject();
            
            JSONObject optionObj = ParsingUtilities.evaluateJsonStringToObject(
                request.getParameter("options"));
            
            List<Exception> exceptions = new LinkedList<Exception>();
            
            job.prepareNewProject();
            
            GDataImporter.parse(
                service,
                job.project,
                job.metadata,
                job,
                100,
                optionObj,
                exceptions
            );
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                if (exceptions.size() == 0) {
                    job.project.update(); // update all internal models, indexes, caches, etc.
                    
                    writer.key("code"); writer.value("ok");
                } else {
                    writer.key("code"); writer.value("error");
                    
                    writer.key("errors");
                    writer.array();
                    for (Exception e : exceptions) {
                        writer.value(e.getLocalizedMessage());
                    }
                    writer.endArray();
                }
                writer.endObject();
            } catch (JSONException e) {
                throw new ServletException(e);
            } finally {
                w.flush();
                w.close();
            }

        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }
    
    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
        throws ServletException, IOException {
    
        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }
        final SpreadsheetService service = getSpreadsheetService(token);

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }
    
        try {
            final JSONObject optionObj = ParsingUtilities.evaluateJsonStringToObject(
                request.getParameter("options"));
            
            final List<Exception> exceptions = new LinkedList<Exception>();
            
            JSONUtilities.safePut(job.config, "state", "creating-project");
            
            final Project project = new Project();
            new Thread() {
                @Override
                public void run() {
                    ProjectMetadata pm = new ProjectMetadata();
                    pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
                    pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));
                    
                    GDataImporter.parse(
                        service,
                        project,
                        pm,
                        job,
                        -1,
                        optionObj,
                        exceptions
                    );
                    
                    if (!job.canceled) {
                        project.update(); // update all internal models, indexes, caches, etc.
                        
                        ProjectManager.singleton.registerProject(project, pm);
                        
                        JSONUtilities.safePut(job.config, "projectID", project.id);
                        JSONUtilities.safePut(job.config, "state", "created-project");
                    }
                }
            }.start();
            
            HttpUtilities.respond(response, "ok", "done");
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }
    
    private DocsService getDocsService(String token) {
        DocsService service = new DocsService(GDataExtension.SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }
    
    private SpreadsheetService getSpreadsheetService(String token) {
        SpreadsheetService service = new SpreadsheetService(GDataExtension.SERVICE_APP_NAME);
        service.setAuthSubToken(token);
        return service;
    }
}
