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

package com.google.refine.operations.column;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.RefineTest;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OnError;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation.HttpHeader;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.TestUtils;


public class ColumnAdditionByFetchingURLsOperationTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}";

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
        OperationRegistry.registerOperation(getCoreModule(), "column-addition-by-fetching-urls", ColumnAdditionByFetchingURLsOperation.class);
    }

    // dependencies
    private Project project;
    private Properties options;
    private EngineConfig engine_config = EngineConfig.reconstruct(ENGINE_JSON_URLS);

    @BeforeMethod
    public void SetUp() throws IOException, ModelException {
        project = createProjectWithColumns("UrlFetchingTests", "fruits");
    }

    private boolean isHostReachable(String host, int timeout){
        boolean state = false;

        try {
            state = InetAddress.getByName(host).isReachable(timeout);
        } catch (IOException e) {
//            e.printStackTrace();
        }

        return state;
    }

    @Test
    public void serializeColumnAdditionByFetchingURLsOperation() throws Exception {
        TestUtils.isSerializedTo(ParsingUtilities.mapper.readValue(json, ColumnAdditionByFetchingURLsOperation.class), json);
    }

    @Test
    public void serializeUrlFetchingProcess() throws Exception {
        AbstractOperation op = ParsingUtilities.mapper.readValue(json, ColumnAdditionByFetchingURLsOperation.class);
        Process process = op.createProcess(project, new Properties());
        TestUtils.isSerializedTo(process, String.format(processJson, process.hashCode()));
    }

    /**
     * Test for caching
     */

    @Test
    public void testUrlCaching() throws Exception {
        if (!isHostReachable("www.random.org", 5000))
            return;

        for (int i = 0; i < 100; i++) {
            Row row = new Row(2);
            row.setCell(0, new Cell(i < 5 ? "apple":"orange", null));
            project.rows.add(row);
        }

        EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                "fruits",
                "\"https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new&city=\"+value",
                OnError.StoreError,
                "rand",
                1,
                500,
                true,
                null);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            // We have 100 rows and 500 ms per row but only two distinct
            // values so we should not wait more than ~2000 ms to get the
            // results. Just to make sure the test passes with plenty of
            // net latency we sleep for longer (but still less than
            // 50,000ms).
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }


        // Inspect rows
        String ref_val = (String)project.rows.get(0).getCellValue(1).toString();
        if (ref_val.startsWith("HTTP error"))
            return;
        Assert.assertFalse(ref_val.equals("apple")); // just to make sure I picked the right column
        for (int i = 1; i < 4; i++) {
            System.out.println("value:" + project.rows.get(i).getCellValue(1));
            // all random values should be equal due to caching
            Assert.assertEquals(project.rows.get(i).getCellValue(1).toString(), ref_val);
        }
        Assert.assertFalse(process.isRunning());
    }

    /**
     * Fetch invalid URLs
     * https://github.com/OpenRefine/OpenRefine/issues/1219
     */
    @Test
    public void testInvalidUrl() throws Exception {
        Row row0 = new Row(2);
        row0.setCell(0, new Cell("auinrestrsc", null)); // malformed -> null
        project.rows.add(row0);
        Row row1 = new Row(2);
        row1.setCell(0, new Cell("https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain", null)); // fine
        project.rows.add(row1);
        Row row2 = new Row(2);
        row2.setCell(0, new Cell("http://anursiebcuiesldcresturce.detur/anusclbc", null)); // well-formed but invalid
        project.rows.add(row2);

        EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
                "fruits",
                "value",
                OnError.StoreError,
                "junk",
                1,
                50,
                true,
                null);

        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Assert.fail("Test interrupted");
        }
        Assert.assertFalse(process.isRunning());

        int newCol = project.columnModel.getColumnByName("junk").getCellIndex();
        // Inspect rows
        Assert.assertEquals(project.rows.get(0).getCellValue(newCol), null);
        Assert.assertTrue(project.rows.get(1).getCellValue(newCol) != null);
        Assert.assertTrue(ExpressionUtils.isError(project.rows.get(2).getCellValue(newCol)));
    }

    @Test
    public void testHttpHeaders() throws Exception {
        Row row0 = new Row(2);
        row0.setCell(0, new Cell("http://headers.jsontest.com", null));
        /*
        http://headers.jsontest.com is a service which returns the HTTP request headers
        as JSON. For example:
        {
           "X-Cloud-Trace-Context": "579a1a2ee5c778dfc0810a3bf131ba4e/11053223648711966807",
           "Authorization": "Basic",
           "Host": "headers.jsontest.com",
           "User-Agent": "OpenRefine",
           "Accept": "*"
        }
        */

        project.rows.add(row0);

        String userAgentValue =  "OpenRefine";
        String authorizationValue = "Basic";
        String acceptValue = "*/*";
        List<HttpHeader> headers = new ArrayList<>();
        headers.add(new HttpHeader("authorization", authorizationValue));
        headers.add(new HttpHeader("user-agent", userAgentValue));
        headers.add(new HttpHeader("accept", acceptValue));

        EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
            "fruits",
            "value",
            OnError.StoreError,
            "junk",
            1,
            50,
            true,
            headers);
        ProcessManager pm = project.getProcessManager();
        Process process = op.createProcess(project, options);
        process.startPerforming(pm);
        Assert.assertTrue(process.isRunning());
      /*  try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Assert.fail("Test interrupted");
            }
        //Assert.assertFalse(process.isRunning());

        int newCol = project.columnModel.getColumnByName("junk").getCellIndex();
        ObjectNode headersUsed = null;

        // sometime, we got response:
        // Error
        // Over Quota
        // This application is temporarily over its serving quota. Please try again later.
        try {
            String response = project.rows.get(0).getCellValue(newCol).toString();
            headersUsed = ParsingUtilities.mapper.readValue(response, ObjectNode.class);
        } catch (IOException ex) {
            return;
        }
        // Inspect the results we got from remote service
        Assert.assertEquals(headersUsed.get("user-agent").asText(), userAgentValue);
        Assert.assertEquals(headersUsed.get("authorization").asText(), authorizationValue);
        Assert.assertEquals(headersUsed.get("accept").asText(), acceptValue);*/
    }

}
