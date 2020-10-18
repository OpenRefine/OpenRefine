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

package org.openrefine.importing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.hadoop.fs.Path;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.openrefine.ProjectManager;
import org.openrefine.ProjectMetadata;
import org.openrefine.RefineServlet;
import org.openrefine.importers.ImporterUtilities;
import org.openrefine.importing.ImportingJob.ImportingJobConfig;
import org.openrefine.importing.ImportingJob.RetrievalRecord;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.model.changes.ChangeDataStore;
import org.openrefine.model.changes.LazyChangeDataStore;
import org.openrefine.util.JSONUtilities;
import org.openrefine.util.ParsingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ImportingUtilities {
    final static protected Logger logger = LoggerFactory.getLogger("importing-utilities");
    
    static public interface Progress {
        public void setProgress(String message, int percent);
        public boolean isCanceled();
    }
    
    static public void loadDataAndPrepareJob(
        HttpServletRequest request,
        HttpServletResponse response,
        Properties parameters,
        final ImportingJob job) throws IOException, ServletException {
        
    	ImportingJobConfig config = job.getJsonConfig();
        RetrievalRecord retrievalRecord = new RetrievalRecord();
        config.retrievalRecord =  retrievalRecord;
        config.state = "loading-raw-data";
        
        final ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
        config.progress = progress;
        try {
            ImportingUtilities.retrieveContentFromPostRequest(
                request,
                parameters,
                job.getRawDataDir(),
                retrievalRecord,
                new Progress() {
                    @Override
                    public void setProgress(String message, int percent) {
                        if (message != null) {
                            JSONUtilities.safePut(progress, "message", message);
                        }
                        JSONUtilities.safePut(progress, "percent", percent);
                    }
                    @Override
                    public boolean isCanceled() {
                        return job.canceled;
                    }
                }
            );
        } catch (Exception e) {
            config.state = "error";
            config.error = "Error uploading data";
            config.errorDetails = e.getLocalizedMessage();
            return;
        }

        config.fileSelection = new ArrayList<>();
        
        String bestFormat = job.autoSelectFiles();
        bestFormat = job.guessBetterFormat(bestFormat);
        
        job.rerankFormats(bestFormat);
        
        config.state = "ready";
        config.hasData = true;
        config.progress = null;
    }
    
    static public void retrieveContentFromPostRequest(
        HttpServletRequest request,
        Properties parameters,
        File rawDataDir,
        RetrievalRecord retrievalRecord,
        final Progress progress
    ) throws Exception {
        List<ImportingFileRecord> fileRecords = new ArrayList<>();
        retrievalRecord.files = fileRecords;
        
        int clipboardCount = 0;
        int uploadCount = 0;
        int downloadCount = 0;
        int archiveCount = 0;
        int sparkCount = 0;
        
        // This tracks the total progress, which involves uploading data from the client
        // as well as downloading data from URLs.
        final SavingUpdate update = new SavingUpdate() {
            @Override
            public void savedMore() {
                progress.setProgress(null, calculateProgressPercent(totalExpectedSize, totalRetrievedSize));
            }
            @Override
            public boolean isCanceled() {
                return progress.isCanceled();
            }
        };
        
        DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
        
        ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
        upload.setProgressListener(new ProgressListener() {
            boolean setContentLength = false;
            long lastBytesRead = 0;
            
            @Override
            public void update(long bytesRead, long contentLength, int itemCount) {
                if (!setContentLength) {
                    // Only try to set the content length if we really know it.
                    if (contentLength >= 0) {
                        update.totalExpectedSize += contentLength;
                        setContentLength = true;
                    }
                }
                if (setContentLength) {
                    update.totalRetrievedSize += (bytesRead - lastBytesRead);
                    lastBytesRead = bytesRead;
                    
                    update.savedMore();
                }
            }
        });

        List<FileItem> tempFiles = upload.parseRequest(request);
        
        progress.setProgress("Uploading data ...", -1);
        for (FileItem fileItem : tempFiles) {
            if (progress.isCanceled()) {
                break;
            }
            
            InputStream stream = fileItem.getInputStream();
            
            String name = fileItem.getFieldName().toLowerCase();
            if (fileItem.isFormField()) {
                if ("clipboard".equals(name)) {
                    String encoding = request.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                    
                    File file = allocateFile(rawDataDir, "clipboard.txt");
                    
                    progress.setProgress("Uploading pasted clipboard text",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
                    
                    ImportingFileRecord fileRecord = new ImportingFileRecord(
                    		null, // sparkURI
                			getRelativePath(file, rawDataDir), // location
                			"(clipboard)", // fileName
                			saveStreamToFile(stream, file, null), // size
                			"clipboard", // origin
                			null, // declaredMimeType
                			null, // mimeType
                			null, // URL
                			null, // encoding
                			encoding, // declaredEncoding
                			"text", // format
                			null // archiveFileName
                			);
                    fileRecords.add(fileRecord);
                    
                    clipboardCount++;
                    
                } else if ("spark".equals(name)) {
                	String urlString = Streams.asString(stream);
                	String filename = extractFilenameFromSparkURI(urlString);
                	
                	ImportingFileRecord fileRecord = new ImportingFileRecord(
                    		urlString, // sparkURI
                			null, // location
                			filename, // fileName
                			0, // size
                			"spark", // origin
                			null, // declaredMimeType
                			null, // mimeType
                			null, // URL
                			null, // encoding
                			null, // declaredEncoding
                			null, // format
                			null // archiveFileName
                			);
                	fileRecords.add(fileRecord);
                	
                	sparkCount++;
                } else if ("download".equals(name)) {
                    String urlString = Streams.asString(stream);
                    URL url = new URL(urlString);
                    
                    ImportingFileRecord fileRecord = new ImportingFileRecord(
                    		null, // sparkURI
                    		null, // location
                    		null, // fileName
                    		0, // size
                    		"download", // origin
                    		null, // declaredMimeType
                    		null, // mimeType
                    		urlString, // URL
                    		null, // encoding
                    		null, // declaredEncoding
                    		null, // format
                    		null // archiveFileName
                    		);

                    if ("http".equals(url.getProtocol()) || "https".equals(url.getProtocol())) {
                        DefaultHttpClient client = new DefaultHttpClient();
                        DecompressingHttpClient httpclient = 
                                new DecompressingHttpClient(client);
                        HttpGet httpGet = new HttpGet(url.toURI());
                        httpGet.setHeader("User-Agent", RefineServlet.getUserAgent());
                        if ("https".equals(url.getProtocol())) {
                            // HTTPS only - no sending password in the clear over HTTP
                            String userinfo = url.getUserInfo();
                            if (userinfo != null) {
                                int s = userinfo.indexOf(':');
                                if (s > 0) {
                                    String user = userinfo.substring(0, s);
                                    String pw = userinfo.substring(s + 1, userinfo.length());
                                    client.getCredentialsProvider().setCredentials(
                                            new AuthScope(url.getHost(), 443),
                                            new UsernamePasswordCredentials(user, pw));
                                }
                            }
                        }
 
                        HttpResponse response = httpclient.execute(httpGet);
                        
                        try {
                            response.getStatusLine();
                            HttpEntity entity = response.getEntity();
                            if (entity == null) {
                                throw new Exception("No content found in " + url.toString());
                            }
                            InputStream stream2 = entity.getContent();
                            String encoding = null;
                            if (entity.getContentEncoding() != null) {
                                encoding = entity.getContentEncoding().getValue();
                            }
                            fileRecord.setDeclaredEncoding(encoding);
                            String contentType = null;
                            if (entity.getContentType() != null) {
                                contentType = entity.getContentType().getValue();
                            }
                            fileRecord.setDeclaredMimeType(contentType);
                            if (saveStream(stream2, url, rawDataDir, progress, update, 
                                    fileRecord, fileRecords,
                                    entity.getContentLength())) {
                                archiveCount++;
                            }
                            downloadCount++;
                            EntityUtils.consume(entity);
                        } finally {
                            httpGet.releaseConnection();
                        }
                    } else {
                        // Fallback handling for non HTTP connections (only FTP?)
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.connect();
                        InputStream stream2 = urlConnection.getInputStream();
                        fileRecord.setDeclaredEncoding( 
                                urlConnection.getContentEncoding());
                        fileRecord.setDeclaredMimeType(
                                urlConnection.getContentType());
                        try {
                            if (saveStream(stream2, url, rawDataDir, progress, 
                                    update, fileRecord, fileRecords,
                                    urlConnection.getContentLength())) {
                                archiveCount++;
                            }
                            downloadCount++;
                        } finally {
                            stream2.close();
                        }
                    }
                } else {
                    String value = Streams.asString(stream);
                    parameters.put(name, value);
                    // TODO: We really want to store this on the request so it's available for everyone
//                    request.getParameterMap().put(name, value);
                    
                }
            } else { // is file content
                String fileName = fileItem.getName();
                if (fileName.length() > 0) {
                    long fileSize = fileItem.getSize();
                    
                    File file = allocateFile(rawDataDir, fileName);
                    
                    progress.setProgress(
                            "Saving file " + fileName + " locally (" + formatBytes(fileSize) + " bytes)",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
                    
                    ImportingFileRecord fileRecord = new ImportingFileRecord(
                    		null, // sparkURI
                    		getRelativePath(file, rawDataDir), // location
                    		fileName, // fileName
                    		saveStreamToFile(stream, file, null), // size
                    		"upload", // origin
                    		fileItem.getContentType(), // declaredMimeType
                    		null, // mimeType
                    		null, // URL
                    		null, // encoding
                    		request.getCharacterEncoding(), 
                    		null, // format
                    		null // archiveFileName
                    		);

                    if (postProcessRetrievedFile(rawDataDir, file, fileRecord, fileRecords, progress)) {
                        archiveCount++;
                    }
                    
                    uploadCount++;
                }
            }
            
            stream.close();
        }
        
        // Delete all temp files.
        for (FileItem fileItem : tempFiles) {
            fileItem.delete();
        }
        
        retrievalRecord.uploadCount = uploadCount;
        retrievalRecord.downloadCount = downloadCount;
        retrievalRecord.clipboardCount = clipboardCount;
        retrievalRecord.archiveCount = archiveCount;
        retrievalRecord.sparkCount = sparkCount;
    }
    
    /**
     * Attempts to extract a filename from a Spark URI.
     * 
     * @param uri the spark URI
     * @return null if it does not succeed, or if there is no final name in the path
     */
    protected static String extractFilenameFromSparkURI(String uri) {
		Path url = new Path(uri);
		if (url.getName().length() > 0) {
			return url.getName();
		} else {
			return null;
		}
	}

	private static boolean saveStream(InputStream stream, URL url, File rawDataDir, final Progress progress,
            final SavingUpdate update, ImportingFileRecord fileRecord, List<ImportingFileRecord> fileRecords, long length)
            throws IOException, Exception {
        String localname = url.getPath();
        if (localname.isEmpty() || localname.endsWith("/")) {
            localname = localname + "temp";
        }
        File file = allocateFile(rawDataDir, localname);

        fileRecord.setFileName(file.getName());
        fileRecord.setLocation(getRelativePath(file, rawDataDir));

        update.totalExpectedSize += length;

        progress.setProgress("Downloading " + url.toString(),
            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
        
        long actualLength = saveStreamToFile(stream, file, update);
        fileRecord.setSize(actualLength);
        if (actualLength == 0) {
            throw new Exception("No content found in " + url.toString());
        } else if (length >= 0) {
            update.totalExpectedSize += (actualLength - length);
        } else {
            update.totalExpectedSize += actualLength;
        }
        progress.setProgress("Saving " + url.toString() + " locally",
            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
        return postProcessRetrievedFile(rawDataDir, file, fileRecord, fileRecords, progress);
    }
    
    static public String getRelativePath(File file, File dir) {
        String location = file.getAbsolutePath().substring(dir.getAbsolutePath().length());
        return (location.startsWith(File.separator)) ? location.substring(1) : location;
    }
    
    static public File allocateFile(File dir, String name) {
        int q = name.indexOf('?');
        if (q > 0) {
            name = name.substring(0, q);
        }
        
        File file = new File(dir, name);     
        // For CVE-2018-19859, issue #1840
        if (!file.toPath().normalize().startsWith(dir.toPath().normalize())) {
        	throw new IllegalArgumentException("Zip archives with files escaping their root directory are not allowed.");
        }
        
        int dot = name.indexOf('.');
        String prefix = dot < 0 ? name : name.substring(0, dot);
        String suffix = dot < 0 ? "" : name.substring(dot);
        int index = 2;
        while (file.exists()) {
            file = new File(dir, prefix + "-" + index++ + suffix);
        }
        
        file.getParentFile().mkdirs();
        
        return file;
    }
    
    static public Reader getFileReader(ImportingJob job, ImportingFileRecord fileRecord, String commonEncoding)
        throws FileNotFoundException {
        
        return getFileReader(new File(job.getRawDataDir(), fileRecord.getLocation()), fileRecord, commonEncoding);
    }
    
    static public Reader getFileReader(File file, ImportingFileRecord fileRecord, String commonEncoding) throws FileNotFoundException {
        return ImporterUtilities.getReaderFromStream(new FileInputStream(file), fileRecord, commonEncoding);
    }
    
    static private abstract class SavingUpdate {
        public long totalExpectedSize = 0;
        public long totalRetrievedSize = 0;
        
        abstract public void savedMore();
        abstract public boolean isCanceled();
    }
    static public long saveStreamToFile(InputStream stream, File file, SavingUpdate update) throws IOException {
        long length = 0;
        FileOutputStream fos = new FileOutputStream(file);
        try {
            byte[] bytes = new byte[16*1024];
            int c;
            while ((update == null || !update.isCanceled()) && (c = stream.read(bytes)) > 0) {
                fos.write(bytes, 0, c);
                length += c;

                if (update != null) {
                    update.totalRetrievedSize += c;
                    update.savedMore();
                }
            }
            return length;
        } finally {
            fos.close();
        }
    }
    
    static public boolean postProcessRetrievedFile(
            File rawDataDir, File file, ImportingFileRecord fileRecord, List<ImportingFileRecord> fileRecords, final Progress progress) {
        
        String mimeType = fileRecord.getDeclaredMimeType();
        String contentEncoding = fileRecord.getDeclaredEncoding();
        
        InputStream archiveIS = tryOpenAsArchive(file, mimeType, contentEncoding);
        if (archiveIS != null) {
            try {
                if (explodeArchive(rawDataDir, archiveIS, fileRecord, fileRecords, progress)) {
                    file.delete();
                    return true;
                }
            } finally {
                try {
                    archiveIS.close();
                } catch (IOException e) {
                    // TODO: what to do?
                }
            }
        }
        
        InputStream uncompressedIS = tryOpenAsCompressedFile(file, mimeType, contentEncoding);
        if (uncompressedIS != null) {
            try {
                File file2 = uncompressFile(rawDataDir, uncompressedIS, fileRecord, progress);
                
                file.delete();
                file = file2;
            } catch (IOException e) {
                // TODO: what to do?
                e.printStackTrace();
            } finally {
                try {
                    uncompressedIS.close();
                } catch (IOException e) {
                    // TODO: what to do?
                }
            }
        }
        
        postProcessSingleRetrievedFile(file, fileRecord);
        fileRecords.add(fileRecord);
        
        return false;
    }
    
    static public void postProcessSingleRetrievedFile(File file, ImportingFileRecord fileRecord) {
        if (fileRecord.getFormat() == null) {
            fileRecord.setFormat(
            		FormatRegistry.getFormat(
                    file.getName(),
                    fileRecord.getDeclaredMimeType()));
        }
    }
    
    static public InputStream tryOpenAsArchive(File file, String mimeType) {
        return tryOpenAsArchive(file, mimeType, null);
    }
    
    static public InputStream tryOpenAsArchive(File file, String mimeType, String contentType) {
        String fileName = file.getName();
        try {
            if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
                return new TarInputStream(new GZIPInputStream(new FileInputStream(file)));
            } else if (fileName.endsWith(".tar.bz2")) {
                return new TarInputStream(new CBZip2InputStream(new FileInputStream(file)));
            } else if (fileName.endsWith(".tar") || "application/x-tar".equals(contentType)) {
                return new TarInputStream(new FileInputStream(file));
            } else if (fileName.endsWith(".zip") 
                    || "application/x-zip-compressed".equals(contentType)
                    || "application/zip".equals(contentType) 
                    || "application/x-compressed".equals(contentType)
                    || "multipar/x-zip".equals(contentType)) {
                return new ZipInputStream(new FileInputStream(file));
            } else if (fileName.endsWith(".kmz")) {
                return new ZipInputStream(new FileInputStream(file));
            }
        } catch (IOException e) {
        }
        return null;
    }
    
    static public boolean explodeArchive(
        File rawDataDir,
        InputStream archiveIS,
        ImportingFileRecord archiveFileRecord,
        List<ImportingFileRecord> fileRecords,
        final Progress progress
    ) {
        if (archiveIS instanceof TarInputStream) {
            TarInputStream tis = (TarInputStream) archiveIS;
            try {
                TarEntry te;
                while (!progress.isCanceled() && (te = tis.getNextEntry()) != null) {
                    if (!te.isDirectory()) {
                        String fileName2 = te.getName();
                        File file2 = allocateFile(rawDataDir, fileName2);
                        
                        progress.setProgress("Extracting " + fileName2, -1);
                        
                        ImportingFileRecord fileRecord2 = new ImportingFileRecord(
                        		null, // sparkURI
                        		getRelativePath(file2, rawDataDir), // location 
                        		fileName2, // fileName	
                    			saveStreamToFile(tis, file2, null), // size
                    			archiveFileRecord.getOrigin(), // origin
                    			null, // declaredMimeType
                    			null, // mimeType
                    			null, // url
                    			null, // encoding
                    			null, // declaredEncoding
                    			null, // format
                    			archiveFileRecord.getFileName());
                        
                        fileRecords.add(fileRecord2);
                    }
                }
            } catch (IOException e) {
                // TODO: what to do?
                e.printStackTrace();
            }
            return true;
        } else if (archiveIS instanceof ZipInputStream) {
            ZipInputStream zis = (ZipInputStream) archiveIS;
            try {
                ZipEntry ze;
                while (!progress.isCanceled() && (ze = zis.getNextEntry()) != null) {
                    if (!ze.isDirectory()) {
                        String fileName2 = ze.getName();
                        File file2 = allocateFile(rawDataDir, fileName2);
                        
                        progress.setProgress("Extracting " + fileName2, -1);
                        
                        ImportingFileRecord fileRecord2 = new ImportingFileRecord(
                        		null, // sparkURI
                        		getRelativePath(file2, rawDataDir), // location
                        		fileName2, // fileName
                        		saveStreamToFile(zis, file2, null), // size
                        		archiveFileRecord.getOrigin(), // origin
                        		null, // declaredMimeType
                        		null, // mimeType
                        		null, // url
                        		null, // encoding
                        		null, // declaredEncoding
                        		null, // format
                        		archiveFileRecord.getFileName());
                        
                        fileRecords.add(fileRecord2);
                    }
                }
            } catch (IOException e) {
                // TODO: what to do?
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
    
    static public InputStream tryOpenAsCompressedFile(File file, String mimeType) {
        return tryOpenAsCompressedFile(file, mimeType, null);
    }
    
    static public InputStream tryOpenAsCompressedFile(File file, String mimeType, String contentEncoding) {
        String fileName = file.getName();
        try {
            if (fileName.endsWith(".gz") 
                    || "gzip".equals(contentEncoding) 
                    || "x-gzip".equals(contentEncoding)
                    || "application/x-gzip".equals(mimeType)) {                
                return new GZIPInputStream(new FileInputStream(file));
            } else if (fileName.endsWith(".bz2")
                    ||"application/x-bzip2".equals(mimeType)) {
                InputStream is = new FileInputStream(file);
                is.mark(4);
                if (!(is.read() == 'B' && is.read() == 'Z')) {
                    // No BZ prefix as appended by command line tools.  Reset and hope for the best
                    is.reset();
                }
                return new CBZip2InputStream(is);
            }
        } catch (IOException e) {
            logger.warn("Something that looked like a compressed file gave an error on open: "+file,e);
        }
        return null;
    }
    
    static public File uncompressFile(
        File rawDataDir,
        InputStream uncompressedIS,
        ImportingFileRecord fileRecord,
        final Progress progress
    ) throws IOException {
        String fileName = fileRecord.getLocation();
        if (fileName.isEmpty()) {
        	fileName = "unknown";
        }
        for (String ext : new String[] {".gz",".bz2"}) {
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length()-ext.length());
                break;
            }
        }
        File file2 = allocateFile(rawDataDir, fileName);
        
        progress.setProgress("Uncompressing " + fileName, -1);
        
        saveStreamToFile(uncompressedIS, file2, null);
        
        fileRecord.setDeclaredEncoding(null);
        fileRecord.setDeclaredMimeType(null);
        fileRecord.setLocation(getRelativePath(file2, rawDataDir));
        
        return file2;
    }
    
    static private int calculateProgressPercent(long totalExpectedSize, long totalRetrievedSize) {
        return totalExpectedSize == 0 ? -1 : (int) (totalRetrievedSize * 100 / totalExpectedSize);
    }
    
    static private String formatBytes(long bytes) {
        return NumberFormat.getIntegerInstance().format(bytes);
    }
    
    static public void previewParse(ImportingJob job, String format, ObjectNode optionObj, List<Exception> exceptions) {
        ImportingFormat record = FormatRegistry.getFormatToRecord().get(format);
        if (record == null || record.parser == null) {
            // TODO: what to do?
            return;
        }
        
        try {
        	job.metadata = createProjectMetadata(optionObj);
	        GridState state = record.parser.parse(
	            job.metadata,
	            job,
	            job.getSelectedFileRecords(),
	            format,
	            100,
	            optionObj
	        );
	        // this is a preview, so we will not need to store any change data on this project
	        job.setProject(new Project(state, new LazyChangeDataStore()));
        } catch(Exception e) {
        	exceptions.add(e);
        }
    }
    
    static public long createProject(
            final ImportingJob job,
            final String format,
            final ObjectNode optionObj,
            final List<Exception> exceptions,
            boolean synchronous) {
        final ImportingFormat record = FormatRegistry.getFormatToRecord().get(format);
        if (record == null || record.parser == null) {
            // TODO: what to do?
            return -1;
        }
        
        job.setState("creating-project");
        
        long projectId = Project.generateID();
        if (synchronous) {
            createProjectSynchronously(
                job, format, optionObj, exceptions, record, projectId);
        } else {
            new Thread() {
                @Override
                public void run() {
                    createProjectSynchronously(
                        job, format, optionObj, exceptions, record, projectId);
                }
            }.start();
        }
        return projectId;
    }
    
    static private void createProjectSynchronously(
        final ImportingJob job,
        final String format,
        final ObjectNode optionObj,
        final List<Exception> exceptions,
        final ImportingFormat record,
        final long projectId
    ) {
        ProjectMetadata pm = createProjectMetadata(optionObj);
        Project newProject = null;
        try {
	        GridState state = record.parser.parse(
	            pm,
	            job,
	            job.getSelectedFileRecords(),
	            format,
	            -1,
	            optionObj
	        );
	        ChangeDataStore dataStore = ProjectManager.singleton.getChangeDataStore(projectId);
	        newProject = new Project(projectId, state, dataStore);
			job.setProject(newProject);
        } catch(Exception e) {
        	exceptions.add(e);
        }
        
        if (!job.canceled) {
            if (exceptions.size() == 0) {
                
                ProjectManager.singleton.registerProject(newProject, pm);
                
                job.setProjectID(newProject.getId());
                job.setState("created-project");
            } else {
                job.setError(exceptions);
            }
            job.touch();
            job.updating = false;
        }
    }

    static public ProjectMetadata createProjectMetadata(ObjectNode optionObj) {
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
        pm.setTags(JSONUtilities.getStringArray(optionObj, "projectTags"));

        String encoding = JSONUtilities.getString(optionObj, "encoding", "UTF-8");
        if ("".equals(encoding)) {
            // encoding can be present, but empty, which won't trigger JSONUtilities default processing
            encoding = "UTF-8";
        }
        pm.setEncoding(encoding);
        return pm;
    }

}
