package com.metaweb.gridworks.commands.edit;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.metaweb.gridworks.Gridworks;
import com.metaweb.gridworks.ProjectManager;
import com.metaweb.gridworks.ProjectMetadata;
import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.importers.ExcelImporter;
import com.metaweb.gridworks.importers.Importer;
import com.metaweb.gridworks.importers.TsvCsvImporter;
import com.metaweb.gridworks.importers.XmlImporter;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

public class CreateProjectCommand extends Command {

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
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
            
            internalImport(request, project, options);

            /*
             * The import process above populates options with parameters
             * in the POST body. That's why we're constructing the project
             * metadata object after calling internalImport().
             */
            ProjectMetadata pm = new ProjectMetadata();
            pm.setName(options.getProperty("project-name"));
            pm.setPassword(options.getProperty("project-password"));
            pm.setEncoding(options.getProperty("encoding"));
            pm.setEncodingConfidence(options.getProperty("encoding_confidence"));
            ProjectManager.singleton.registerProject(project, pm);

            project.columnModel.update();
            project.recomputeRowContextDependencies();
            
            redirect(response, "/project.html?project=" + project.id);
        } catch (Exception e) {
            redirect(response, "/error.html?redirect=index.html&msg=" +
                ParsingUtilities.encode("Failed to import file: " + e.getLocalizedMessage())
            );
            e.printStackTrace();
        }
    }
    
    protected void internalImport(
        HttpServletRequest    request,
        Project               project,
        Properties            options
    ) throws Exception {
        MultipartParser parser = new MultipartParser(request, 1024 * 1024 * 1024);
        
        if (parser != null) {
            Part part = null;
            String url = null;
            
            while ((part = parser.readNextPart()) != null) {
                
                if (part.isFile()) {
                    internalImportFilePart((FilePart) part, project, options);
                    
                } else if (part.isParam()) {
                    ParamPart paramPart = (ParamPart) part;
                    String paramName = paramPart.getName();
                    
                    if (paramName.equals("raw-text")) {
                        StringReader reader = new StringReader(paramPart.getStringValue());
                        try {
                            internalInvokeImporter(project, new TsvCsvImporter(), options, reader);
                        } finally {
                            reader.close();
                        }
                    } else if (paramName.equals("url")) {
                        url = paramPart.getStringValue();
                    } else {
                        options.put(paramName, paramPart.getStringValue());
                    }
                }
            }
            
            if (url != null && url.length() > 0) {
                internalImportURL(request, project, options, url);
            }
        }
    }
    
    protected void internalImportFilePart(
        FilePart    filePart,
        Project     project,
        Properties  options
    ) throws Exception {
        
        Importer importer = guessImporter(options, null, filePart.getFileName());
        
        internalInvokeImporter(project, importer, options, filePart.getInputStream(), null);
    }
    
    protected void internalImportURL(
        HttpServletRequest    request,
        Project               project,
        Properties            options,
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
            Importer importer = guessImporter(
                options, 
                connection.getContentType(),
                url.getPath()
            );
            
            internalInvokeImporter(project, importer, options, inputStream, connection.getContentEncoding());
        } finally {
            inputStream.close();
        }
    }
    
    protected void internalInvokeImporter(
        Project     project,
        Importer    importer,
        Properties  options,
        InputStream rawInputStream,
        String      encoding
    ) throws Exception {
        
        int limit = -1;
        int skip = 0;
        
        if (options.containsKey("limit")) {
            String s = options.getProperty("limit");
            try {
                limit = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        if (options.containsKey("skip")) {
            String s = options.getProperty("skip");
            try {
                skip = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        
        BufferedInputStream inputStream = new BufferedInputStream(rawInputStream);
        
        if (importer.takesReader()) {
            /*
             * NOTE(SM): The ICU4J char detection code requires the input stream to support mark/reset. 
             * Unfortunately, not all ServletInputStream implementations are marking, so we need do 
             * this memory-expensive wrapping to make it work. It's far from ideal but I don't have 
             * a more efficient solution.
             */
            byte[] bytes = new byte[1024 * 4];
            {
                inputStream.mark(bytes.length);
                inputStream.read(bytes);
                inputStream.reset();
            }
            
            CharsetDetector detector = new CharsetDetector();
            detector.setDeclaredEncoding("utf8"); // most of the content on the web is encoded in UTF-8 so start with that
            
            Reader reader = null;
            CharsetMatch[] charsetMatches = detector.setText(bytes).detectAll();
            for (CharsetMatch charsetMatch : charsetMatches) {
                try {
                    reader = new InputStreamReader(inputStream, charsetMatch.getName());
                    
                    options.setProperty("encoding", charsetMatch.getName());
                    options.setProperty("encoding_confidence", Integer.toString(charsetMatch.getConfidence()));
                    
                    Gridworks.log(
                        "Best encoding guess: " + 
                        charsetMatch.getName() + 
                        " [confidence: " + charsetMatch.getConfidence() + "]");
                    
                    break;
                } catch (UnsupportedEncodingException e) {
                    // silent
                }
            }
            
            if (reader == null) { // when all else fails
                reader = encoding != null ?
                        new InputStreamReader(inputStream, encoding) :
                        new InputStreamReader(inputStream);
            }
            
            try {
                importer.read(reader, project, options, skip, limit);
            } finally {
                reader.close();
            }
        } else {
            try {
                importer.read(inputStream, project, options, skip, limit);
            } finally {
                inputStream.close();
            }
        }        
    }
    
    protected void internalInvokeImporter(
        Project     project,
        Importer    importer,
        Properties  options,
        Reader      reader
    ) throws Exception {
        
        int limit = -1;
        int skip = 0;
        
        if (options.containsKey("limit")) {
            String s = options.getProperty("limit");
            try {
                limit = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        if (options.containsKey("skip")) {
            String s = options.getProperty("skip");
            try {
                skip = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        
        importer.read(reader, project, options, skip, limit);
    }
    
    protected Importer guessImporter(
            Properties options, String contentType, String fileName) {
        
        if (contentType != null) {
            contentType = contentType.toLowerCase().trim();
            
            if ("application/msexcel".equals(contentType) ||
                "application/x-msexcel".equals(contentType) ||
                "application/x-ms-excel".equals(contentType) ||
                "application/vnd.ms-excel".equals(contentType) ||
                "application/x-excel".equals(contentType) ||
                "application/xls".equals(contentType)) {
                
                return new ExcelImporter(false);
            } else if("application/x-xls".equals(contentType)) {
                return new ExcelImporter(true); 
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".xls")) {
                return new ExcelImporter(false); 
            } else if (fileName.endsWith(".xlsx")) {
                return new ExcelImporter(true); 
            } else if (
                    fileName.endsWith(".xml") ||
                    fileName.endsWith(".rss")
                ) {
                return new XmlImporter(); 
            }
        }
        
        return new TsvCsvImporter();
    }
}
