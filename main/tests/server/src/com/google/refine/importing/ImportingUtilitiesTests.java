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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
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
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okhttp3.HttpUrl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FileSystem;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.entity.mime.FileBody;
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
import com.google.refine.importers.LineBasedFormatGuesser;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importers.TextFormatGuesser;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp() {
        super.setUp();
        importFlowSettings();
    }

    @Test
    public void createProjectMetadataTest()
            throws Exception {
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                "{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
        ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        assertEquals(pm.getName(), "acme");
        assertEquals(pm.getEncoding(), "UTF-8");
        assertEquals(pm.getTags().length, 0);
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
            assertEquals(result, urlPathFixed);
        } else {
            assertEquals(result, urlPath);
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
            assertEquals(result, urlPathFixed);
        } else {
            assertEquals(result, urlPath);
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
        MockResponse mockResponse = new MockResponse.Builder()
                .body(RESPONSE_BODY)
                .code(401)
                .build();
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
        try {
            ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord, getDummyProgress());
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

        try {
            ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord, getDummyProgress());
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

    @Test
    public void importZipArchive() throws IOException {
        importArchive("movies.zip", "application/zip");
    }

    @Test
    public void import7zArchive() throws IOException {
        importArchive("movies.7z", "application/x-7z-compressed");
    }

    /**
     * This tests both exploding a zip or 7z archive into it's constituent files as well as importing them all (both)
     * and making sure that the recording of archive names and file names works correctly.
     * <p>
     * It's kind of a lot to have in one test, but it's a sequence of steps that need to be done in order.
     *
     * @throws IOException
     */
    private void importArchive(String filename, String mimeType) throws IOException {
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        String suffix = filename.substring(filename.lastIndexOf(".") + 1);
        File tmp = File.createTempFile("openrefine-test-movies", suffix, job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord, "origin", "upload");
        JSONUtilities.safePut(fileRecord, "declaredEncoding", "UTF-8");

        JSONUtilities.safePut(fileRecord, "declaredMimeType", mimeType);
        JSONUtilities.safePut(fileRecord, "fileName", filename);
        JSONUtilities.safePut(fileRecord, "location", tmp.getName());

        assertTrue(ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, getDummyProgress()));
        assertEquals(fileRecords.size(), 2);
        assertEquals(fileRecords.get(0).get("fileName").asText(), "movies-condensed.tsv");
        assertEquals(fileRecords.get(0).get("archiveFileName").asText(), filename);
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
        assertEquals(project.rows.get(0).getCell(0).getValue(), filename);
        assertEquals(project.columnModel.columns.get(1).getName(), "File");
        assertEquals(project.rows.get(0).getCell(1).getValue(), "movies-condensed.tsv");
        assertEquals(project.columnModel.columns.get(2).getName(), "name");
        assertEquals(project.rows.get(0).getCell(2).getValue(), "Wayne's World");

        // Make sure we imported both files contained in the zip file
        assertEquals(project.rows.size(), 252);

        ArrayNode importOptionsArray = metadata.getImportOptionMetadata();
        assertEquals(importOptionsArray.size(), 2);
        ObjectNode importOptions = (ObjectNode) importOptionsArray.get(0);
        assertEquals(importOptions.get("archiveFileName").asText(), filename);
        assertEquals(importOptions.get("fileSource").asText(), "movies-condensed.tsv");
        assertTrue(importOptions.get("includeFileSources").asBoolean());
        assertTrue(importOptions.get("includeArchiveFileName").asBoolean());

        importOptions = (ObjectNode) importOptionsArray.get(1);
        assertEquals(importOptions.get("fileSource").asText(), "movies.tsv");
        assertEquals(importOptions.get("archiveFileName").asText(), filename);
    }

    /**
     * Test regression from issue 7314 to make sure we can import a compressed file which is NOT an archive.
     *
     * @throws IOException
     */
    @Test
    public void importCompressedNonArchive() throws IOException {
        String filename = "persons.csv.gz";
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        File tmp = File.createTempFile("openrefine-test-persons", ".csv.gz", job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        ArrayNode fileRecords = ParsingUtilities.mapper.createArrayNode();
        ObjectNode fileRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(fileRecord, "origin", "upload");
        JSONUtilities.safePut(fileRecord, "declaredEncoding", "UTF-8");
        JSONUtilities.safePut(fileRecord, "declaredMimeType", "application/gzip");
        JSONUtilities.safePut(fileRecord, "fileName", filename);
        JSONUtilities.safePut(fileRecord, "location", tmp.getName());

        // False return just means "not an archive"
        assertFalse(ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, getDummyProgress()));
        assertEquals(fileRecords.size(), 1);
        assertEquals(fileRecords.get(0).get("fileName").asText(), "persons.csv.gz");
        assertEquals(fileRecords.get(0).has("archiveFileName"), false);

        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(options, "separator", ",");
        JSONUtilities.safePut(options, "includeArchiveFileName", true); // not an archive, so ignored
        JSONUtilities.safePut(options, "includeFileSources", true);

        ImportingParserBase parser = new SeparatorBasedImporter();
        List<Exception> exceptions = new ArrayList<Exception>();
        parser.parse(
                project,
                metadata,
                job,
                JSONUtilities.getObjectList(fileRecords),
                "csv",
                -1,
                options,
                exceptions);
        assertEquals(exceptions.size(), 0);
        project.update();

        assertEquals(project.columnModel.columns.get(0).getName(), "File");
        assertEquals(project.rows.get(0).getCell(0).getValue(), "persons.csv.gz");
        assertEquals(project.columnModel.columns.get(1).getName(), "Name");
        assertEquals(project.rows.get(0).getCell(1).getValue(), "Person1");

        assertEquals(project.rows.size(), 3);

        ArrayNode importOptionsArray = metadata.getImportOptionMetadata();
        assertEquals(importOptionsArray.size(), 1);
        ObjectNode importOptions = (ObjectNode) importOptionsArray.get(0);
        assertEquals(importOptions.get("fileSource").asText(), "persons.csv.gz");
        assertTrue(importOptions.get("includeFileSources").asBoolean());
    }

    @Test
    public void importUnsupportedZipFile() throws IOException {
        for (String basename : new String[] { "unsupportedPPMD" }) {
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
                () -> ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, getDummyProgress()));
        assertThrows("Failed to throw for " + filename, FileUploadBase.InvalidContentTypeException.class,
                () -> ImportingUtilities.retrieveContentFromPostRequest(request,
                        new Properties(), job.getRawDataDir(), fileRecord, getDummyProgress()));
        assertThrows("Failed to throw for " + filename, IOException.class,
                () -> ImportingUtilities.loadDataAndPrepareJob(request, response, new Properties(), job, fileRecord));
    }

    @Test
    public void testImportCompressedFiles() throws IOException, URISyntaxException {
        final String FILENAME_BASE = "persons";
        final int LINES = 4;
        String[] suffixes = { "", ".csv.gz", ".csv.bz2", ".csv.Z", ".csv.lzma", ".csv.xz" };
        InputStreamReader reader = null;
        for (String suffix : suffixes) {
            String filename = FILENAME_BASE + suffix;
            Path filePath = Paths.get(ClassLoader.getSystemResource(filename).toURI());

            File tmp = File.createTempFile("openrefine-test-" + FILENAME_BASE, suffix, job.getRawDataDir());
            tmp.deleteOnExit();
            byte[] contents = Files.readAllBytes(filePath);
            Files.write(tmp.toPath(), contents);
            // Write two copies of the data for compressors which support concatenated streams
            boolean concatenationSupported = suffix.endsWith(".gz") || suffix.endsWith(".bz2") || suffix.endsWith(".xz");
            if (concatenationSupported) {
                Files.write(tmp.toPath(), contents, StandardOpenOption.APPEND);
            }

            File uncompressedFile = ImportingUtilities.uncompressFile(job.getRawDataDir(), tmp, "", "",
                    ParsingUtilities.mapper.createObjectNode(), getDummyProgress());
            Assert.assertNotNull(uncompressedFile, "Failed to open compressed file: " + filename);

            reader = new InputStreamReader(new FileInputStream(uncompressedFile), StandardCharsets.UTF_8);
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(reader);

            int linecount = concatenationSupported ? LINES * 2 : LINES;
            assertEquals(StreamSupport.stream(records.spliterator(), false).count(), linecount,
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
                { "persons.csv.Z", true },
                { "persons.csv.lzma", true },
                { "persons.csv.xz", true },
                { "movies.7z", true },
                { "unsupportedPPMD.zip", true },
        };
        for (Object[] test : cases) {
            assertEquals(ImportingUtilities.isCompressed(new File(ClassLoader.getSystemResource((String) test[0]).getFile())), test[1],
                    "Wrong value for isCompressed of: " + Arrays.toString(test));
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
            server.start();
            String url = server.url("input.csv ").toString();
            server.enqueue(new MockResponse.Builder()
                    .code(404)
                    .status("HTTP/1.1 404 Not Found")
                    .build());

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

            try {
                ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord,
                        getDummyProgress());
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

        assertEquals(ImportingUtilities.getFileName(fileRecord), fileName);
    }

    @Test
    public void testFormatForMultipleCSVFiles() throws IOException, FileUploadException {
        testMultipleFiles("birds", "food.small", ".csv", ContentType.create("text/csv"), "text/line-based/*sv");
    }

    @Test
    public void testFormatForMultipleTSVFiles() throws IOException, FileUploadException {
        testMultipleFiles("movies", "presidents", ".tsv", ContentType.create("text/tsv"), "text/line-based/*sv");
    }

    @Test
    public void testFormatForMultipleExcelFiles() throws IOException, FileUploadException {
        testMultipleFiles("dates", "excel95", ".xls",
                ContentType.create("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), "binary/text/xml/xls/xlsx");
    }

    @Test
    public void testFormatForMultipleJSONFiles() throws IOException, FileUploadException {
        testMultipleFiles("grid_small", "grid_small", ".json", ContentType.create("text/json"), "text/json");
    }

    @Test
    public void testFormatForMultipleODSFiles() throws IOException, FileUploadException {
        testMultipleFiles("films", "films", ".ods", ContentType.create("application/vnd.oasis.opendocument.spreadsheet"), "text/xml/ods");
    }

    private void testMultipleFiles(String file1, String file2, String fileSuffix, ContentType contentType, String expectedFormat)
            throws IOException, FileUploadException {

        String filepath1 = ClassLoader.getSystemResource(file1 + fileSuffix).getPath();
        String filepath2 = ClassLoader.getSystemResource(file2 + fileSuffix).getPath();

        File tmp1 = File.createTempFile("openrefine-test-" + file1, fileSuffix, job.getRawDataDir());
        tmp1.deleteOnExit();

        FileUtils.copyFile(new File(filepath1), tmp1);

        File tmp2 = File.createTempFile("openrefine-test-" + file2, fileSuffix, job.getRawDataDir());
        tmp2.deleteOnExit();

        FileUtils.copyFile(new File(filepath2), tmp2);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        FileBody fileBody1 = new FileBody(tmp1, contentType);
        FileBody fileBody2 = new FileBody(tmp2, contentType);
        builder = builder.addPart("upload", fileBody1);
        builder = builder.addPart("upload", fileBody2);

        HttpEntity entity = builder.build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn(entity.getContentType());
        when(req.getMethod()).thenReturn("POST");
        when(req.getContentLength()).thenReturn((int) entity.getContentLength());
        when(req.getInputStream()).thenReturn(new MockServletInputStream(is));

        ImportingJob job = ImportingManager.createJob();
        Map<String, String> parameters = ParsingUtilities.parseParameters(req);
        ObjectNode config = ParsingUtilities.mapper.createObjectNode();
        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(config, "retrievalRecord", retrievalRecord);
        JSONUtilities.safePut(config, "state", "loading-raw-data");

        ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord, getDummyProgress());

        assertEquals(expectedFormat, JSONUtilities.getArray(retrievalRecord, "files").get(0).get("format").asText());
        assertEquals(expectedFormat, JSONUtilities.getArray(retrievalRecord, "files").get(1).get("format").asText());
    }

    private ImportingUtilities.Progress getDummyProgress() {
        return new ImportingUtilities.Progress() {

            @Override
            public void setProgress(String message, int percent) {
            }

            @Override
            public boolean isCanceled() {
                return false;
            }
        };
    }

    // TODO: This isn't synchronized with what's actually used in production
    private void importFlowSettings() {
        // Register Format guessers
        ImportingManager.registerFormatGuesser("text", new TextFormatGuesser());
        ImportingManager.registerFormatGuesser("text/line-based", new LineBasedFormatGuesser());

        // Extension to format mappings
        ImportingManager.registerExtension(".txt", "text");
        ImportingManager.registerExtension(".csv", "text/line-based/*sv");
        ImportingManager.registerExtension(".tsv", "text/line-based/*sv");
        ImportingManager.registerExtension(".xml", "text/xml");
        ImportingManager.registerExtension(".atom", "text/xml");
        ImportingManager.registerExtension(".json", "text/json");
        ImportingManager.registerExtension(".js", "text/json");
        ImportingManager.registerExtension(".xls", "binary/text/xml/xls/xlsx");
        ImportingManager.registerExtension(".xlsx", "binary/text/xml/xls/xlsx");
        ImportingManager.registerExtension(".ods", "text/xml/ods");
        ImportingManager.registerExtension(".nt", "text/rdf/nt");
        ImportingManager.registerExtension(".ntriples", "text/rdf/nt");
        ImportingManager.registerExtension(".n3", "text/rdf/n3");
        ImportingManager.registerExtension(".ttl", "text/rdf/ttl");
        ImportingManager.registerExtension(".jsonld", "text/rdf/ld+json");
        ImportingManager.registerExtension(".rdf", "text/rdf/xml");
        ImportingManager.registerExtension(".marc", "text/marc");
        ImportingManager.registerExtension(".mrc", "text/marc");
        ImportingManager.registerExtension(".wiki", "text/wiki");

        // Mime type to format mappings
        ImportingManager.registerMimeType("text/plain", "text");
        ImportingManager.registerMimeType("text/csv", "text/line-based/*sv");
        ImportingManager.registerMimeType("text/x-csv", "text/line-based/*sv");
        ImportingManager.registerMimeType("text/tab-separated-value", "text/line-based/*sv");
        ImportingManager.registerMimeType("text/tab-separated-values", "text/line-based/*sv");
        ImportingManager.registerMimeType("text/fixed-width", "text/line-based/fixed-width");
        ImportingManager.registerMimeType("application/n-triples", "text/rdf/nt");
        ImportingManager.registerMimeType("text/n3", "text/rdf/n3");
        ImportingManager.registerMimeType("text/rdf+n3", "text/rdf/n3");
        ImportingManager.registerMimeType("text/turtle", "text/rdf/ttl");
        ImportingManager.registerMimeType("application/xml", "text/xml");
        ImportingManager.registerMimeType("text/xml", "text/xml");
        ImportingManager.registerMimeType("+xml", "text/xml"); // suffix will be tried only as fallback
        ImportingManager.registerMimeType("application/rdf+xml", "text/rdf/xml");
        ImportingManager.registerMimeType("application/ld+json", "text/rdf/ld+json");
        ImportingManager.registerMimeType("application/atom+xml", "text/xml");
        ImportingManager.registerMimeType("application/msexcel", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/x-msexcel", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/x-ms-excel", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/vnd.ms-excel", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/x-excel", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/xls", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/x-xls", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.template",
                "binary/text/xml/xls/xlsx");
        ImportingManager.registerMimeType("application/vnd.oasis.opendocument.spreadsheet", "text/xml/ods");
        ImportingManager.registerMimeType("application/json", "text/json");
        ImportingManager.registerMimeType("application/javascript", "text/json");
        ImportingManager.registerMimeType("text/json", "text/json");
        ImportingManager.registerMimeType("+json", "text/json"); // suffix will be tried only as fallback
        ImportingManager.registerMimeType("application/marc", "text/marc");
    }
}
