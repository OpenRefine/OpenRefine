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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;
import com.google.api.services.fusiontables.Fusiontables;
import com.google.api.services.fusiontables.model.Table;
import com.google.api.services.fusiontables.model.TableList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import com.google.refine.ProjectManager;
import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importing.DefaultImportingController;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.model.metadata.ProjectMetadata;
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
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("documents");
            writer.array();
            
            try {
                listSpreadsheets(GoogleAPIExtension.getDriveService(token), writer);
                listFusionTables(FusionTableHandler.getFusionTablesService(token), writer);
            }  catch (Exception e) {
                logger.error("doListDocuments exception:" + ExceptionUtils.getStackTrace(e));
            }  finally {
                writer.endArray();
                writer.endObject();
            }
        } catch (JSONException e) {
            throw new ServletException(e);
        } finally {
            w.flush();
            w.close();
        }
    }
    
    private void listSpreadsheets(Drive drive, JSONWriter writer)
            throws IOException, JSONException {
         com.google.api.services.drive.Drive.Files.List files = drive.files().list();
         files.setQ("mimeType = 'application/vnd.google-apps.spreadsheet'");
         files.setFields("nextPageToken, files(id, name, webViewLink, owners, modifiedTime)");
         FileList fileList = files.execute();
         
        for (File entry : fileList.getFiles()) {
            writer.object();
            writer.key("docId"); writer.value(entry.getId());
            writer.key("docLink"); writer.value(entry.getWebViewLink());
            writer.key("docSelfLink"); writer.value(entry.getWebViewLink());
            writer.key("title"); writer.value(entry.getName());
            
            writer.key("type"); writer.value("spreadsheet");
            
            com.google.api.client.util.DateTime updated = entry.getModifiedTime();
            if (updated != null) {
                writer.key("updated"); writer.value(updated.toString());
            }
            
            writer.key("authors"); writer.array();
            for (User user : entry.getOwners()) {
                writer.value(user.getDisplayName());
            }
            writer.endArray();
            
            writer.endObject();
        }
    }
    
    private void listFusionTables(Fusiontables service, JSONWriter writer)
        throws IOException, JSONException {
        
        Fusiontables.Table.List listTables = service.table().list();
        TableList tablelist = listTables.execute();
        
        if (tablelist == null || tablelist.getItems() == null)
            return;
        
        for (Table table : tablelist.getItems()) {
            String id = table.getTableId();
            String name = table.getName();
            String link = "https://www.google.com/fusiontables/DataSource?docid=" + id;
            
            // Add JSON object to our stream
            writer.object();
            writer.key("docId"); writer.value(id);
            writer.key("docLink"); writer.value(link);
            writer.key("docSelfLink"); writer.value(link);
            writer.key("title"); writer.value(name);
            writer.key("type"); writer.value("table");
            writer.endObject();
        }
    }
    
    private void doInitializeParserUI(
        HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        String token = TokenCookie.getToken(request);
        String type = parameters.getProperty("docType");
        String urlString = parameters.getProperty("docUrl");
        JSONObject result = new JSONObject();
        JSONObject options = new JSONObject();
        
        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, "options", options);
        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
        
        if ("spreadsheet".equals(type)) {
            JSONArray worksheets = new JSONArray();
            // extract spreadSheetId from URL
            String spreadSheetId = GoogleAPIExtension.extractSpreadSheetId(urlString);
            
            JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
            JSONUtilities.safePut(options, "headerLines", 1); // number of header lines
            JSONUtilities.safePut(options, "worksheets", worksheets);
            
            List<Sheet> worksheetEntries =
                    getWorksheetEntriesForDoc(token, spreadSheetId);
            for (Sheet sheet : worksheetEntries) {
                JSONObject worksheetO = new JSONObject();
                JSONUtilities.safePut(worksheetO, "name", sheet.getProperties().getTitle());
                JSONUtilities.safePut(worksheetO, "rows", sheet.getProperties().getGridProperties().getRowCount());
                JSONUtilities.safePut(worksheetO, "link", 
                        "https://sheets.googleapis.com/v4/spreadsheets/" + spreadSheetId + "/values/" + sheet.getProperties().getTitle());
                
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
        try {
            JSONObject optionObj = ParsingUtilities.evaluateJsonStringToObject(
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
                exceptions
            );
            
            Writer w = response.getWriter();
            JSONWriter writer = new JSONWriter(w);
            try {
                writer.object();
                if (exceptions.size() == 0) {
                    job.project.update(); // update all internal models, indexes, caches, etc.
                    
                    writer.key("status"); writer.value("ok");
                } else {
                    writer.key("status"); writer.value("error");
                    
                    writer.key("errors");
                    writer.array();
                    DefaultImportingController.writeErrors(writer, exceptions);
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
        } finally {
            job.touch();
            job.updating = false;
        }
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
        try {
            final JSONObject optionObj = ParsingUtilities.evaluateJsonStringToObject(
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
                            exceptions
                        );
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
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }
}
