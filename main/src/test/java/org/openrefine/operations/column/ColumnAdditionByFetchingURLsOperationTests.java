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

package org.openrefine.operations.column;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OnError;
import org.openrefine.operations.Operation;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.column.ColumnAdditionByFetchingURLsOperation.HttpHeader;
import org.openrefine.process.LongRunningProcessStub;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;

public class ColumnAdditionByFetchingURLsOperationTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}";

    // This is only used for serialization tests. The URL is never fetched.
    private String json = "{\"op\":\"core/column-addition-by-fetching-urls\","
            + "\"description\":\"Create column employments at index 2 by fetching URLs based on column orcid using expression grel:\\\"https://pub.orcid.org/\\\"+value+\\\"/employments\\\"\","
            + "\"engineConfig\":{\"mode\":\"row-based\",\"facets\":[]},"
            + "\"newColumnName\":\"employments\","
            + "\"columnInsertIndex\":2,"
            + "\"baseColumnName\":\"orcid\","
            + "\"urlExpression\":\"grel:\\\"https://pub.orcid.org/\\\"+value+\\\"/employments\\\"\","
            + "\"onError\":\"set-to-blank\","
            + "\"delay\":500,"
            + "\"cacheResponses\":true,"
            + "\"httpHeadersJson\":["
            + "    {\"name\":\"authorization\",\"value\":\"\"},"
            + "    {\"name\":\"user-agent\",\"value\":\"OpenRefine 3.0 rc.1 [TRUNK]\"},"
            + "    {\"name\":\"accept\",\"value\":\"application/json\"}"
            + "]}";

    private String processJson = ""
            + "{\n" +
            "    \"description\" : \"Create column employments at index 2 by fetching URLs based on column orcid using expression grel:\\\"https://pub.orcid.org/\\\"+value+\\\"/employments\\\"\",\n"
            +
            "    \"id\" : %d,\n" +
            "    \"immediate\" : false,\n" +
            "    \"progress\" : 0,\n" +
            "    \"status\" : \"pending\"\n" +
            " }";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation("core", "column-addition-by-fetching-urls", ColumnAdditionByFetchingURLsOperation.class);
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    // dependencies
    private Project project;
    private Properties options;
    private EngineConfig engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    private void runAndWait(EngineDependentOperation op, int timeout) throws Exception {
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project.getHistory(), project.getProcessManager());
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        int time = 0;
        try {
            while (process.isRunning() && time < timeout) {
                Thread.sleep(200);
                time += 200;
            }
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning(), "Process failed to complete within timeout " + timeout);
    }

    @Test
    public void serializeColumnAdditionByFetchingURLsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnAdditionByFetchingURLsOperation.class), json,
                ParsingUtilities.defaultWriter);
    }

    @Test
    public void serializeUrlFetchingProcess() throws Exception {
        project = createProject("UrlFetchingTests",
                new String[] { "foo" },
                new Serializable[][] {
                        { "bar" }
                });
        Operation op = ParsingUtilities.mapper.readValue(json, ColumnAdditionByFetchingURLsOperation.class);
        Process process = op.createProcess(project.getHistory(), project.getProcessManager());
        TestUtils.isSerializedTo(process, String.format(processJson, process.hashCode()), ParsingUtilities.defaultWriter);
    }

    /**
     * Test for caching
     */
    @Test
    public void testUrlCaching() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/random");

            project = createProject("UrlFetchingTests",
                    new String[] { "fruits" },
                    new Serializable[][] {
                            { "apple" },
                            { "apple" },
                            { "orange" },
                            { "orange" },
                            { "orange" },
                            { "orange" }
                    });

            Random rand = new Random();
            for (int i = 0; i < 6; i++) {
                // We won't need them all, but queue 6 random responses
                server.enqueue(new MockResponse().setBody(Integer.toString(rand.nextInt(100))));
            }

            EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                    "fruits",
                    "\"" + url + "?city=\"+value",
                    OnError.StoreError,
                    "rand",
                    1,
                    500,
                    true,
                    null);

            // We have 6 rows and 500 ms per row but only two distinct
            // values so we should not wait much more than ~1000 ms to get the
            // results.
            runAndWait(op, 1500);

            // Inspect rows
            List<IndexedRow> rows = project.getCurrentGridState().collectRows();
            String refVal = (String) rows.get(0).getRow().getCellValue(1).toString();
            Assert.assertEquals(rows.get(1).getRow().getCellValue(1).toString(), refVal);
            server.shutdown();
        }
    }

    /**
     * Fetch invalid URLs https://github.com/OpenRefine/OpenRefine/issues/1219
     */
    @Test
    public void testInvalidUrl() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/random");
            server.enqueue(new MockResponse());
            HttpUrl url404 = server.url("/404");
            server.enqueue(new MockResponse().setResponseCode(404).setBody("not found").setStatus("NOT FOUND"));

            project = createProject("UrlFetchingTests",
                    new String[] { "fruits" },
                    new Serializable[][] {
                            { "malformed" }, // malformed -> null
                            { url.toString() }, // fine
                            { url404.toString() } // well-formed but invalid
                    });

            EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                    "fruits",
                    "value",
                    OnError.StoreError,
                    "junk",
                    1,
                    50,
                    true,
                    null);

            LongRunningProcessStub process = new LongRunningProcessStub(
                    op.createProcess(project.getHistory(), project.getProcessManager()));
            process.run();

            GridState grid = project.getCurrentGridState();
            int newCol = 1;
            // Inspect rows
            List<Row> rows = grid.collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
            Assert.assertEquals(rows.get(0).getCellValue(newCol), null);
            Assert.assertTrue(rows.get(1).getCellValue(newCol) != null);
            Assert.assertTrue(ExpressionUtils.isError(rows.get(2).getCellValue(newCol)));
        }
    }

    @Test
    public void testHttpHeaders() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/checkheader");

            project = createProject("UrlFetchingTests",
                    new String[] { "fruits" },
                    new Serializable[][] {
                            { url.toString() }
                    });

            String userAgentValue = "OpenRefine";
            String authorizationValue = "Basic";
            String acceptValue = "*/*";
            List<HttpHeader> headers = new ArrayList<>();
            headers.add(new HttpHeader("authorization", authorizationValue));
            headers.add(new HttpHeader("user-agent", userAgentValue));
            headers.add(new HttpHeader("accept", acceptValue));

            server.enqueue(new MockResponse().setBody("first"));
            server.enqueue(new MockResponse().setBody("second"));

            EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                    "fruits",
                    "value",
                    OnError.StoreError,
                    "junk",
                    1,
                    50,
                    true,
                    headers);
            LongRunningProcessStub process = new LongRunningProcessStub(
                    op.createProcess(project.getHistory(), project.getProcessManager()));
            process.run();

            // Inspect rows
            List<IndexedRow> rows = project.getCurrentGridState().collectRows();
            Assert.assertEquals(rows.get(0).getRow().getCellValue(1), "first");

            RecordedRequest request = server.takeRequest();
            Assert.assertEquals(request.getHeader("user-agent"), userAgentValue);
            Assert.assertEquals(request.getHeader("authorization"), authorizationValue);
            Assert.assertEquals(request.getHeader("accept"), acceptValue);
            server.shutdown();
        }
    }

    @Test
    public void testRetries() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/retries");

            project = createProject(new String[] { "fruits" },
                    new Serializable[][] {
                            { "test1" }, { "test2" }, { "test3" }
                    });

            // Queue 5 error responses with 1 sec. Retry-After interval
            for (int i = 0; i < 5; i++) {
                server.enqueue(new MockResponse()
                        .setHeader("Retry-After", 1)
                        .setResponseCode(429)
                        .setBody(Integer.toString(i, 10)));
            }

            server.enqueue(new MockResponse().setBody("success"));

            EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                    "fruits",
                    "\"" + url + "?city=\"+value",
                    OnError.StoreError,
                    "rand",
                    1,
                    100,
                    false,
                    null);

            // 6 requests (4 retries @1 sec) + final response
            long start = System.currentTimeMillis();
            LongRunningProcessStub process = new LongRunningProcessStub(
                    op.createProcess(project.getHistory(), project.getProcessManager()));
            process.run();

            // Make sure that our Retry-After headers were obeyed (4*1 sec vs 4*100msec)
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed > 4000, "Retry-After retries didn't take long enough - elapsed = " + elapsed);

            // 1st row fails after 4 tries (3 retries), 2nd row tries twice and gets value
            List<Row> rows = project.getCurrentGridState().collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
            assertTrue(rows.get(0).getCellValue(1).toString().contains("HTTP error 429"), "missing 429 error");
            assertEquals(rows.get(1).getCellValue(1).toString(), "success");

            server.shutdown();
        }
    }

    @Test
    public void testExponentialRetries() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/retries");

            project = createProject(new String[] { "fruits" },
                    new Serializable[][] {
                            { "test1" }, { "test2" }, { "test3" }
                    });

            // Use 503 Server Unavailable with no Retry-After header this time
            for (int i = 0; i < 5; i++) {
                server.enqueue(new MockResponse()
                        .setResponseCode(503)
                        .setBody(Integer.toString(i, 10)));
            }
            server.enqueue(new MockResponse().setBody("success"));

            server.enqueue(new MockResponse().setBody("not found").setResponseCode(404));

            ColumnAdditionByFetchingURLsOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                    "fruits",
                    "\"" + url + "?city=\"+value",
                    OnError.StoreError,
                    "rand",
                    1,
                    100,
                    false,
                    null);

            // 6 requests (4 retries 200, 400, 800, 200 msec) + final response
            long start = System.currentTimeMillis();
            LongRunningProcessStub process = new LongRunningProcessStub(
                    op.createProcess(project.getHistory(), project.getProcessManager()));
            process.run();

            // Make sure that our exponential back off is working
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed > 1600, "Exponential retries didn't take enough time - elapsed = " + elapsed);

            // 1st row fails after 4 tries (3 retries), 2nd row tries twice and gets value, 3rd row is hard error
            List<Row> rows = project.getCurrentGridState().collectRows().stream().map(IndexedRow::getRow).collect(Collectors.toList());
            assertTrue(rows.get(0).getCellValue(1).toString().contains("HTTP error 503"), "Missing 503 error");
            assertEquals(rows.get(1).getCellValue(1).toString(), "success");
            assertTrue(rows.get(2).getCellValue(1).toString().contains("HTTP error 404"), "Missing 404 error");

            server.shutdown();
        }
    }

}
