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

package com.google.refine.tests.model;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Cell;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.operations.OnError;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.refine.process.Process;
import com.google.refine.process.ProcessManager;
import com.google.refine.tests.RefineTest;


public class UrlFetchingTests extends RefineTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}}";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    private Project project;
    private Properties options;
    private JSONObject engine_config;

    @BeforeMethod
    public void SetUp() throws JSONException, IOException, ModelException {
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
        Assert.assertTrue(ref_val != "apple"); // just to make sure I picked the right column
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
        String jsonString = "[{\"name\": \"authorization\",\"value\": \""+authorizationValue+
                             "\"},{\"name\": \"user-agent\",\"value\": \""+userAgentValue+
                             "\"},{\"name\": \"accept\",\"value\": \""+acceptValue+"\"}]";

        JSONArray httpHeadersJson = new JSONArray(jsonString);

        EngineDependentOperation op = new ColumnAdditionByFetchingURLsOperation(engine_config,
            "fruits",
            "value",
            OnError.StoreError,
            "junk",
            1,
            50,
            true,
            httpHeadersJson);
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
        JSONObject headersUsed = null;
        
        // sometime, we got response: 
        // Error
        // Over Quota
        // This application is temporarily over its serving quota. Please try again later.
        try { 
            headersUsed = new JSONObject(project.rows.get(0).getCellValue(newCol).toString());
        } catch (JSONException ex) {
            return;
        }
        // Inspect the results we got from remote service
        Assert.assertEquals(headersUsed.getString("User-Agent"), userAgentValue);
        Assert.assertEquals(headersUsed.getString("Authorization"), authorizationValue);
        Assert.assertEquals(headersUsed.getString("Accept"), acceptValue);
    }

}
