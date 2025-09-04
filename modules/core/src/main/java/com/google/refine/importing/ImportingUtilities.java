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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.StreamingNotSupportedException;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.InputStreamStatistics;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileSystem;
import org.apache.commons.io.FilenameUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.mozilla.universalchardet.UnicodeBOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingManager.Format;
import com.google.refine.importing.UrlRewriter.Result;
import com.google.refine.model.Project;
import com.google.refine.util.HttpClient;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class ImportingUtilities {

    public static final int BUFFER_SIZE = 128 * 1024;
    final static protected Logger logger = LoggerFactory.getLogger("importing-utilities");

    final public static List<String> allowedProtocols = Arrays.asList("http", "https", "ftp", "sftp");

    static public interface Progress {

        public void setProgress(String message, int percent);

        public boolean isCanceled();
    }

    /**
     * @deprecated Use
     *             {@link #loadDataAndPrepareJob(HttpServletRequest, HttpServletResponse, Map, ImportingJob, ObjectNode)}
     *             instead.
     */
    @Deprecated
    static public void loadDataAndPrepareJob(
            HttpServletRequest request,
            HttpServletResponse response,
            Properties parameters,
            final ImportingJob job,
            ObjectNode config) throws IOException, ServletException {

        Map<String, String> parametersMap = propsToMap(parameters);
        loadDataAndPrepareJob(request, response, parametersMap, job, config);
    }

    private static Map<String, String> propsToMap(Properties properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
    }

    static public void loadDataAndPrepareJob(
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, String> parameters,
            final ImportingJob job,
            ObjectNode config) throws IOException, ServletException {

        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(config, "retrievalRecord", retrievalRecord);
        JSONUtilities.safePut(config, "state", "loading-raw-data");

        final ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
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
                    });
        } catch (Exception e) {
            JSONUtilities.safePut(config, "state", "error");
            JSONUtilities.safePut(config, "error", "Error uploading data");
            // Prefer the nested exception if it's something we wrapped, otherwise use the top level exception
            JSONUtilities.safePut(config, "errorDetails", e.getCause() == null ? e.getLocalizedMessage() : String.valueOf(e.getCause()));
            throw new IOException(e);
        }

        ArrayNode fileSelectionIndexes = ParsingUtilities.mapper.createArrayNode();
        JSONUtilities.safePut(config, "fileSelection", fileSelectionIndexes);

        EncodingGuesser.guess(job);

        String bestFormat = ImportingUtilities.autoSelectFiles(job, retrievalRecord, fileSelectionIndexes);
        bestFormat = ImportingUtilities.guessBetterFormat(job, bestFormat);

        ArrayNode rankedFormats = ParsingUtilities.mapper.createArrayNode();
        ImportingUtilities.rankFormats(job, bestFormat, rankedFormats);
        JSONUtilities.safePut(config, "rankedFormats", rankedFormats);

        JSONUtilities.safePut(config, "state", "ready");
        JSONUtilities.safePut(config, "hasData", true);
        config.remove("progress");
    }

    static public void updateJobWithNewFileSelection(ImportingJob job, ArrayNode fileSelectionArray) {
        job.setFileSelection(fileSelectionArray);

        String bestFormat = ImportingUtilities.getCommonFormatForSelectedFiles(job, fileSelectionArray);
        bestFormat = ImportingUtilities.guessBetterFormat(job, bestFormat);

        ArrayNode rankedFormats = ParsingUtilities.mapper.createArrayNode();
        ImportingUtilities.rankFormats(job, bestFormat, rankedFormats);
        job.setRankedFormats(rankedFormats);
    }

    /**
     * @deprecated Use {@link #retrieveContentFromPostRequest(HttpServletRequest, Map, File, ObjectNode, Progress)}
     *             instead.
     */
    @Deprecated
    static public void retrieveContentFromPostRequest(
            HttpServletRequest request,
            Properties parameters,
            File rawDataDir,
            ObjectNode retrievalRecord,
            final Progress progress) throws IOException, FileUploadException {

        Map<String, String> parametersMap = propsToMap(parameters);
        retrieveContentFromPostRequest(request, parametersMap, rawDataDir, retrievalRecord, progress);
    }

    static public void retrieveContentFromPostRequest(
            HttpServletRequest request,
            Map<String, String> parameters,
            File rawDataDir,
            ObjectNode retrievalRecord,
            final Progress progress) throws IOException, FileUploadException {

        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
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

        List<FileItem> tempFiles = (List<FileItem>) upload.parseRequest(request);

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

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(fileRecord, "origin", "clipboard");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", encoding);
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", (String) null);
                    JSONUtilities.safePut(fileRecord, "fileName", "(clipboard)");
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

                    progress.setProgress("Uploading pasted clipboard text",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFile(stream, file, null));
                    JSONUtilities.safePut(fileRecord, "format", guessBetterFormat(file, fileRecord));
                    JSONUtilities.append(fileRecords, fileRecord);

                    clipboardCount++;

                } else if (name.equals("download")) {
                    String urlString = Streams.asString(stream).trim();
                    URL url = new URL(urlString);

                    if (!allowedProtocols.contains(url.getProtocol().toLowerCase())) {
                        throw new IOException("Unsupported protocol: " + url.getProtocol());
                    }

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
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
                        final URL lastUrl = url;
                        final HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {

                            @Override
                            public String handleResponse(final ClassicHttpResponse response) throws IOException {
                                final int status = response.getCode();
                                if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
                                    final HttpEntity entity = response.getEntity();
                                    if (entity == null) {
                                        throw new IOException("No content found in " + lastUrl.toExternalForm());
                                    }

                                    try {
                                        InputStream stream2 = entity.getContent();

                                        String mimeType = null;
                                        String charset = null;
                                        ContentType contentType = ContentType.parse(entity.getContentType());
                                        if (contentType != null) {
                                            mimeType = contentType.getMimeType();
                                            Charset cs = contentType.getCharset();
                                            if (cs != null) {
                                                charset = cs.toString();
                                            }
                                        }
                                        JSONUtilities.safePut(fileRecord, "declaredMimeType", mimeType);
                                        JSONUtilities.safePut(fileRecord, "declaredEncoding", charset);
                                        if (saveStream(stream2, lastUrl, rawDataDir, progress, update,
                                                fileRecord, fileRecords,
                                                entity.getContentLength())) {
                                            return "saved"; // signal to increment archive count
                                        }

                                    } catch (final IOException ex) {
                                        throw new ClientProtocolException(ex);
                                    }
                                    return null;
                                } else {
                                    // String errorBody = EntityUtils.toString(response.getEntity());
                                    throw new ClientProtocolException(String.format("HTTP error %d : %s for URL %s", status,
                                            response.getReasonPhrase(), lastUrl.toExternalForm()));
                                }
                            }
                        };

                        HttpClient httpClient = new HttpClient();
                        if (httpClient.getResponse(urlString, null, responseHandler) != null) {
                            archiveCount++;
                        }
                        downloadCount++;
                    } else {
                        // Fallback handling for non HTTP connections (only FTP?)
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setConnectTimeout(5000);
                        urlConnection.connect();
                        try (InputStream stream2 = urlConnection.getInputStream()) {
                            JSONUtilities.safePut(fileRecord, "declaredEncoding",
                                    urlConnection.getContentEncoding());
                            JSONUtilities.safePut(fileRecord, "declaredMimeType",
                                    urlConnection.getContentType());
                            if (saveStream(stream2, url, rawDataDir, progress,
                                    update, fileRecord, fileRecords,
                                    urlConnection.getContentLength())) {
                                archiveCount++;
                            }
                            downloadCount++;
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

                    ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
                    JSONUtilities.safePut(fileRecord, "origin", "upload");
                    JSONUtilities.safePut(fileRecord, "declaredEncoding", request.getCharacterEncoding());
                    JSONUtilities.safePut(fileRecord, "declaredMimeType", fileItem.getContentType());
                    JSONUtilities.safePut(fileRecord, "fileName", fileName);
                    JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

                    progress.setProgress(
                            "Saving file " + fileName + " locally (" + formatBytes(fileSize) + " bytes)",
                            calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

                    JSONUtilities.safePut(fileRecord, "size", saveStreamToFile(stream, file, null));
                    // TODO: This needs to be refactored to be able to test import from archives
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
            final SavingUpdate update, ObjectNode fileRecord, ArrayNode fileRecords, long length)
            throws IOException {
        String localname = url.getPath();
        if (localname.isEmpty() || localname.endsWith("/")) {
            localname = localname + "temp";
        }
        File file = allocateFile(rawDataDir, localname);

        JSONUtilities.safePut(fileRecord, "fileName", file.getName());
        JSONUtilities.safePut(fileRecord, "location", getRelativePath(file, rawDataDir));

        update.totalExpectedSize += length;

        progress.setProgress("Downloading " + url, // TODO: Localize
                calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));

        long actualLength = saveStreamToFile(stream, file, update);
        JSONUtilities.safePut(fileRecord, "size", actualLength);
        if (actualLength == 0) {
            throw new IOException("No content found in " + url);
        } else if (length >= 0) {
            update.totalExpectedSize += (actualLength - length);
        } else {
            update.totalExpectedSize += actualLength;
        }
        progress.setProgress("Saving " + url + " locally", // TODO: Localize
                calculateProgressPercent(update.totalExpectedSize, update.totalRetrievedSize));
        return postProcessRetrievedFile(rawDataDir, file, fileRecord, fileRecords, progress);
    }

    static public String getRelativePath(File file, File dir) {
        String location = file.getAbsolutePath().substring(dir.getAbsolutePath().length());
        return (location.startsWith(File.separator)) ? location.substring(1) : location;
    }

    /**
     * Replace the illegal character with '-' in the path in Windows
     *
     * @param path:
     *            file path
     * @return the replaced path or original path if the OS is not Windows
     */
    public static String normalizePath(String path) {
        FileSystem currentFileSystem = FileSystem.getCurrent();
        if (currentFileSystem != FileSystem.WINDOWS) {
            return path;
        }
        // normalize the file name if the current system is windows
        String normalizedLocalName = "";
        String pathWithWSeparator = FilenameUtils.separatorsToWindows(path);
        String separator = String.format("\\%c", File.separatorChar);
        String[] paths = pathWithWSeparator.split(separator);
        for (String p : paths) {
            if (p.equals("")) {
                continue;
            }
            p = currentFileSystem.toLegalFileName(p, '-');
            normalizedLocalName += String.format("%c%s", File.separatorChar, p);
        }
        return normalizedLocalName;
    }

    static public File allocateFile(File dir, String name) {
        int q = name.indexOf('?');
        if (q > 0) {
            name = name.substring(0, q);
        }
        name = normalizePath(name);
        File file = new File(dir, name);
        Path normalizedFile = file.toPath().normalize();
        // For CVE-2018-19859, issue #1840
        if (!normalizedFile.startsWith(dir.toPath().normalize() + File.separator)) {
            throw new IllegalArgumentException("Zip archives with files escaping their root directory are not allowed.");
        }

        Path normalizedParent = normalizedFile.getParent();
        String fileName = normalizedFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String prefix = dot < 0 ? fileName : fileName.substring(0, dot);
        String suffix = dot < 0 ? "" : fileName.substring(dot);
        int index = 2;
        while (file.exists()) {
            file = normalizedParent.resolve(prefix + "-" + index++ + suffix).toFile();
        }

        file.getParentFile().mkdirs();

        return file;
    }

    static public Reader getFileReader(ImportingJob job, ObjectNode fileRecord, String commonEncoding)
            throws FileNotFoundException {

        return getFileReader(getFile(job, JSONUtilities.getString(fileRecord, "location", "")), fileRecord, commonEncoding);
    }

    static public Reader getFileReader(File file, ObjectNode fileRecord, String commonEncoding) throws FileNotFoundException {
        return getReaderFromStream(new FileInputStream(file), fileRecord, commonEncoding);
    }

    static public Reader getReaderFromStream(InputStream inputStream, ObjectNode fileRecord, String commonEncoding) {
        // FIXME: commonEncoding may represent user's override of guessed encoding, so should be used in preference
        // to the guessed encoding(s). But, what to do if we have multiple files with different encodings?
        // (very unlikely, but still possible)
        String encoding = getEncoding(fileRecord);
        if (commonEncoding != null && !commonEncoding.equals(encoding)) {
            logger.info("Overriding guessed encoding {} with user's choice: {}", encoding, commonEncoding);
            encoding = commonEncoding;
        }
        try {
            return getInputStreamReader(inputStream, encoding);
        } catch (UnsupportedEncodingException e) {
            // This should never happen since they picked from a list of supported encodings
            throw new RuntimeException("Unsupported encoding: " + encoding, e);
        } catch (IOException e) {
            throw new RuntimeException("Exception from UnicodeBOMInputStream", e);
        }
    }

    static public File getFile(ImportingJob job, ObjectNode fileRecord) {
        return getFile(job, JSONUtilities.getString(fileRecord, "location", ""));
    }

    static public File getFile(ImportingJob job, String location) {
        return new File(job.getRawDataDir(), location);
    }

    static public String getFileSource(ObjectNode fileRecord) {
        return JSONUtilities.getString(
                fileRecord,
                "url",
                JSONUtilities.getString(fileRecord, "fileName", "unknown"));
    }

    static public String getFileName(ObjectNode fileRecord) {
        return JSONUtilities.getString(
                fileRecord,
                "fileName",
                "unknown");
    }

    static public String getArchiveFileName(ObjectNode fileRecord) {
        return JSONUtilities.getString(
                fileRecord,
                "archiveFileName",
                null);
    }

    static public boolean hasArchiveFileField(List<ObjectNode> fileRecords) {
        List<ObjectNode> filterResults = fileRecords.stream().filter(fileRecord -> getArchiveFileName(fileRecord) != null)
                .collect(Collectors.toList());
        return filterResults.size() > 0;
    }

    static private abstract class SavingUpdate {

        public long totalExpectedSize = 0;
        public long totalRetrievedSize = 0;

        abstract public void savedMore();

        abstract public boolean isCanceled();
    }

    static private long saveStreamToFile(InputStream stream, File file, SavingUpdate update) throws IOException {
        long length = 0;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] bytes = new byte[16 * 1024];
            int c;
            while ((update == null || !update.isCanceled()) && (c = stream.read(bytes)) > 0) {
                fos.write(bytes, 0, c);
                length += c;

                if (update != null) {
                    update.totalRetrievedSize += c;
                    update.savedMore();
                    if (stream instanceof InputStreamStatistics) {
                        // TODO: Can we use this to improve progress reporting?
                        ((InputStreamStatistics) stream).getCompressedCount();
                    }
                }
            }
            return length;
        }
    }

    /**
     * Processes a retrieved file to explode archives and decompress compressed files.
     *
     * @param rawDataDir
     *            the directory where raw data files are stored
     * @param file
     *            the file to be processed
     * @param fileRecord
     *            an ObjectNode containing metadata for the file being processed
     * @param fileRecords
     *            an ArrayNode where metadata for all processed files is recorded
     * @param progress
     *            an implementation of Progress interface to track progress and check for cancellation
     * @return true if the file was successfully handled as an archive
     * @throws IOException
     *             if a processing error or file handling issue occurs
     */
    static public boolean postProcessRetrievedFile(
            File rawDataDir, File file, ObjectNode fileRecord, ArrayNode fileRecords, final Progress progress) throws IOException {

        String mimeType = JSONUtilities.getString(fileRecord, "declaredMimeType", null);
        String contentEncoding = JSONUtilities.getString(fileRecord, "declaredEncoding", null);

        if (explodeArchive(rawDataDir, file, mimeType, fileRecord, fileRecords, progress)) {
            // FIXME: We really don't want to unpack the entire archive before doing our preview
            file.delete();
            return true;
        }

        if (file.getName().endsWith(".7z")) {
            try {
                // FIXME: We really don't want to unpack the entire archive before doing our preview
                if (explode7zipArchive(rawDataDir, file, fileRecord, fileRecords, progress)) {
                    file.delete();
                    return true;
                }
            } catch (Exception e) {
                throw new IOException("Unexpected error attempting to explode .7z file", e);
            }
        }

        File file2 = uncompressFile(rawDataDir, file, mimeType, contentEncoding, fileRecord, progress);
        if (file2 != null) {
            file.delete();
            file = file2;
        }

        postProcessSingleRetrievedFile(file, fileRecord);
        JSONUtilities.append(fileRecords, fileRecord);

        return false;
    }

    static public void postProcessSingleRetrievedFile(File file, ObjectNode fileRecord) {
        if (!fileRecord.has("format")) {
            JSONUtilities.safePut(fileRecord, "format", guessBetterFormat(file, fileRecord));
        }
    }

    @Deprecated
    static public InputStream tryOpenAsArchive(File file, String mimeType) throws IOException {
        return tryOpenAsArchive(file, mimeType, null);
    }

    @Deprecated
    static public InputStream tryOpenAsArchive(File file, String mimeType, String contentType) throws IOException {
        return null;
    }

    private static boolean isFileGZipped(File file) {
        int magic = 0;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
        } catch (IOException ignored) {
        }
        return magic == GZIPInputStream.GZIP_MAGIC;
    }

    public static boolean isCompressed(File file) throws IOException {
        // Check for common compressed file types to protect ourselves from binary data
        try (final BufferedInputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));) {
            try {
                is.mark(10);
                CompressorInputStream input = new CompressorStreamFactory()
                        .createCompressorInputStream(is);
                if (input != null) {
                    return true;
                }
            } catch (CompressorException e) {
                // Fall through and try other methods
            }
            byte[] magic = new byte[4];
            is.mark(100);
            int count = is.read(magic);
            // This legacy code is still useful for things like ZIPs with unsupported compressors (e.g. PPMD)
            if (count == 4 && Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x03, 0x04 }) || // start of zip
                    Arrays.equals(magic, new byte[] { 0x50, 0x4B, 0x07, 0x08 }) // zip data descriptor
            ) {
                return true;
            }
            is.reset();
            is.mark(100);
            try (CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream(is);) {
                return true;
            } catch (CompressorException e) {
                // Not technically compressed, but binary formats as well (e.g. zip)
                is.reset();
                try (ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(is);) {
                    return true;
                } catch (StreamingNotSupportedException ex) {
                    // 7zip returns StreamingNotSupportedException, but it can be used with File or SeekableByteChannel
                    return true;
                } catch (ArchiveException ex) {
                    return false;
                }
            }
        }
    }

    // FIXME: This is wasteful of space and time. We should try to process on the fly
    // (also for preview, we don't want the user to wait while we unpack the entire thing)
    static private boolean explodeArchive(
            File rawDataDir,
            File file,
            String mimeType,
            ObjectNode archiveFileRecord,
            ArrayNode fileRecords,
            final Progress progress) throws IOException {

        ArchiveInputStream archiveInputStream = null;

        String myMimeType = Strings.nullToEmpty(mimeType);
        String filename = file.getName();
        if (myMimeType.startsWith("application/vnd.openxmlformats-officedocument.") ||
                myMimeType.startsWith("application/vnd.oasis.opendocument.") ||
                filename.endsWith(".ods") ||
                filename.endsWith(".xlsx")) {
            return false;
        }

        try (
                FileInputStream fis = new FileInputStream(file);
                FileChannel fc = fis.getChannel();
                final BufferedInputStream is = new BufferedInputStream(fis);) {
            is.mark(100);
            try {
                // Use a CompressorStreamFactory configured to decompress concatenated streams
                CompressorInputStream in = new CompressorStreamFactory(true).createCompressorInputStream(is);
                try {
                    is.reset();
                    archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(is);
                } catch (ArchiveException e) {
                    // Not a recognized archive type
                    return false;
                }
            } catch (CompressorException e) {
                // Only a few archive types are compressed, so this isn't unexpected
                is.reset();
                String format;
                try {
                    format = ArchiveStreamFactory.detect(is);
                } catch (ArchiveException e1) {
                    // It's not an archive format that we recognize
                    return false;
                }
                try {
                    if (ArchiveStreamFactory.SEVEN_Z.equals(format)) {
                        SevenZFile szf = new SevenZFile.Builder().setSeekableByteChannel(fc.position(0)).get();
                        for (SevenZArchiveEntry entry : szf.getEntries()) {
                            if (progress.isCanceled()) {
                                break;
                            }
                            if (!entry.isDirectory()) {
                                ObjectNode fileRecord2 = processArchiveEntry(rawDataDir, archiveFileRecord, progress, entry.getName(),
                                        szf.getInputStream(entry));
                                JSONUtilities.append(fileRecords, fileRecord2);
                            }
                        }
                    } else if (ArchiveStreamFactory.ZIP.equals(format)) {
                        // Apache docs recommend ZipFile over ZipArchiveInputStream, which is what the factory returns
                        // so we handle it separately, similar to 7Zip with a SeekableByteChannel
                        ZipFile zf = new ZipFile.Builder().setSeekableByteChannel(fc.position(0)).get();
                        for (Iterator<ZipArchiveEntry> it = zf.getEntries().asIterator(); it.hasNext();) {
                            ZipArchiveEntry entry = it.next();
                            if (progress.isCanceled()) {
                                break;
                            }
                            if (!entry.isDirectory()) {
                                ObjectNode fileRecord2 = processArchiveEntry(rawDataDir, archiveFileRecord, progress, entry.getName(),
                                        zf.getInputStream(entry));
                                JSONUtilities.append(fileRecords, fileRecord2);
                            }
                        }
                    } else {
                        is.reset();
                        archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(is);
                        ArchiveEntry te;
                        while (!progress.isCanceled() && (te = archiveInputStream.getNextEntry()) != null) {
                            if (!te.isDirectory()) {
                                ObjectNode fileRecord2 = processArchiveEntry(rawDataDir, archiveFileRecord, progress, te.getName(),
                                        archiveInputStream);
                                JSONUtilities.append(fileRecords, fileRecord2);
                            }
                        }
                    }
                } catch (ArchiveException ex) {
                    // TODO: We could try skipping just this entry and continuing, but this is the historical behavior
                    throw new IOException("Error expanding archive", ex);
                }
            }
        }
        return true;
    }

    /**
     * Explode a 7-zip archive. The decompessor doesn't support stream based decompression, so we need to do it
     * separately/differently from all the others.
     */
    private static boolean explode7zipArchive(File rawDataDir, File file, ObjectNode archiveFileRecord, ArrayNode fileRecords,
            Progress progress) throws IOException {

        SevenZFile sevenZFile = new SevenZFile.Builder().setFile(file).get();
        if (sevenZFile == null) {
            return false;
        }

        SevenZArchiveEntry entry;
        while (!progress.isCanceled() && (entry = sevenZFile.getNextEntry()) != null) {
            String fileName2 = entry.getName();
            if (!entry.isDirectory()) {
                File file2 = allocateFile(rawDataDir, fileName2);
                progress.setProgress("Extracting " + fileName2, -1);
                ObjectNode fileRecord2 = ParsingUtilities.mapper.createObjectNode();
                JSONUtilities.safePut(fileRecord2, "origin", JSONUtilities.getString(archiveFileRecord, "origin", null));
                JSONUtilities.safePut(fileRecord2, "declaredEncoding", (String) null);
                JSONUtilities.safePut(fileRecord2, "declaredMimeType", (String) null);
                JSONUtilities.safePut(fileRecord2, "fileName", fileName2);
                JSONUtilities.safePut(fileRecord2, "archiveFileName", JSONUtilities.getString(archiveFileRecord, "fileName", null));
                JSONUtilities.safePut(fileRecord2, "location", getRelativePath(file2, rawDataDir));

                byte[] content = new byte[BUFFER_SIZE];
                // TODO: Track progress and/or decompression ratio
//                    sevenZFile.getStatisticsForCurrentEntry().getCompressedCount();
//                    sevenZFile.getStatisticsForCurrentEntry().getUncompressedCount();
                try (FileOutputStream fos = new FileOutputStream(file2)) {
                    int len;
                    long total = 0;
                    while ((len = sevenZFile.read(content, 0, BUFFER_SIZE)) > 0) {
                        fos.write(content, 0, len);
                        total = total + len;
                    }
                    JSONUtilities.safePut(fileRecord2, "size", total);
                }

                postProcessSingleRetrievedFile(file2, fileRecord2);
                JSONUtilities.append(fileRecords, fileRecord2);
            }
        }
        return true;
    }

    private static ObjectNode processArchiveEntry(File rawDataDir, ObjectNode archiveFileRecord, Progress progress, String entryName,
            InputStream archiveInputStream) throws IOException {
        File tmpFile = allocateFile(rawDataDir, entryName);

        progress.setProgress("Extracting " + entryName, -1);

        ObjectNode fileRecord2 = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord2, "origin", JSONUtilities.getString(archiveFileRecord, "origin", null));
        JSONUtilities.safePut(fileRecord2, "declaredEncoding", (String) null);
        JSONUtilities.safePut(fileRecord2, "declaredMimeType", (String) null);
        JSONUtilities.safePut(fileRecord2, "fileName", entryName);
        JSONUtilities.safePut(fileRecord2, "archiveFileName", JSONUtilities.getString(archiveFileRecord, "fileName", null));
        JSONUtilities.safePut(fileRecord2, "location", getRelativePath(tmpFile, rawDataDir));

        JSONUtilities.safePut(fileRecord2, "size", saveStreamToFile(archiveInputStream, tmpFile, null));
        postProcessSingleRetrievedFile(tmpFile, fileRecord2);
        return fileRecord2;
    }

    static File uncompressFile(
            File rawDataDir,
            File file,
            String mimeType,
            String contentEncoding,
            ObjectNode fileRecord,
            final Progress progress) throws IOException {

        String fileName = JSONUtilities.getString(fileRecord, "location", "unknown");
        for (String ext : new String[] { ".gz", ".bz2" }) {
            if (fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length() - ext.length());
                break;
            }
        }

        try (
                final BufferedInputStream is = new BufferedInputStream(Files.newInputStream(file.toPath()));
                // Use a CompressorStreamFactory configured to decompress concatenated streams
                CompressorInputStream uncompressedIS = new CompressorStreamFactory(true).createCompressorInputStream(is);) {

            File file2 = allocateFile(rawDataDir, fileName);

            progress.setProgress("Uncompressing " + fileName, -1);

            saveStreamToFile(uncompressedIS, file2, null);

            JSONUtilities.safePut(fileRecord, "declaredEncoding", (String) null);
            JSONUtilities.safePut(fileRecord, "declaredMimeType", (String) null);
            JSONUtilities.safePut(fileRecord, "location", getRelativePath(file2, rawDataDir));

            return file2;
        } catch (CompressorException ex) {
            // If we weren't able to find a decompressor, just return. Allow any other IOException to throw
            return null;
        }
    }

    static private int calculateProgressPercent(long totalExpectedSize, long totalRetrievedSize) {
        return totalExpectedSize == 0 ? -1 : (int) (totalRetrievedSize * 100 / totalExpectedSize);
    }

    static private String formatBytes(long bytes) {
        return NumberFormat.getIntegerInstance().format(bytes);
    }

    static public String getEncoding(ObjectNode firstFileRecord) {
        String encoding = JSONUtilities.getString(firstFileRecord, "encoding", null);
        if (encoding == null || encoding.isEmpty()) {
            encoding = JSONUtilities.getString(firstFileRecord, "declaredEncoding", null);
        }
        return encoding;
    }

    /**
     * Figure out the best (most common) format for the set of files, select all files which match that format, and
     * return the format found.
     * 
     * @param job
     *            ImportingJob object
     * @param retrievalRecord
     *            JSON object containing "files" key with all our files
     * @param fileSelectionIndexes
     *            JSON array of selected file indices matching best format
     * @return best (highest frequency) format
     */
    static public String autoSelectFiles(ImportingJob job, ObjectNode retrievalRecord, ArrayNode fileSelectionIndexes) {
        final Map<String, Integer> formatToCount = new HashMap<String, Integer>();
        List<String> formats = new ArrayList<String>();

        ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        int count = fileRecords.size();
        for (int i = 0; i < count; i++) {
            ObjectNode fileRecord = JSONUtilities.getObjectElement(fileRecords, i);
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

        // Default to "text" to avoid parsing as "binary/excel".
        // "text" is more general than "text/line-based", so a better starting point
        String bestFormat = formats.size() > 0 ? formats.get(0) : "text";
        if (JSONUtilities.getInt(retrievalRecord, "archiveCount", 0) == 0) {
            // If there's no archive, then select everything
            for (int i = 0; i < count; i++) {
                JSONUtilities.append(fileSelectionIndexes, i);
            }
        } else {
            // Otherwise, select files matching the best format
            for (int i = 0; i < count; i++) {
                ObjectNode fileRecord = JSONUtilities.getObjectElement(fileRecords, i);
                String format = JSONUtilities.getString(fileRecord, "format", null);
                if (format != null && format.equals(bestFormat)) {
                    JSONUtilities.append(fileSelectionIndexes, i);
                }
            }

            // If nothing matches the best format, but we have some files,
            // then select them all
            if (fileSelectionIndexes.size() == 0 && count > 0) {
                for (int i = 0; i < count; i++) {
                    JSONUtilities.append(fileSelectionIndexes, i);
                }
            }
        }
        return bestFormat;
    }

    static public String getCommonFormatForSelectedFiles(ImportingJob job, ArrayNode fileSelectionIndexes) {
        ObjectNode retrievalRecord = job.getRetrievalRecord();

        final Map<String, Integer> formatToCount = new HashMap<String, Integer>();
        List<String> formats = new ArrayList<String>();

        ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        int count = fileSelectionIndexes.size();
        for (int i = 0; i < count; i++) {
            int index = JSONUtilities.getIntElement(fileSelectionIndexes, i, -1);
            if (index >= 0 && index < fileRecords.size()) {
                ObjectNode fileRecord = JSONUtilities.getObjectElement(fileRecords, index);
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
        ObjectNode retrievalRecord = job.getRetrievalRecord();
        return retrievalRecord != null ? guessBetterFormat(job, retrievalRecord, bestFormat) : bestFormat;
    }

    static String guessBetterFormat(ImportingJob job, ObjectNode retrievalRecord, String bestFormat) {
        ArrayNode fileRecords = JSONUtilities.getArray(retrievalRecord, "files");
        return fileRecords != null ? guessBetterFormat(job, fileRecords, bestFormat) : bestFormat;
    }

    static String guessBetterFormat(ImportingJob job, ArrayNode fileRecords, String bestFormat) {
        if (bestFormat != null && fileRecords != null && fileRecords.size() > 0) {
            ObjectNode firstFileRecord = JSONUtilities.getObjectElement(fileRecords, 0);
            String encoding = getEncoding(firstFileRecord);
            String location = JSONUtilities.getString(firstFileRecord, "location", null);

            if (location != null) {
                File file = new File(job.getRawDataDir(), location);
                bestFormat = guessBetterFormat(file, encoding, bestFormat);
            }
        }
        return bestFormat;
    }

    static String guessBetterFormat(File file, String fileEncoding, String bestFormat) {
        if (bestFormat != null && file != null) {
            while (true) {
                String betterFormat = null;

                List<FormatGuesser> guessers = ImportingManager.formatToGuessers.get(bestFormat);
                if (guessers != null) {
                    for (FormatGuesser guesser : guessers) {
                        betterFormat = guesser.guess(file, fileEncoding, bestFormat);
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
        return bestFormat;
    }

    static String guessBetterFormat(File file, ObjectNode fileRecord) {
        String encoding = JSONUtilities.getString(fileRecord, "declaredEncoding", null);
        String bestFormat = ImportingManager.getFormat(file.getName(), JSONUtilities.getString(fileRecord, "declaredMimeType", null));
        bestFormat = bestFormat == null ? "text" : bestFormat;
        return guessBetterFormat(file, encoding, bestFormat);
    }

    static void rankFormats(ImportingJob job, final String bestFormat, ArrayNode rankedFormats) {
        final Map<String, String[]> formatToSegments = new HashMap<String, String[]>();

        boolean download = bestFormat == null ? true : ImportingManager.formatToRecord.get(bestFormat).download;

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
            rankedFormats.add(format);
        }
    }

    static public void previewParse(ImportingJob job, String format, ObjectNode optionObj, List<Exception> exceptions) {
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
                exceptions);

        job.project.update(); // update all internal models, indexes, caches, etc.
    }

    static public long createProject(
            final ImportingJob job,
            final String format,
            final ObjectNode optionObj,
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
            final ObjectNode optionObj,
            final List<Exception> exceptions,
            final Format record,
            final Project project) {
        ProjectMetadata pm = createProjectMetadata(optionObj);
        record.parser.parse(
                project,
                pm,
                job,
                job.getSelectedFileRecords(),
                format,
                -1,
                optionObj,
                exceptions);

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

    static public ProjectMetadata createProjectMetadata(ObjectNode optionObj) {
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
        pm.setTags(JSONUtilities.getStringArray(optionObj, "projectTags"));
        pm.setDescription(JSONUtilities.getString(optionObj, "projectDescription", ""));
        pm.setCreator(JSONUtilities.getString(optionObj, "projectCreator", ""));

        String encoding = JSONUtilities.getString(optionObj, "encoding", "UTF-8");
        if ("".equals(encoding)) {
            // encoding can be present, but empty, which won't trigger JSONUtilities default processing
            encoding = "UTF-8";
        }
        pm.setEncoding(encoding);
        return pm;
    }

    public static InputStreamReader getInputStreamReader(InputStream is, String encoding) throws IOException {
        if (encoding == null) {
            return new InputStreamReader(is);
        } else if (EncodingGuesser.UTF_8_BOM.equals(encoding)) { // Handle our fake UTF-8 with BOM encoding
            return new InputStreamReader(new UnicodeBOMInputStream(is, true), UTF_8);
        }
        return new InputStreamReader(is, encoding);
    }
}
