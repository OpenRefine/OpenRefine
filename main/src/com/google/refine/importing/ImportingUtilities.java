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

package com.google.refine.importing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.importing.ImportingManager.Format;
import com.google.refine.importing.UrlRewriter.Result;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

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
        final ImportingJob job,
        JSONObject config) throws IOException, ServletException {
        
        JSONObject retrievalRecord = new JSONObject();
        JSONUtilities.safePut(config, "retrievalRecord", retrievalRecord);
        JSONUtilities.safePut(config, "state", "loading-raw-data");
        
        final JSONObject progress = new JSONObject();
        JSONUtilities.safePut(config, "progress", progress);
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
            JSONUtilities.safePut(config, "state", "error");
            JSONUtilities.safePut(config, "error", "Error uploading data");
            JSONUtilities.safePut(config, "errorDetails", e.getLocalizedMessage());
            return;
        }
        
        JSONArray fileSelectionIndexes = new JSONArray();
        JSONUtilities.safePut(config, "fileSelection", fileSelectionIndexes);
        
        String bestFormat = ImportingUtilities.autoSelectFiles(job, retrievalRecord, fileSelectionIndexes);
        bestFormat = ImportingUtilities.guessBetterFormat(job, bestFormat);
        
        JSONArray rankedFormats = new JSONArray();
        ImportingUtilities.rankFormats(job, bestFormat, rankedFormats);
        JSONUtilities.safePut(config, "rankedFormats", rankedFormats);
        
        JSONUtilities.safePut(config, "state", "ready");
        JSONUtilities.safePut(config, "hasData", true);
        config.remove("progress");
    }
    
    static public void updateJobWithNewFileSelection(ImportingJob job, JSONArray fileSelectionArray) {
        job.setFileSelection(fileSelectionArray);
        
        String bestFormat = ImportingUtilities.getCommonFormatForSelectedFiles(job, fileSelectionArray);
        bestFormat = ImportingUtilities.guessBetterFormat(job, bestFormat);
        
        JSONArray rankedFormats = new JSONArray();
        ImportingUtilities.rankFormats(job, bestFormat, rankedFormats);
        job.setRankedFormats(rankedFormats);
    }
    
    static public void retrieveContentFromPostRequest(
        HttpServletRequest request,
        Properties parameters,
        File rawDataDir,
        JSONObject retrievalRecord,
        final Progress progress
    ) throws Exception {
        JSONArray fileRecords = new JSONArray();
        JSONUtilities.safePut(retrievalRecord, "files", fileRecords);
        
        int clipboardCount = 0;
        int uploadCount = 0;
        int downloadCount = 0;
        int archiveCount = 0;
        
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

        @SuppressWarnings("unchecked")
        List<FileItem> tempFiles = (List<FileItem>)upload.parseRequest(request);
        
        progress.setProgress("Uploading data ...", -1);
        parts: for (FileItem fileItem : tempFiles) {
            if (progress.isCanceled()) {
                break;
            }
            
            InputStream stream = fileItem.getInputStream();
            
            String name = fileItem.getFieldName().toLowerCase();
            if (fileItem.isFormField()) {
                if (name.equals("clipboard")) {
                    String encoding = request.getCharacterEncoding();
                    if (encoding == null) {
                        encoding = "UTF-8";
                    }
                    
                    File file = allocateFile(rawDataDir, "clipboard.txt");
                    
                    JSONObject fileRecord = new JSONObject();
                    JSONUtilities.safePut(fileRecord, "origin", "clipboard");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", encoding);
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", (String) null);
                    JSONUtilities.safePut(fileRecord, "format", "text");
                    JSONUtilities.safePut(fileRecord, "fileName", "(clipboard)");
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));
                    
                    progress.setProgress("Uploading pasted clipboard text",
                        calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
                    
                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFile(stream, file, null));
                    JSONUtilities.append(fileRecords, fileRecord);
                    
                    clipboardCount++;
                    
                } else if (name.equals("download")) {
                    String urlString = Streams.asString(stream);
                    URL url = new URL(urlString);
                    
                    JSONObject fileRecord = new JSONObject();
                    JSONUtilities.safePut(fileRecord, "origin", "download");
                    JSONUtilities.safePut(fileRecord, "url", urlString);
                    
                    for (UrlRewriter rewriter : ImportingManager.urlRewriters) {
                        Result result = rewriter.rewrite(urlString);
                        if (result != null) {
                            urlString = result.rewrittenUrl;
                            url = new URL(urlString);
                            
                            JSONUtilities.safePut(fileRecord, "url", urlString);
                            JSONUtilities.safePut(fileRecord, "format", result.format);
                            if (!result.download) {
                                downloadCount++;
                                JSONUtilities.append(fileRecords, fileRecord);
                                continue parts;
                            }
                        }
                    }

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
                            JSONUtilities.safePut(fileRecord, "declaredEncoding", encoding);
                            String contentType = null;
                            if (entity.getContentType() != null) {
                                contentType = entity.getContentType().getValue();
                            }
                            JSONUtilities.safePut(fileRecord, "declaredMimeType", contentType);
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
                        JSONUtilities.safePut(fileRecord, "declaredEncoding", 
                                urlConnection.getContentEncoding());
                        JSONUtilities.safePut(fileRecord, "declaredMimeType", 
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
                    
                    JSONObject fileRecord = new JSONObject();
                    JSONUtilities.safePut(fileRecord, "origin", "upload");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", request.getCharacterEncoding());
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", fileItem.getContentType());
                    JSONUtilities.safePut(fileRecord, "fileName", fileName);
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

                    progress.setProgress(
                        "Saving file " + fileName + " locally (" + formatBytes(fileSize) + " bytes)",
                        calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
                    
                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFile(stream, file, null));
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
        
        JSONUtilities.safePut(retrievalRecord, "uploadCount", uploadCount);
        JSONUtilities.safePut(retrievalRecord, "downloadCount", downloadCount);
        JSONUtilities.safePut(retrievalRecord, "clipboardCount", clipboardCount);
        JSONUtilities.safePut(retrievalRecord, "archiveCount", archiveCount);
    }

    private static boolean saveStream(InputStream stream, URL url, File rawDataDir, final Progress progress,
            final SavingUpdate update, JSONObject fileRecord, JSONArray fileRecords, long length)
            throws IOException, Exception {
        String localname = url.getPath();
        if (localname.isEmpty() || localname.endsWith("/")) {
            localname = localname + "temp";
        }
        File file = allocateFile(rawDataDir, localname);

        JSONUtilities.safePut(fileRecord, "fileName", file.getName());
        JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

        update.totalExpectedSize += length;

        progress.setProgress("Downloading " + url.toString(),
            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
        
        long actualLength = saveStreamToFile(stream, file, update);
        JSONUtilities.safePut(fileRecord, "size", actualLength);
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
    
    static public Reader getFileReader(ImportingJob job, JSONObject fileRecord, String commonEncoding)
        throws FileNotFoundException {
        
        return getFileReader(getFile(job, JSONUtilities.getString(fileRecord, "location", "")), fileRecord, commonEncoding);
    }
    
    static public Reader getFileReader(File file, JSONObject fileRecord, String commonEncoding) throws FileNotFoundException {
        return getReaderFromStream(new FileInputStream(file), fileRecord, commonEncoding);
    }
    
    static public Reader getReaderFromStream(InputStream inputStream, JSONObject fileRecord, String commonEncoding) {
        String encoding = getEncoding(fileRecord);
        if (encoding == null) {
            encoding = commonEncoding;
        }
        if (encoding != null) {
            try {
                return new InputStreamReader(inputStream, encoding);
            } catch (UnsupportedEncodingException e) {
                // Ignore and fall through
            }
        }
        return new InputStreamReader(inputStream);
    }
    
    static public File getFile(ImportingJob job, JSONObject fileRecord) {
        return getFile(job, JSONUtilities.getString(fileRecord, "location", ""));
    }
    
    static public File getFile(ImportingJob job, String location) {
        return new File(job.getRawDataDir(), location);
    }
    
    static public String getFileSource(JSONObject fileRecord) {
        return JSONUtilities.getString(
            fileRecord,
            "url",
            JSONUtilities.getString(fileRecord, "fileName", "unknown")
        );
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
            File rawDataDir, File file, JSONObject fileRecord, JSONArray fileRecords, final Progress progress) {
        
        String mimeType = JSONUtilities.getString(fileRecord, "declaredMimeType", null);
        String contentEncoding = JSONUtilities.getString(fileRecord, "declaredEncoding", null);
        
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
        JSONUtilities.append(fileRecords, fileRecord);
        
        return false;
    }
    
    static public void postProcessSingleRetrievedFile(File file, JSONObject fileRecord) {
        if (!fileRecord.has("format")) {
            JSONUtilities.safePut(fileRecord, "format",
                ImportingManager.getFormat(
                    file.getName(),
                    JSONUtilities.getString(fileRecord, "declaredMimeType", null)));
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
        JSONObject archiveFileRecord,
        JSONArray fileRecords,
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
                        
                        JSONObject fileRecord2 = new JSONObject();
                        JSONUtilities.safePut(fileRecord2, "origin", JSONUtilities.getString(archiveFileRecord, "origin", null));
                        JSONUtilities.safePut(fileRecord2, "declaredEncoding", (String) null);
                        JSONUtilities.safePut(fileRecord2, "declaredMimeType", (String) null);
                        JSONUtilities.safePut(fileRecord2, "fileName", fileName2);
                        JSONUtilities.safePut(fileRecord2, "archiveFileName", JSONUtilities.getString(archiveFileRecord, "fileName", null));
                        JSONUtilities.safePut(fileRecord2, "location", getRelativePath(file2, rawDataDir));

                        JSONUtilities.safePut(fileRecord2, "size", saveStreamToFile(tis, file2, null));
                        postProcessSingleRetrievedFile(file2, fileRecord2);
                        
                        JSONUtilities.append(fileRecords, fileRecord2);
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
                        
                        JSONObject fileRecord2 = new JSONObject();
                        JSONUtilities.safePut(fileRecord2, "origin", JSONUtilities.getString(archiveFileRecord, "origin", null));
                        JSONUtilities.safePut(fileRecord2, "declaredEncoding", (String) null);
                        JSONUtilities.safePut(fileRecord2, "declaredMimeType", (String) null);
                        JSONUtilities.safePut(fileRecord2, "fileName", fileName2);
                        JSONUtilities.safePut(fileRecord2, "archiveFileName", JSONUtilities.getString(archiveFileRecord, "fileName", null));
                        JSONUtilities.safePut(fileRecord2, "location", getRelativePath(file2, rawDataDir));

                        JSONUtilities.safePut(fileRecord2, "size", saveStreamToFile(zis, file2, null));
                        postProcessSingleRetrievedFile(file2, fileRecord2);
                        
                        JSONUtilities.append(fileRecords, fileRecord2);
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
        JSONObject fileRecord,
        final Progress progress
    ) throws IOException {
        String fileName = JSONUtilities.getString(fileRecord, "location", "unknown");
        for (String ext : new String[] {".gz",".bz2"}) {
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length()-ext.length());
                break;
            }
        }
        File file2 = allocateFile(rawDataDir, fileName);
        
        progress.setProgress("Uncompressing " + fileName, -1);
        
        saveStreamToFile(uncompressedIS, file2, null);
        
        JSONUtilities.safePut(fileRecord, "declaredEncoding", (String) null);
        JSONUtilities.safePut(fileRecord, "declaredMimeType", (String) null);
        JSONUtilities.safePut(fileRecord, "location", getRelativePath(file2, rawDataDir));
        
        return file2;
    }
    
    static private int calculateProgressPercent(long totalExpectedSize, long totalRetrievedSize) {
        return totalExpectedSize == 0 ? -1 : (int) (totalRetrievedSize * 100 / totalExpectedSize);
    }
    
    static private String formatBytes(long bytes) {
        return NumberFormat.getIntegerInstance().format(bytes);
    }
    
    static public String getEncoding(JSONObject fileRecord) {
        String encoding = JSONUtilities.getString(fileRecord, "encoding", null);
        if (encoding == null || encoding.isEmpty()) {
            encoding = JSONUtilities.getString(fileRecord, "declaredEncoding", null);
        }
        return encoding;
    }

    /**
     * Figure out the best (most common) format for the set of files, select
     * all files which match that format, and return the format found.
     * 
     * @param job ImportingJob object
     * @param retrievalRecord JSON object containing "files" key with all our files
     * @param fileSelectionIndexes JSON array of selected file indices matching best format
     * @return best (highest frequency) format
     */
    static public String autoSelectFiles(ImportingJob job, JSONObject retrievalRecord, JSONArray fileSelectionIndexes) {
        final Map<String, Integer> formatToCount = new HashMap<String, Integer>();
        List<String> formats = new ArrayList<String>();
        
        JSONArray fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        int count = fileRecords.length();
        for (int i = 0; i < count; i++) {
            JSONObject fileRecord = JSONUtilities.getObjectElement(fileRecords, i);
            String format = JSONUtilities.getString(fileRecord, "format", null);
            if (format != null) {
                if (formatToCount.containsKey(format)) {
                    formatToCount.put(format, formatToCount.get(format) + 1);
                } else {
                    formatToCount.put(format, 1);
                    formats.add(format);
                }
            }
        }
        Collections.sort(formats, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return formatToCount.get(o2) - formatToCount.get(o1);
            }
        });
        
        // Default to text/line-based to to avoid parsing as binary/excel.
        String bestFormat = formats.size() > 0 ? formats.get(0) : "text/line-based";
        if (JSONUtilities.getInt(retrievalRecord, "archiveCount", 0) == 0) {
            // If there's no archive, then select everything
            for (int i = 0; i < count; i++) {
                JSONUtilities.append(fileSelectionIndexes, i);
            }
        } else {
            // Otherwise, select files matching the best format
            for (int i = 0; i < count; i++) {
                JSONObject fileRecord = JSONUtilities.getObjectElement(fileRecords, i);
                String format = JSONUtilities.getString(fileRecord, "format", null);
                if (format != null && format.equals(bestFormat)) {
                    JSONUtilities.append(fileSelectionIndexes, i);
                }
            }
            
            // If nothing matches the best format but we have some files,
            // then select them all
            if (fileSelectionIndexes.length() == 0 && count > 0) {
                for (int i = 0; i < count; i++) {
                    JSONUtilities.append(fileSelectionIndexes, i);
                }
            }
        }
        return bestFormat;
    }
    
    static public String getCommonFormatForSelectedFiles(ImportingJob job, JSONArray fileSelectionIndexes) {
        JSONObject retrievalRecord = job.getRetrievalRecord();
        
        final Map<String, Integer> formatToCount = new HashMap<String, Integer>();
        List<String> formats = new ArrayList<String>();
        
        JSONArray fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        int count = fileSelectionIndexes.length();
        for (int i = 0; i < count; i++) {
            int index = JSONUtilities.getIntElement(fileSelectionIndexes, i, -1);
            if (index >= 0 && index < fileRecords.length()) {
                JSONObject fileRecord = JSONUtilities.getObjectElement(fileRecords, index);
                String format = JSONUtilities.getString(fileRecord, "format", null);
                if (format != null) {
                    if (formatToCount.containsKey(format)) {
                        formatToCount.put(format, formatToCount.get(format) + 1);
                    } else {
                        formatToCount.put(format, 1);
                        formats.add(format);
                    }
                }
            }
        }
        Collections.sort(formats, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return formatToCount.get(o2) - formatToCount.get(o1);
            }
        });
        
        return formats.size() > 0 ? formats.get(0) : null;
    }
    
    static String guessBetterFormat(ImportingJob job, String bestFormat) {
        JSONObject retrievalRecord = job.getRetrievalRecord();
        return retrievalRecord != null ? guessBetterFormat(job, retrievalRecord, bestFormat) : bestFormat;
    }
    
    static String guessBetterFormat(ImportingJob job, JSONObject retrievalRecord, String bestFormat) {
        JSONArray fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        return fileRecords != null ? guessBetterFormat(job, fileRecords, bestFormat) : bestFormat;
    }
    
    static String guessBetterFormat(ImportingJob job, JSONArray fileRecords, String bestFormat) {
        if (bestFormat != null && fileRecords != null && fileRecords.length() > 0) {
            JSONObject firstFileRecord = JSONUtilities.getObjectElement(fileRecords, 0);
            String encoding = getEncoding(firstFileRecord);
            String location = JSONUtilities.getString(firstFileRecord, "location", null);
            
            if (location != null) {
                File file = new File(job.getRawDataDir(), location);
                
                while (true) {
                    String betterFormat = null;
                    
                    List<FormatGuesser> guessers = ImportingManager.formatToGuessers.get(bestFormat);
                    if (guessers != null) {
                        for (FormatGuesser guesser : guessers) {
                            betterFormat = guesser.guess(file, encoding, bestFormat);
                            if (betterFormat != null) {
                                break;
                            }
                        }
                    }
                    
                    if (betterFormat != null && !betterFormat.equals(bestFormat)) {
                        bestFormat = betterFormat;
                    } else {
                        break;
                    }
                }
            }
        }
        return bestFormat;
    }
    
    static void rankFormats(ImportingJob job, final String bestFormat, JSONArray rankedFormats) {
        final Map<String, String[]> formatToSegments = new HashMap<String, String[]>();
        
        boolean download = bestFormat == null ? true :
            ImportingManager.formatToRecord.get(bestFormat).download;
        
        List<String> formats = new ArrayList<String>(ImportingManager.formatToRecord.keySet().size());
        for (String format : ImportingManager.formatToRecord.keySet()) {
            Format record = ImportingManager.formatToRecord.get(format);
            if (record.uiClass != null && record.parser != null && record.download == download) {
                formats.add(format);
                formatToSegments.put(format, format.split("/"));
            }
        }
        
        if (bestFormat == null) {
            Collections.sort(formats);
        } else {
            Collections.sort(formats, new Comparator<String>() {
                @Override
                public int compare(String format1, String format2) {
                    if (format1.equals(bestFormat)) {
                        return -1;
                    } else if (format2.equals(bestFormat)) {
                        return 1;
                    } else {
                        return compareBySegments(format1, format2);
                    }
                }
                
                int compareBySegments(String format1, String format2) {
                    int c = commonSegments(format2) - commonSegments(format1);
                    return c != 0 ? c : format1.compareTo(format2);
                }
                
                int commonSegments(String format) {
                    String[] bestSegments = formatToSegments.get(bestFormat);
                    String[] segments = formatToSegments.get(format);
                    if (bestSegments == null || segments == null) {
                        return 0;
                    } else {
                        int i;
                        for (i = 0; i < bestSegments.length && i < segments.length; i++) {
                            if (!bestSegments[i].equals(segments[i])) {
                                break;
                            }
                        }
                        return i;
                    }
                }
            });
        }
        
        for (String format : formats) {
            JSONUtilities.append(rankedFormats, format);
        }
    }

    
    static public void previewParse(ImportingJob job, String format, JSONObject optionObj, List<Exception> exceptions) {
        Format record = ImportingManager.formatToRecord.get(format);
        if (record == null || record.parser == null) {
            // TODO: what to do?
            return;
        }
        
        job.prepareNewProject();
        
        record.parser.parse(
            job.project,
            job.metadata,
            job,
            job.getSelectedFileRecords(),
            format,
            100,
            optionObj,
            exceptions
        );
        
        job.project.update(); // update all internal models, indexes, caches, etc.
    }
    
    static public long createProject(
            final ImportingJob job,
            final String format,
            final JSONObject optionObj,
            final List<Exception> exceptions,
            boolean synchronous) {
        final Format record = ImportingManager.formatToRecord.get(format);
        if (record == null || record.parser == null) {
            // TODO: what to do?
            return -1;
        }
        
        job.setState("creating-project");
        
        final Project project = new Project();
        if (synchronous) {
            createProjectSynchronously(
                job, format, optionObj, exceptions, record, project);
        } else {
            new Thread() {
                @Override
                public void run() {
                    createProjectSynchronously(
                        job, format, optionObj, exceptions, record, project);
                }
            }.start();
        }
        return project.id;
    }
    
    static private void createProjectSynchronously(
        final ImportingJob job,
        final String format,
        final JSONObject optionObj,
        final List<Exception> exceptions,
        final Format record,
        final Project project
    ) {
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
        String encoding = JSONUtilities.getString(optionObj, "encoding", "UTF-8");
        if ("".equals(encoding)) {
            // encoding can be present, but empty, which won't trigger JSONUtilities default processing
            encoding = "UTF-8";
        }
        pm.setEncoding(encoding);
        
        record.parser.parse(
            project,
            pm,
            job,
            job.getSelectedFileRecords(),
            format,
            -1,
            optionObj,
            exceptions
        );
        
        if (!job.canceled) {
            if (exceptions.size() == 0) {
                project.update(); // update all internal models, indexes, caches, etc.
                
                ProjectManager.singleton.registerProject(project, pm);
                
                job.setProjectID(project.id);
                job.setState("created-project");
            } else {
                job.setError(exceptions);
            }
            job.touch();
            job.updating = false;
        }
    }
    

}
