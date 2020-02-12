package com.google.refine.extension.gdata;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.File.ContentHints;
import com.google.api.services.drive.model.File.ContentHints.Thumbnail;
import com.google.refine.ProjectManager;
import com.google.refine.browsing.Engine;
import com.google.refine.commands.Command;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.commands.project.ExportRowsCommand;
import com.google.refine.exporters.CustomizableTabularExporterUtilities;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class UploadCommand extends Command {
    static final Logger logger = LoggerFactory.getLogger("gdata_upload");
    
    private static final String METADATA_DESCRIPTION = "OpenRefine project dump";
    private static final String METADATA_ICONLINK = "https://raw.githubusercontent.com/OpenRefine/OpenRefine/master/main/webapp/modules/core/images/logo-openrefine-550.png";
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	if(!hasValidCSRFToken(request)) {
    		respondCSRFError(response);
    		return;
    	}
        
        String token = TokenCookie.getToken(request);
        if (token == null) {
            HttpUtilities.respond(response, "error", "Not authorized");
            return;
        }

        ProjectManager.singleton.setBusy(true);
        try {
            Project project = getProject(request);
            Engine engine = getEngine(request, project);
            Properties params = ExportRowsCommand.getRequestParameters(request);
            String name = params.getProperty("name");
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            Writer w = response.getWriter();
            JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
            try {
                writer.writeStartObject();
                
                List<Exception> exceptions = new LinkedList<Exception>();
                String url = upload(project, engine, params, token, name, exceptions);
                if (url != null) {
                    writer.writeStringField("status", "ok");
                    writer.writeStringField("url", url);
                } else if (exceptions.size() == 0) {
                    writer.writeStringField("status", "error");
                    writer.writeStringField("message", "No such format");
                } else {
                    for (Exception e : exceptions) {
                        logger.warn(e.getLocalizedMessage(), e);
                    }
                    writer.writeStringField("status", "error");
                    writer.writeStringField("message", exceptions.get(0).getLocalizedMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
                writer.writeStringField("status", "error");
                writer.writeStringField("message", e.getMessage());
            } finally {
                writer.writeEndObject();
                writer.flush();
                writer.close();
                w.flush();
                w.close();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }

    static private String upload(
            Project project, Engine engine, Properties params,
            String token, String name, List<Exception> exceptions) {
        String format = params.getProperty("format");
        if ("gdata/google-spreadsheet".equals(format)) {
            return uploadSpreadsheet(project, engine, params, token, name, exceptions);
        } else if (("raw/openrefine-project").equals(format)) {
            return uploadOpenRefineProject(project, token, name, exceptions);
        }
        return null;
    }
    
    private static byte[] getImageFromUrl(String urlText) throws IOException {
        URL url = new URL(urlText);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
         
        try (InputStream inputStream = url.openStream()) {
            int n = 0;
            byte [] buffer = new byte[ 1024 ];
            while (-1 != (n = inputStream.read(buffer))) {
                output.write(buffer, 0, n);
            }
        }
     
        return output.toByteArray();
    }

    private static String uploadOpenRefineProject(Project project, String token,
            String name, List<Exception> exceptions) {
        FileOutputStream fos = null;
        
        try {
            java.io.File filePath = java.io.File.createTempFile(name, ".tgz"); 
            filePath.deleteOnExit();
            
            fos = new FileOutputStream(filePath);
            FileProjectManager.gzipTarToOutputStream(project, fos);
            
            File fileMetadata = new File();
            String asB64 = Base64.encodeBase64URLSafeString(getImageFromUrl(METADATA_ICONLINK));
            
            Thumbnail tn = new Thumbnail();
            tn.setMimeType("image/x-icon").setImage(asB64);
            ContentHints contentHints = new ContentHints();
            contentHints.setThumbnail(tn); 
            
            fileMetadata.setName(name)
                .setDescription(METADATA_DESCRIPTION)
                .setContentHints(contentHints);
            FileContent projectContent = new FileContent("application/zip", filePath);
            File file = GoogleAPIExtension.getDriveService(token)
                    .files().create(fileMetadata, projectContent)
                .setFields("id")
                .execute();
            logger.info("File ID: " + file.getId());
            
            return file.getId();
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            exceptions.add(e);
        } 
        
        return null;
    }

    static private String uploadSpreadsheet(
            final Project project, final Engine engine, final Properties params,
            String token, String name, List<Exception> exceptions) {
        
        Drive driveService = GoogleAPIExtension.getDriveService(token);
        
        try {
            File body = new File();
            body.setName(name);
            body.setDescription("Spreadsheet uploaded from OpenRefine project: " + name);
            body.setMimeType("application/vnd.google-apps.spreadsheet");

            File file = driveService.files().create(body).execute();
            String spreadsheetId =  file.getId();

            SpreadsheetSerializer serializer = new SpreadsheetSerializer(
                    GoogleAPIExtension.getSheetsService(token),
                    spreadsheetId,
                    exceptions);
            
            CustomizableTabularExporterUtilities.exportRows(
                    project, engine, params, serializer);
            
            return serializer.getUrl();
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            exceptions.add(e);
        }
        return null;
    }
}
