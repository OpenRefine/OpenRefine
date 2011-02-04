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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.commands.Command;
import com.google.refine.importers.Importer;
import com.google.refine.importers.ImporterRegistry;
import com.google.refine.importers.ReaderImporter;
import com.google.refine.importers.StreamImporter;
import com.google.refine.importers.TsvCsvImporter;
import com.google.refine.importers.UrlImporter;
import com.google.refine.model.Project;
import com.google.refine.util.IOUtils;
import com.google.refine.util.ParsingUtilities;
import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public class CreateProjectCommand extends Command {

    final static Logger logger = LoggerFactory.getLogger("create-project_command");

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProjectManager.singleton.setBusy(true);
        try {

            /* 
             * Set UTF-8 as request encoding, then ServletFileUpload will use it as default encoding
             */
            if (request.getCharacterEncoding() == null) {
                request.setCharacterEncoding("UTF-8");
            }
            
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

            internalImport(request, project, pm, options);

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

            redirect(response, "/project?project=" + project.id);
        } catch (Exception e) {
            respondWithErrorPage(request, response, "Failed to import file", e);
        } finally {
            ProjectManager.singleton.setBusy(false);
        }
    }

    protected void internalImport(
        HttpServletRequest    request,
        Project               project,
        ProjectMetadata       metadata,
        Properties            options
    ) throws Exception {

        ServletFileUpload upload = new ServletFileUpload();
        String url = options.getProperty("url");
        boolean imported = false;

        FileItemIterator iter = upload.getItemIterator(request);
        while (iter.hasNext()) {
            FileItemStream item = iter.next();
            String name = item.getFieldName().toLowerCase();
            InputStream stream = item.openStream();
            if (item.isFormField()) {
                if (name.equals("raw-text")) {
                    Reader reader = new InputStreamReader(stream,request.getCharacterEncoding());
                    try {
                        internalInvokeImporter(project, new TsvCsvImporter(), metadata, options, reader);
                        imported = true;
                    } finally {
                        reader.close();
                    }
                } else if (name.equals("project-url")) {
                    url = Streams.asString(stream, request.getCharacterEncoding());
                } else {
                    options.put(name, Streams.asString(stream, request.getCharacterEncoding()));
                }
            } else {
                String fileName = item.getName().toLowerCase();
                if (fileName.length() > 0) {
                    try {
                        internalImportFile(project, metadata, options, fileName, stream);
                        imported = true;
                    } finally {
                        stream.close();
                    }
                }
            }
        }

        if (!imported && url != null && url.length() > 0) {
            internalImportURL(request, project, metadata, options, url);
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
        Project         project,
        ProjectMetadata metadata,
        Properties      options,
        String          fileName,
        InputStream     inputStream
    ) throws Exception {

        logger.info("Importing '{}'", fileName);

        if (fileName.endsWith(".zip") || fileName.endsWith(".tar") || fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar.bz2")) {

            // first, save the file on disk, since we need two passes and we might
            // not have enough memory to keep it all in there
            File file = save(inputStream);

            // in the first pass, gather statistics about what files are in there
            // unfortunately, we have to rely on files extensions, which is horrible but
            // better than nothing
            HashMap<String,Integer> ext_map = new HashMap<String,Integer>();

            FileInputStream fis = new FileInputStream(file);
            InputStream is = getStream(fileName, fis);

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
                    fis.close();
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

            logger.info("Most frequent extensions: {}", exts.toString());

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
                                internalImportFile(project, metadata, options, name, sis);
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
                                internalImportFile(project, metadata, options, name, sis);
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
            internalImportFile(project, metadata, options, getExtension(fileName)[0], new GZIPInputStream(inputStream));
        } else if (fileName.endsWith(".bz2")) {
            internalImportFile(project, metadata, options, getExtension(fileName)[0], new CBZip2InputStream(inputStream));
        } else {
            load(project, metadata, options, fileName, inputStream);
        }
    }

    public static class ValuesComparator implements Comparator<Entry<String,Integer>>, Serializable {
        private static final long serialVersionUID = 8845863616149837657L;

        public int compare(Entry<String,Integer> o1, Entry<String,Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }

    private void load(Project project, ProjectMetadata metadata, Properties options, String fileName, InputStream inputStream) throws Exception {
        Importer importer = ImporterRegistry.guessImporter(null, fileName);
        internalInvokeImporter(project, importer, metadata, options, inputStream, null);
    }

    private File save(InputStream is) throws IOException {
        File temp = this.servlet.getTempFile(Long.toString(System.currentTimeMillis()));
        temp.deleteOnExit();
        IOUtils.copy(is,temp);
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
        } else if (fileName.endsWith(".tar")) {
            return new TarInputStream(is);
        } else {
            return new ZipInputStream(is);
        }
    }

    private String[] getExtension(String filename) {
        String[] result = new String[2];
        int ext_index = filename.lastIndexOf('.');
        result[0] = (ext_index == -1) ? filename : filename.substring(0,ext_index);
        result[1] = (ext_index == -1) ? "" : filename.substring(ext_index + 1);
        return result;
    }

    protected void internalImportURL(
        HttpServletRequest request,
        Project project,
        ProjectMetadata metadata,
        Properties options,
        String urlString) throws Exception {
        
        URL url = new URL(urlString);
        URLConnection connection = null;

        // Try for a URL importer first
        Importer importer = ImporterRegistry.guessUrlImporter(url);
        if (importer instanceof UrlImporter) {
            ((UrlImporter) importer).read(url, project, metadata, options);
        } else {
            // If we couldn't find one, try opening URL and treating as a stream
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
                String contentType = connection.getContentType();
                int semicolon = contentType.indexOf(';');
                if (semicolon >= 0) {
                    contentType = contentType.substring(0, semicolon);
                }
                
                importer = ImporterRegistry.guessImporter(contentType, url.getPath());
                
                internalInvokeImporter(project, importer, metadata, options, inputStream, connection.getContentEncoding());
            } finally {
                inputStream.close();
            }
        }
    }

    protected void internalInvokeImporter(
        Project         project,
        Importer        importer,
        ProjectMetadata metadata,
        Properties      options,
        InputStream     rawInputStream,
        String          encoding
    ) throws Exception {
        if (importer instanceof ReaderImporter) {

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
            options.setProperty("encoding_confidence", "0"); // in case we don't find anything suitable

            InputStreamReader reader = null;
            CharsetMatch[] charsetMatches = detector.setText(bytes).detectAll();
            if (charsetMatches.length > 0) {
                CharsetMatch charsetMatch = charsetMatches[0]; // matches are ordered - first is best match
                int confidence = charsetMatch.getConfidence();
                // Threshold was 50.  Do we ever want to not use our best guess even if it's low confidence? - tfmorris
                if (confidence >= 20) {
                    try {
                        reader = new InputStreamReader(inputStream, charsetMatch.getName());
                    } catch (UnsupportedEncodingException e) {
                        // ignored - we'll fall back to a different reader later
                    }
                    // Encoding will be set later at common exit point
                    options.setProperty("encoding_confidence", Integer.toString(confidence));    
                    logger.info("Best encoding guess: {} [confidence: {}]", charsetMatch.getName(), charsetMatch.getConfidence());
                } else {
                    logger.debug("Poor encoding guess: {} [confidence: {}]", charsetMatch.getName(), charsetMatch.getConfidence());
                }
            }

            if (reader == null) { // when all else fails
                if (encoding != null) {
                    reader = new InputStreamReader(inputStream, encoding);
                } else {
                    reader = new InputStreamReader(inputStream);
                }
            }
            // Get the actual encoding which will be used and save it for project metadata
            options.setProperty("encoding", reader.getEncoding());

            ((ReaderImporter) importer).read(reader, project, metadata, options);
        } else {
            // TODO: How do we set character encoding here?
            // Things won't work right if it's not set, so pick some arbitrary values
            if (encoding != null) {
                options.setProperty("encoding", encoding);
            }
            options.setProperty("encoding_confidence", "0");
            ((StreamImporter) importer).read(rawInputStream, project, metadata, options);
        }
    }

    protected void internalInvokeImporter(
        Project         project,
        ReaderImporter  importer,
        ProjectMetadata metadata,
        Properties      options,
        Reader          reader
    ) throws Exception {
        importer.read(reader, project, metadata, options);
    }

}
