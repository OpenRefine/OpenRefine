/*******************************************************************************
 * Copyright (C) 2018, 2020 OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.importing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.StreamSupport;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterTest;
import com.google.refine.importers.ImportingParserBase;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingUtilities.Progress;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
    }

    @Test
    public void createProjectMetadataTest()
            throws Exception {
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                "{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
        ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        Assert.assertEquals(pm.getName(), "acme");
        Assert.assertEquals(pm.getEncoding(), "UTF-8");
        Assert.assertTrue(pm.getTags().length == 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testZipSlip() throws IOException {
        File tempDir = TestUtils.createTempDirectory("openrefine-zip-slip-test");
        // For CVE-2018-19859, issue #1840
        ImportingUtilities.allocateFile(tempDir, "../../tmp/script.sh");
    }

    @Test
    public void testAllocateFileDeduplication() throws IOException {
        // Test for comment https://github.com/OpenRefine/OpenRefine/issues/3043#issuecomment-671057317
        File tempDir = TestUtils.createTempDirectory("openrefine-allocate-file-test");
        File dirA = new File(tempDir, "a");
        dirA.mkdir();
        File conflicting = new File(dirA, "dummy");
        Assert.assertTrue(conflicting.createNewFile());

        File allocated = ImportingUtilities.allocateFile(dirA, ".././a/dummy");
        Assert.assertEquals(allocated, new File(dirA, "dummy-2"));
    }

    @Test
    public void testNormalizePath() {
        // Test for issue Unable to create a project from a URL on Windows if the URL path contains ":" character #4625
        // https://github.com/OpenRefine/OpenRefine/issues/4625
        String urlPath = "/a/b:c/dummy:test";
        String urlPathFixed = "\\a\\b-c\\dummy-test";
        String result = ImportingUtilities.normalizePath(urlPath);
        FileSystem fileSystem = FileSystem.getCurrent();
        if (fileSystem == FileSystem.WINDOWS) {
            Assert.assertEquals(urlPathFixed, result);
        } else {
            Assert.assertEquals(urlPath, result);
        }
    }

    @Test
    public void testNormalizePathWithDifferentSeparator() {
        // Test for issue Unable to create a project from a URL on Windows if the URL path contains ":" character #4625
        // https://github.com/OpenRefine/OpenRefine/issues/4625
        String urlPath = "\\a\\b:c\\dummy:test";
        String urlPathFixed = "\\a\\b-c\\dummy-test";
        String result = ImportingUtilities.normalizePath(urlPath);
        FileSystem fileSystem = FileSystem.getCurrent();
        if (fileSystem == FileSystem.WINDOWS) {
            Assert.assertEquals(urlPathFixed, result);
        } else {
            Assert.assertEquals(urlPath, result);
        }
    }

    @Test
    public void testAllocateFileWithIllegalCharInWindows() throws IOException {
        // Test for issue Unable to create a project from a URL on Windows if the URL path contains ":" character #4625
        // https://github.com/OpenRefine/OpenRefine/issues/4625
        File tempDir = TestUtils.createTempDirectory("openrefine-allocate-file-test");
        File dirA = new File(tempDir, "a");
        Assert.assertTrue(dirA.mkdir());
        String urlPath = ".././a/b:c/dummy:test";
        String urlPathFixed = ".././a/b-c/dummy-test";
        File allocated = ImportingUtilities.allocateFile(dirA, urlPath);
        FileSystem fileSystem = FileSystem.getCurrent();
        if (fileSystem == FileSystem.WINDOWS) {
            Assert.assertEquals(allocated, new File(dirA, urlPathFixed));
        } else {
            Assert.assertEquals(allocated, new File(dirA, urlPath));
        }
    }

    @Test
    public void testAllocateFileWithIllegalCharInWindowsDifferentSeparator() throws IOException {
        // Test for issue Unable to create a project from a URL on Windows if the URL path contains ":" character #4625
        // https://github.com/OpenRefine/OpenRefine/issues/4625
        File tempDir = TestUtils.createTempDirectory("openrefine-allocate-file-test");
        File dirA = new File(tempDir, "a");
        Assert.assertTrue(dirA.mkdir());
        String urlPath = "..\\.\\a\\b:c\\dummy:test";
        String urlPathFixed = "..\\.\\a\\b-c\\dummy-test";
        File allocated = ImportingUtilities.allocateFile(dirA, urlPath);
        FileSystem fileSystem = FileSystem.getCurrent();
        if (fileSystem == FileSystem.WINDOWS) {
            Assert.assertEquals(allocated, new File(dirA, urlPathFixed));
        } else {
            Assert.assertEquals(allocated, new File(dirA, urlPath));
        }
    }

    @Test
    public void urlImporting() throws IOException {

        String RESPONSE_BODY = "{code:401,message:Unauthorised}";

        MockWebServer server = new MockWebServer();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(RESPONSE_BODY);
        mockResponse.setResponseCode(401);
        server.start();
        server.enqueue(mockResponse);
        HttpUrl url = server.url("/random");
        String MESSAGE = String.format("HTTP error %d : %s for URL %s", 401,
                "Client Error", url);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        StringBody stringBody = new StringBody(url.toString(), ContentType.MULTIPART_FORM_DATA);
        builder = builder.addPart("download", stringBody);
        HttpEntity entity = builder.build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn(entity.getContentType());
        when(req.getParameter("download")).thenReturn(url.toString());
        when(req.getMethod()).thenReturn("POST");
        when(req.getContentLength()).thenReturn((int) entity.getContentLength());
        when(req.getInputStream()).thenReturn(new MockServletInputStream(is));

        ImportingJob job = ImportingManager.createJob();
        Map<String, String> parameters = ParsingUtilities.parseParameters(req);
        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
        try {
            ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord,
                    new ImportingUtilities.Progress() {

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
            fail("No Exception was thrown");
        } catch (Exception exception) {
            assertEquals(exception.getMessage(), MESSAGE);
        } finally {
            server.close();
        }
    }

    @Test
    public void urlImportingInvalidProtocol() throws IOException {

        String url = "file:///etc/passwd";
        String message = "Unsupported protocol: file";

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        StringBody stringBody = new StringBody(url.toString(), ContentType.MULTIPART_FORM_DATA);
        builder = builder.addPart("download", stringBody);
        HttpEntity entity = builder.build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn(entity.getContentType());
        when(req.getParameter("download")).thenReturn(url.toString());
        when(req.getMethod()).thenReturn("POST");
        when(req.getContentLength()).thenReturn((int) entity.getContentLength());
        when(req.getInputStream()).thenReturn(new MockServletInputStream(is));

        ImportingJob job = ImportingManager.createJob();
        Map<String, String> parameters = ParsingUtilities.parseParameters(req);
        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
        try {
            ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord,
                    new ImportingUtilities.Progress() {

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
            fail("No Exception was thrown");
        } catch (Exception exception) {
            assertEquals(exception.getMessage(), message);
        }
    }

    public static class MockServletInputStream extends ServletInputStream {

        private final InputStream delegate;

        public MockServletInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            return delegate.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }

    }

    /**
     * This tests both exploding a zip archive into it's constituent files as well as importing them all (both) and
     * making sure that the recording of archive names and file names works correctly.
     * <p>
     * It's kind of a lot to have in one test, but it's a sequence of steps that need to be done in order.
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void importArchive() throws IOException {
        String filename = "movies.zip";
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        File tmp = File.createTempFile("openrefine-test-movies", ".zip", job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        Progress dummyProgress = new Progress() {

            @Override
            public void setProgress(String message, int percent) {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };

        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord, "origin", "upload");
        JSONUtilities.safePut(fileRecord, "declaredEncoding", "UTF-8");
        JSONUtilities.safePut(fileRecord, "declaredMimeType", "application/x-zip-compressed");
        JSONUtilities.safePut(fileRecord, "fileName", filename);
        JSONUtilities.safePut(fileRecord, "location", tmp.getName());

        assertTrue(ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, dummyProgress));
        assertEquals(fileRecords.size(), 2);
        assertEquals(fileRecords.get(0).get("fileName").asText(), "movies-condensed.tsv");
        assertEquals(fileRecords.get(0).get("archiveFileName").asText(), "movies.zip");
        assertEquals(fileRecords.get(1).get("fileName").asText(), "movies.tsv");

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "includeArchiveFileName", true);
        JSONUtilities.safePut(options, "includeFileSources", true);

        ImportingParserBase parser = new SeparatorBasedImporter();
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parse(
                project,
                metadata,
                job,
                JSONUtilities.getObjectList(fileRecords),
                "tsv",
                -1,
                options,
                exceptions);
        assertEquals(exceptions.size(), 0);
        project.update();

        assertEquals(project.columnModel.columns.get(0).getName(), "Archive");
        assertEquals(project.rows.get(0).getCell(0).getValue(), "movies.zip");
        assertEquals(project.columnModel.columns.get(1).getName(), "File");
        assertEquals(project.rows.get(0).getCell(1).getValue(), "movies-condensed.tsv");
        assertEquals(project.columnModel.columns.get(2).getName(), "name");
        assertEquals(project.rows.get(0).getCell(2).getValue(), "Wayne's World");

        // Make sure we imported both files contained in the zip file
        assertEquals(project.rows.size(), 252);

        ArrayNode importOptionsArray = metadata.getImportOptionMetadata();
        assertEquals(importOptionsArray.size(), 2);
        ObjectNode importOptions = (ObjectNode) importOptionsArray.get(0);
        assertEquals(importOptions.get("archiveFileName").asText(), "movies.zip");
        assertEquals(importOptions.get("fileSource").asText(), "movies-condensed.tsv");
        assertTrue(importOptions.get("includeFileSources").asBoolean());
        assertTrue(importOptions.get("includeArchiveFileName").asBoolean());

        importOptions = (ObjectNode) importOptionsArray.get(1);
        assertEquals(importOptions.get("fileSource").asText(), "movies.tsv");
        assertEquals(importOptions.get("archiveFileName").asText(), "movies.zip");
    }

    @Test
    public void importUnsupportedZipFile() throws IOException {
        for (String basename : new String[] { "unsupportedPPMD", "notazip" }) {
            testInvalidZipFile(basename);
        }
    }

    private void testInvalidZipFile(String basename) throws IOException {
        String filename = basename + ".zip";
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        File tmp = File.createTempFile("openrefine-test-" + basename, ".zip", job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        Progress dummyProgress = new Progress() {

            @Override
            public void setProgress(String message, int percent) {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };

        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord, "origin", "upload");
        JSONUtilities.safePut(fileRecord, "declaredEncoding", "UTF-8");
        JSONUtilities.safePut(fileRecord, "declaredMimeType", "application/x-zip-compressed");
        JSONUtilities.safePut(fileRecord, "fileName", filename);
        JSONUtilities.safePut(fileRecord, "location", tmp.getName());

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThrows("Failed to throw for " + filename, IOException.class,
                () -> ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, dummyProgress));
        assertThrows("Failed to throw for " + filename, FileUploadBase.InvalidContentTypeException.class,
                () -> ImportingUtilities.retrieveContentFromPostRequest(request,
                        new Properties(), job.getRawDataDir(), fileRecord, dummyProgress));
        assertThrows("Failed to throw for " + filename, IOException.class,
                () -> ImportingUtilities.loadDataAndPrepareJob(request, response, new Properties(), job, fileRecord));
    }

    @Test
    public void testImportCompressedFiles() throws IOException, URISyntaxException {
        final String FILENAME_BASE = "persons";
        final int LINES = 4;
        String[] suffixes = { "", ".csv.gz", ".csv.bz2" };
        InputStreamReader reader = null;
        for (String suffix : suffixes) {
            String filename = FILENAME_BASE + suffix;
            Path filePath = Paths.get(ClassLoader.getSystemResource(filename).toURI());

            File tmp = File.createTempFile("openrefine-test-" + FILENAME_BASE, suffix, job.getRawDataDir());
            tmp.deleteOnExit();
            byte[] contents = Files.readAllBytes(filePath);
            Files.write(tmp.toPath(), contents);
            // Write two copies of the data to test reading concatenated streams
            Files.write(tmp.toPath(), contents, StandardOpenOption.APPEND);

            InputStream is = ImportingUtilities.tryOpenAsCompressedFile(tmp, null, null);
            Assert.assertNotNull(is, "Failed to open compressed file: " + filename);

            reader = new InputStreamReader(is); // TODO: This needs an encoding
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);

            Assert.assertEquals(StreamSupport.stream(records.spliterator(), false).count(), LINES * 2,
                    "row count mismatch for " + filename);
        }
        reader.close();
    }

    @Test
    public void testIsCompressedFile() throws IOException {
        Object[][] cases = {
                { "movies.tsv", false },
                { "persons.csv", false },
                { "persons.csv.gz", true },
                { "persons.csv.bz2", true },
                { "unsupportedPPMD.zip", true },
        };
        for (Object[] test : cases) {
            assertEquals(ImportingUtilities.isCompressed(new File(ClassLoader.getSystemResource((String) test[0]).getFile())), test[1],
                    "Wrong value for isCompressed of: " + test);
        }

    }

    /**
     * This test method is designed to validate the behavior of the system when a URL with a trailing space is used. It
     * simulates a scenario where a URL with a trailing space is used to retrieve content from a POST request. The
     * expected behavior is that the system should trim the URL and proceed with the request as normal.
     *
     * @throws IOException
     *             if an I/O error occurs during the test
     * @throws FileUploadException
     *             if a file upload error occurs during the test
     */
    @Test
    public void testTrailingSpaceInUrl() throws IOException, FileUploadException {
        try (MockWebServer server = new MockWebServer()) {
            String url = server.url("input.csv ").toString();
            server.enqueue(new MockResponse()
                    .setHttp2ErrorCode(404)
                    .setStatus("HTTP/1.1 404 Not Found"));

            String message = String.format("HTTP error %d : %s for URL %s", 404,
                    "Not Found", url.trim());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            StringBody stringBody = new StringBody(url, ContentType.MULTIPART_FORM_DATA);
            builder = builder.addPart("download", stringBody);
            HttpEntity entity = builder.build();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            entity.writeTo(os);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getContentType()).thenReturn(entity.getContentType());
            when(req.getParameter("download")).thenReturn(url);
            when(req.getMethod()).thenReturn("POST");
            when(req.getContentLength()).thenReturn((int) entity.getContentLength());
            when(req.getInputStream()).thenReturn(new MockServletInputStream(is));

            ImportingJob job = ImportingManager.createJob();
            Map<String, String> parameters = ParsingUtilities.parseParameters(req);
            ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
            Progress dummyProgress = new Progress() {

                @Override
                public void setProgress(String message, int percent) {
                }

                @Override
                public boolean isCanceled() {
                    return false;
                }
            };

            try {
                ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord, dummyProgress);
                fail("No Exception was thrown");
            } catch (ClientProtocolException exception) {
                assertEquals(exception.getMessage(), message);
            }
        }
    }

    @Test
    public void testGetFileName() {
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        String fileName = "aFileName";

        JSONUtilities.safePut(fileRecord, "fileName", fileName);

        assertEquals(fileName, ImportingUtilities.getFileName(fileRecord));
    }
}
