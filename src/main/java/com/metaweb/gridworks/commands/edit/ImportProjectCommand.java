package com.metaweb.gridworks.commands.edit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

public class ImportProjectCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Properties options = ParsingUtilities.parseUrlParameters(request);
            long projectID = Project.generateID();
            
            internalImport(request, options, projectID);

            ProjectManager.singleton.importProject(projectID);
            if (options.containsKey("project-name")) {
                String projectName = options.getProperty("project-name");
                if (projectName != null && projectName.length() > 0) {
                    ProjectManager.singleton.getProjectMetadata(projectID).setName(projectName);
                }
            }
            
            redirect(response, "/project.html?project=" + projectID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected void internalImport(
        HttpServletRequest    request,
        Properties            options,
        long                  projectID
    ) throws Exception {
        MultipartParser parser = null;
        try {
            parser = new MultipartParser(request, 1024 * 1024 * 1024);
        } catch (Exception e) {
            // silent
        }
        
        if (parser != null) {
            Part part = null;
            String url = null;
            
            while ((part = parser.readNextPart()) != null) {
                if (part.isFile()) {
                    FilePart filePart = (FilePart) part;
                    InputStream inputStream = filePart.getInputStream();
                    try {
                        internalImportInputStream(projectID, inputStream);
                    } finally {
                        inputStream.close();
                    }
                } else if (part.isParam()) {
                    ParamPart paramPart = (ParamPart) part;
                    String paramName = paramPart.getName();
                    if (paramName.equals("url")) {
                        url = paramPart.getStringValue();
                    } else {
                        options.put(paramName, paramPart.getStringValue());
                    }
                }
            }
            
            if (url != null && url.length() > 0) {
                internalImportURL(request, options, projectID, url);
            }
        }
    }
    
    protected void internalImportURL(
        HttpServletRequest    request,
        Properties            options,
        long                  projectID,
        String                urlString
    ) throws Exception {
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
            internalImportInputStream(projectID, inputStream);
        } finally {
            inputStream.close();
        }
    }
    
    protected void internalImportInputStream(long projectID, InputStream inputStream) throws IOException {
        File destDir = ProjectManager.singleton.getProjectDir(projectID);
        destDir.mkdirs();
        
        untar(destDir, inputStream);
    }
    
    protected void untar(File destDir, InputStream inputStream) throws IOException {
        TarInputStream tin = new TarInputStream(inputStream);
        TarEntry tarEntry = null;
        
        while ((tarEntry = tin.getNextEntry()) != null) {
            File destEntry = new File(destDir, tarEntry.getName());
            File parent = destEntry.getParentFile();
            
            if (!parent.exists()) {
                parent.mkdirs();
            }
            
            if (tarEntry.isDirectory()) {
                destEntry.mkdirs();
            } else {
                FileOutputStream fout = new FileOutputStream(destEntry);
                try {
                    tin.copyEntryContents(fout);
                } finally {
                    fout.close();
                }
            }
        }
    }
}
