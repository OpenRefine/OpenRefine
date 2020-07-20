/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
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

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Properties;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.ImporterTest;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.when;

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
    
    private ObjectNode getNestedOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        path.add("results");
        path.add("result");
        
        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }

    @Test
    public void urlImporting() throws IOException {

        String RESPONSE_BODY = "{code:401,message:Unauthorised}";
        String MESSAGE = String.format("HTTP error %d : %s | %s", 401,
                "Client Error", RESPONSE_BODY);

        MockWebServer server = new MockWebServer();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setBody(RESPONSE_BODY);
        mockResponse.setResponseCode(401);
        server.start();
        server.enqueue(mockResponse);
        HttpUrl url = server.url("/random");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        StringBody stringBody = new StringBody(url.toString(), ContentType.MULTIPART_FORM_DATA);
        builder = builder.addPart("download", stringBody);
        HttpEntity entity = builder.build();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        entity.writeTo(os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
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
            Assert.fail("No Exception was thrown");
        } catch (Exception exception) {
            Assert.assertEquals(MESSAGE, exception.getMessage());
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

    }
}
