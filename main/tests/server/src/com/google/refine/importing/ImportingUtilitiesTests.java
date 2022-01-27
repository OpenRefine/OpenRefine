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
import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterTest;
import com.google.refine.importers.ImportingParserBase;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingUtilities.Progress;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp(){
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

    @Test(expectedExceptions=IllegalArgumentException.class)
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
        conflicting.createNewFile();
        
        File allocated = ImportingUtilities.allocateFile(dirA, ".././a/dummy");
        Assert.assertEquals(allocated, new File(dirA, "dummy-2"));
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
        when(req.getContentType()).thenReturn(entity.getContentType().getValue());
        when(req.getParameter("download")).thenReturn(url.toString());
        when(req.getMethod()).thenReturn("POST");
        when(req.getContentLength()).thenReturn((int) entity.getContentLength());
        when(req.getInputStream()).thenReturn(new MockServletInputStream(is));


        ImportingJob job = ImportingManager.createJob();
        Properties parameters = ParsingUtilities.parseUrlParameters(req);
        ObjectNode retrievalRecord = ParsingUtilities.mapper.createObjectNode();
        ObjectNode progress = ParsingUtilities.mapper.createObjectNode();
        try {
            ImportingUtilities.retrieveContentFromPostRequest(req, parameters, job.getRawDataDir(), retrievalRecord, new ImportingUtilities.Progress() {
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
     * This tests both exploding a zip archive into it's constituent files
     * as well as importing them all (both) and making sure that the
     * recording of archive names and file names works correctly.
     *
     * It's kind of a lot to have in one test, but it's a sequence
     * of steps that need to be done in order.
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void importArchive() throws IOException{
        String filename = "movies.zip";
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        File tmp = File.createTempFile("openrefine-test-movies", ".zip", job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        Progress dummyProgress = new Progress() {
            @Override
            public void setProgress(String message, int percent) {}

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
                IteratorUtils.toList(fileRecords.iterator()),
                "tsv",
                -1,
                options,
                exceptions
                );
        assertEquals(exceptions.size(), 0);
        project.update();

        assertEquals(project.columnModel.columns.get(0).getName(),"Archive");
        assertEquals(project.rows.get(0).getCell(0).getValue(),"movies.zip");
        assertEquals(project.columnModel.columns.get(1).getName(),"File");
        assertEquals(project.rows.get(0).getCell(1).getValue(),"movies-condensed.tsv");
        assertEquals(project.columnModel.columns.get(2).getName(),"name");
        assertEquals(project.rows.get(0).getCell(2).getValue(),"Wayne's World");

        // Make sure we imported both files contained in the zip file
        assertEquals(project.rows.size(), 252);

        ArrayNode importOptionsArray = metadata.getImportOptionMetadata();
        assertEquals(importOptionsArray.size(), 2);
        ObjectNode importOptions = (ObjectNode)importOptionsArray.get(0);
        assertEquals(importOptions.get("archiveFileName").asText(), "movies.zip");
        assertEquals(importOptions.get("fileSource").asText(), "movies-condensed.tsv");
        assertTrue(importOptions.get("includeFileSources").asBoolean());
        assertTrue(importOptions.get("includeArchiveFileName").asBoolean());

        importOptions = (ObjectNode)importOptionsArray.get(1);
        assertEquals(importOptions.get("fileSource").asText(), "movies.tsv");
        assertEquals(importOptions.get("archiveFileName").asText(), "movies.zip");
    }

    @Test
    public void importUnsupportedZipFile() throws IOException{
        String filename = "unsupportedPPMD.zip";
        String filepath = ClassLoader.getSystemResource(filename).getPath();
        // Make a copy in our data directory where it's expected
        File tmp = File.createTempFile("openrefine-test-unsupportedPPMD", ".zip", job.getRawDataDir());
        tmp.deleteOnExit();
        FileUtils.copyFile(new File(filepath), tmp);

        Progress dummyProgress = new Progress() {
            @Override
            public void setProgress(String message, int percent) {}

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

        assertThrows(IOException.class, () -> ImportingUtilities.postProcessRetrievedFile(job.getRawDataDir(), tmp, fileRecord, fileRecords, dummyProgress));
        assertThrows(FileUploadBase.InvalidContentTypeException.class, () -> ImportingUtilities.retrieveContentFromPostRequest(request, new Properties(), job.getRawDataDir(), fileRecord, dummyProgress));
        assertThrows(IOException.class, () -> ImportingUtilities.loadDataAndPrepareJob(request, response, new Properties(), job, fileRecord));

    }

}
