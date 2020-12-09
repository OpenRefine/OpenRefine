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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import org.openrefine.RefineTest;
import org.openrefine.browsing.EngineConfig;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.expr.MetaParser;
import org.openrefine.grel.Parser;
import org.openrefine.model.Cell;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.ModelException;
import org.openrefine.model.Project;
import org.openrefine.model.Row;
import org.openrefine.operations.Operation;
import org.openrefine.operations.EngineDependentOperation;
import org.openrefine.operations.OnError;
import org.openrefine.operations.OperationRegistry;
import org.openrefine.operations.column.ColumnAdditionByFetchingURLsOperation;
import org.openrefine.operations.column.ColumnAdditionByFetchingURLsOperation.HttpHeader;
import org.openrefine.process.Process;
import org.openrefine.process.ProcessManager;
import org.openrefine.util.ParsingUtilities;
import org.openrefine.util.TestUtils;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;


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
            +"{\n" + 
            "    \"description\" : \"Create column employments at index 2 by fetching URLs based on column orcid using expression grel:\\\"https://pub.orcid.org/\\\"+value+\\\"/employments\\\"\",\n" + 
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
        Assert.assertFalse(process.isRunning());
    }

    @Test
    public void serializeColumnAdditionByFetchingURLsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnAdditionByFetchingURLsOperation.class), json, ParsingUtilities.defaultWriter);
    }
    
    @Test
    public void serializeUrlFetchingProcess() throws Exception {
    	project = createProject("UrlFetchingTests",
        		new String[] {"foo"},
        		new Serializable[][] {
        	{"bar"}
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
                            new String[] {"fruits"},
                            new Serializable[][] {
                    {"apple"},
                    {"apple"},
                    {"orange"},
                    {"orange"},
                    {"orange"},
                    {"orange"}
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
            String refVal = (String)rows.get(0).getRow().getCellValue(1).toString();
            Assert.assertFalse(refVal.equals("apple")); // just to make sure I picked the right column
            Assert.assertEquals(rows.get(1).getRow().getCellValue(1).toString(), refVal);
            server.shutdown();
        }
    }

    
    /**
     * Fetch invalid URLs
     * https://github.com/OpenRefine/OpenRefine/issues/1219
     */
    @Test
    public void testInvalidUrl() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/random");
            server.enqueue(new MockResponse());

            project = createProject("UrlFetchingTests",
                            new String[] {"fruits"},
                            new Serializable[][] {
                    {"auinrestrsc"}, // malformed -> null
                    {"https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain"}, // fine
                    {"http://anursiebcuiesldcresturce.detur/anusclbc"} // well-formed but invalid
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

            runAndWait(op, 3000);

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
                            new String[] {"fruits"},
                            new Serializable[][] {
                    {url.toString()}
            });

            String userAgentValue =  "OpenRefine";
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

            runAndWait(op, 3000);

            // Inspect rows
            List<IndexedRow> rows = project.getCurrentGridState().collectRows();
            Assert.assertEquals(rows.get(0).getRow().getCellValue(1), null);
            Assert.assertTrue(rows.get(1).getRow().getCellValue(1) != null);
            Assert.assertTrue(ExpressionUtils.isError(rows.get(2).getRow().getCellValue(1)));

            RecordedRequest request = server.takeRequest();
            Assert.assertEquals(request.getHeader("user-agent"), userAgentValue);
            Assert.assertEquals(request.getHeader("authorization"), authorizationValue);
            Assert.assertEquals(request.getHeader("accept"), acceptValue);
                server.shutdown();
        }
    }


}
