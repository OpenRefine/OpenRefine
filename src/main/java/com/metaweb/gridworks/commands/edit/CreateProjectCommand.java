package com.metaweb.gridworks.commands.edit;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

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
                    
                    FilePart filePart = (FilePart) part;
                    InputStream stream = filePart.getInputStream();
                    String name = filePart.getFileName().toLowerCase();
                    try {
                        internalImportFile(project, options, name, stream);
                    } finally {
                        stream.close();
                    }
                    
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
        
    static class SafeInputStream extends FilterInputStream {
        public SafeInputStream(InputStream stream) {
            super(stream);
        }
               
        @Override
        public void close() {
            // some libraries attempt to close the input stream while they can't 
            // read anymore from it... unfortunately this behavior prevents 
            // the zip input stream from functioning correctly so we just have
            // to ignore those close() calls and just close it ourselves
            // forcefully later
        }
        
        public void reallyClose() throws IOException {
            super.close();
        }
    }
    
    protected void internalImportFile(
        Project     project,
        Properties  options,
        String    fileName,
        InputStream inputStream
    ) throws Exception {

        Gridworks.info("Importing " + fileName + "");
        
        if (fileName.endsWith(".zip") || fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar.bz2")) {

            // first, save the file on disk, since we need two passes and we might 
            // not have enough memory to keep it all in there
            File file = save(inputStream);
            
            // in the first pass, gather statistics about what files are in there
            // unfortunately, we have to rely on files extensions, which is horrible but
            // better than nothing
            HashMap<String,Integer> ext_map = new HashMap<String,Integer>();

            InputStream is = getStream(fileName, new FileInputStream(file));
            
            // NOTE(SM): unfortunately, java.io does not provide any generalized class for 
            // archive-like input streams so while both TarInputStream and ZipInputStream 
            // behave precisely the same, there is no polymorphic behavior so we have
            // to treat each instance explicitly... one of those times you wish you had
            // closures
            try {
                if (is instanceof TarInputStream) {
                    TarInputStream tis = (TarInputStream) is;
                    TarEntry te;
                    while ((te = tis.getNextEntry()) != null) {
                        if (!te.isDirectory()) {
                            mapExtension(te.getName(),ext_map);
                        }
                    }
                } else if (is instanceof ZipInputStream) {
                    ZipInputStream zis = (ZipInputStream) is;
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (!ze.isDirectory()) {
                            mapExtension(ze.getName(),ext_map);
                        }
                    }
                }
            } finally {
                try {
                    is.close();
                } catch (IOException e) {}
            }

            // sort extensions by how often they appear
            List<Entry<String,Integer>> values = new ArrayList<Entry<String,Integer>>(ext_map.entrySet());
            Collections.sort(values, new ValuesComparator());

            if (values.size() == 0) {
                throw new RuntimeException("The archive contains no files.");
            }
            
            // this will contain the set of extensions we'll load from the archive
            HashSet<String> exts = new HashSet<String>();
            
            // find the extension that is most frequent or those who share the highest frequency value
            if (values.size() == 1) {
                exts.add(values.get(0).getKey());
            } else {
                Entry<String,Integer> most_frequent = values.get(0);
                Entry<String,Integer> second_most_frequent = values.get(1);
                if (most_frequent.getValue() > second_most_frequent.getValue()) { // we have a winner
                    exts.add(most_frequent.getKey());
                } else { // multiple extensions have the same frequency
                    int winning_frequency = most_frequent.getValue();
                    for (Entry<String,Integer> e : values) {
                        if (e.getValue() == winning_frequency) {
                            exts.add(e.getKey());
                        }
                    }
                }
            }
            Gridworks.log("Most frequent extensions: " + exts.toString());

            // second pass, load the data for real
            is = getStream(fileName, new FileInputStream(file));
            SafeInputStream sis = new SafeInputStream(is);
            try {
                if (is instanceof TarInputStream) {
                    TarInputStream tis = (TarInputStream) is;
                    TarEntry te;
                    while ((te = tis.getNextEntry()) != null) {
                        if (!te.isDirectory()) {
                            String name = te.getName();
                            String ext = getExtension(name)[1];
                            if (exts.contains(ext)) {
                                internalImportFile(project, options, name, sis);
                            }
                        }
                    }
                } else if (is instanceof ZipInputStream) {
                    ZipInputStream zis = (ZipInputStream) is;
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (!ze.isDirectory()) {
                            String name = ze.getName();
                            String ext = getExtension(name)[1];
                            if (exts.contains(ext)) {
                                internalImportFile(project, options, name, sis);
                            }
                        }
                    }
                }
            } finally {
                try {
                    sis.reallyClose();
                } catch (IOException e) {}
            }

        } else if (fileName.endsWith(".gz")) {
            internalImportFile(project, options, getExtension(fileName)[0], new GZIPInputStream(inputStream));
        } else if (fileName.endsWith(".bz2")) {
            internalImportFile(project, options, getExtension(fileName)[0], new CBZip2InputStream(inputStream));
        } else {
            load(project, options, fileName, inputStream);
        }
    }

    public static class ValuesComparator implements Comparator<Entry<String,Integer>>, Serializable {
        private static final long serialVersionUID = 8845863616149837657L;

        public int compare(Entry<String,Integer> o1, Entry<String,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }
    
    private void load(Project project, Properties options, String fileName, InputStream inputStream) throws Exception {
        Importer importer = guessImporter(options, null, fileName);
        internalInvokeImporter(project, importer, options, inputStream, null);
    }

    private File save(InputStream is) throws IOException {
        File temp = Gridworks.getTempFile(Long.toString(System.currentTimeMillis()));
        temp.deleteOnExit();
        copy(is,temp);
        is.close();
        return temp;
    }
    
    private void mapExtension(String name, Map<String,Integer> ext_map) {
        String ext = getExtension(name)[1];
        if (ext_map.containsKey(ext)) {
            ext_map.put(ext, ext_map.get(ext) + 1);
        } else {
            ext_map.put(ext, 1);
        }
    }

    private InputStream getStream(String fileName, InputStream is) throws IOException {
        if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) { 
            return new TarInputStream(new GZIPInputStream(is));
        } else if (fileName.endsWith(".tar.bz2")) {
            return new TarInputStream(new CBZip2InputStream(is));
        } else {
            return new ZipInputStream(is);
        }
    }
    
    private String[] getExtension(String filename) {
        String[] result = new String[2];
        int ext_index = filename.lastIndexOf(".");
        result[0] = (ext_index == -1) ? filename : filename.substring(0,ext_index);
        result[1] = (ext_index == -1) ? "" : filename.substring(ext_index + 1);
        return result;
    }
    
    private static long copy(InputStream input, File file) throws IOException {
        FileOutputStream output = new FileOutputStream(file);
        byte[] buffer = new byte[4 * 1024];
        long count = 0;
        int n = 0;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
        } finally {
            try {
                output.close();
            } catch (IOException e) {}
            try {
                input.close();
            } catch (IOException e) {}
        }
        return count;
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
        
        int limit = getIntegerOption("limit",options,-1);
        int skip = getIntegerOption("skip",options,0);
                
        if (importer.takesReader()) {

            BufferedInputStream inputStream = new BufferedInputStream(rawInputStream);
            
            // NOTE(SM): The ICU4J char detection code requires the input stream to support mark/reset. 
            // Unfortunately, not all ServletInputStream implementations are marking, so we need do 
            // this memory-expensive wrapping to make it work. It's far from ideal but I don't have 
            // a more efficient solution.
            byte[] bytes = new byte[1024 * 4];
            inputStream.mark(bytes.length);
            inputStream.read(bytes);
            inputStream.reset();
            
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
            
            importer.read(reader, project, options, skip, limit);
        } else {
            importer.read(rawInputStream, project, options, skip, limit);
        }        
    }
    
    protected void internalInvokeImporter(
        Project     project,
        Importer    importer,
        Properties  options,
        Reader      reader
    ) throws Exception {
        
        int limit = getIntegerOption("limit",options,-1);
        int skip = getIntegerOption("skip",options,0);
        
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
            } else if("application/xml".equals(contentType) ||
                      "text/xml".equals(contentType) ||
                      "application/rss+xml".equals(contentType) ||
                      "application/atom+xml".equals(contentType) ||
                      "application/rdf+xml".equals(contentType)) {
                return new XmlImporter();
            }
        } else if (fileName != null) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".xls")) {
                return new ExcelImporter(false); 
            } else if (fileName.endsWith(".xlsx")) {
                return new ExcelImporter(true); 
            } else if (
                    fileName.endsWith(".xml") ||
                    fileName.endsWith(".rdf") ||
                    fileName.endsWith(".atom") ||
                    fileName.endsWith(".rss")
                ) {
                return new XmlImporter(); 
            }
        }
        
        return new TsvCsvImporter();
    }
    
    private int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return value;
    }
}
