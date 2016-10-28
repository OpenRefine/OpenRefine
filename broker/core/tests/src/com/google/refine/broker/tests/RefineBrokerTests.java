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

package com.google.refine.broker.tests;

import static com.google.refine.broker.RefineBroker.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.broker.RefineBroker;
import com.google.refine.broker.RefineBrokerImpl;

public class RefineBrokerTests {

    Logger logger;
    File data;
    
    @BeforeSuite public void suite_init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        data = new File("data");
        if (!data.exists()) data.mkdirs();
    }

    @AfterSuite public void suite_destroy() {
        for (File f : data.listFiles()) {
            f.delete();
        }
        data.delete();
    }

    // ------------------------------------------------------------------------------------
    
    ServletConfig config = null;
    RefineBroker broker = null;
    
    @BeforeTest public void test_init() throws Exception {
        logger = LoggerFactory.getLogger(this.getClass());
        config = mock(ServletConfig.class);
        when(config.getInitParameter("refine.data")).thenReturn(data.getAbsolutePath());
        when(config.getInitParameter("refine.development")).thenReturn("true");

        broker = new RefineBrokerImpl();
        broker.init(config);
    }

    @AfterTest public void test_destroy() throws Exception {
        broker.destroy();
        broker = null;
        config = null;
    }
    
    // ------------------------------------------------------------------------------------

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    StringWriter writer = null;
    
    @BeforeMethod public void setup() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @AfterMethod public void teardown() throws Exception {
        response = null;
        request = null;
    }
    
    // ------------------------------------------------------------------------------------

    @Test public void testLifeCycle() {
        Assert.assertTrue(true);
    }

    @Test public void testService() {
        try {
            success(broker, request, response, EXPIRE);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testObtainLockFailure() {
        try {
            failure(broker, request, response, OBTAIN_LOCK);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testReleaseLockFailure() {
        try {
            failure(broker, request, response, RELEASE_LOCK);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testGetStateFailure() {
        try {
            failure(broker, request, response, GET_STATE, "pid", "project1934983948", "uid", "testuser", "rev", "0");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testBrokenAllLockFailure() {
        try {
            failure(broker, request, response, OBTAIN_LOCK, "pid", "project", "uid", "testuser", "locktype", Integer.toString(ALL), "lockvalue", "1");
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    @Test public void testBrokenColLockFailure() {
        try {
            failure(broker, request, response, OBTAIN_LOCK, "pid", "project", "uid", "testuser", "locktype", Integer.toString(COL), "lockvalue", "1,1");
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    @Test public void testBrokenCellLockFailure() {
        try {
            failure(broker, request, response, OBTAIN_LOCK, "pid", "project", "uid", "testuser", "locktype", Integer.toString(CELL), "lockvalue", "1");
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    @Test public void testLockSimple() {
        String project = "proj0";
        String user = "testuser";
        
        try {
            logger.info("--- obtain ALL lock on project ---");
            JSONObject result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            assertJSON(result, "uid", "testuser");
            String lock = result.getString("lock");
    
            logger.info("--- obtain ALL lock on project ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);

            logger.info("--- obtain COL lock on project ---");
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", "1");
            assertJSON(result, "uid", "testuser");
            lock = result.getString("lock");
    
            logger.info("--- release COL lock on project ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);

            logger.info("--- obtain CELL lock on project ---");
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(CELL), "lockvalue", "1,1");
            assertJSON(result, "uid", "testuser");
            lock = result.getString("lock");
            
            logger.info("--- release CELL lock on project ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    @Test public void testLocksAllBlocks() {
        String project = "proj1";
        String user = "testuser";
        String user2 = "testuser2";

        try {
            logger.info("--- obtain ALL lock on project ---");
            JSONObject result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            assertJSON(result, "uid", user);
            String lock = result.getString("lock");

            logger.info("--- another using asking for any lock will fail ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(ALL), "lockvalue", "");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", "1");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(CELL), "lockvalue", "1,1");

            logger.info("--- same user asking for lower capable locks will return the ALL one ---");
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", "1");
            String lock2 = result.getString("lock");
            Assert.assertEquals(lock, lock2);
            
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(CELL), "lockvalue", "1,1");
            lock2 = result.getString("lock");
            Assert.assertEquals(lock, lock2);
            
            logger.info("--- release the ALL lock ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testLocksColBlocks() {
        String project = "proj2";
        String user = "testuser";
        String user2 = "testuser2";

        try {
            logger.info("--- obtain COL lock on project ---");
            JSONObject result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", "1");
            String lock = result.getString("lock");

            logger.info("--- other user must fail to obtain lock on the same COL or ALL ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(ALL), "lockvalue", "");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", "1");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(CELL), "lockvalue", "1,1");
            
            logger.info("--- but succeed in getting a COL lock on another column or cell ---");
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", "2");
            String lock2 = result.getString("lock");
            
            logger.info("--- now it's our first user's turn to fail to get lock ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", "2");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(CELL), "lockvalue", "2,1");
            
            logger.info("--- release the locks ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user2, "lock", lock2);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test public void testLocksCellBlocks() {
        String project = "proj3";
        String user = "testuser";
        String user2 = "testuser2";

        try {
            logger.info("--- obtain CELL lock on project ---");
            JSONObject result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(CELL), "lockvalue", "1,1");
            String lock = result.getString("lock");

            logger.info("--- other user must fail to obtain lock on the same CELL, COL or ALL ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(ALL), "lockvalue", "");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", "1");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(CELL), "lockvalue", "1,1");
            
            logger.info("--- but succeed in getting a CELL lock on a cell in another column ---");
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(CELL), "lockvalue", "2,1");
            String lock2 = result.getString("lock");
            
            logger.info("--- now it's our first user's turn to fail to get lock ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", "2");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(CELL), "lockvalue", "2,1");
            
            logger.info("--- release the locks ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user2, "lock", lock2);
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    @Test public void testCompleteProjectLifeCycle() {
        try {
            String project = "proj4";
            String user = "testuser";
            String user2 = "testuser2";
            String data = "blah";
            String metadata = "{}";
            String transformations = "[]";
            String rev = "0";
            
            logger.info("--- obtain ALL lock on project ---");
            JSONObject result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            assertJSON(result, "uid", user);
            String lock = result.getString("lock");

            logger.info("--- start project ---");
            success(broker, request, response, START, "pid", project, "uid", user, "lock", lock, "data", data, "metadata", metadata, "transformations", transformations);

            logger.info("--- verify project state contains lock ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", rev);
            JSONArray locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 1);
            JSONObject l = locks.getJSONObject(0);
            assertJSON(l, "uid", "testuser");
            Assert.assertEquals(l.getInt("type"), ALL);
            
            logger.info("--- release ALL lock on project ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
            
            logger.info("--- verify no locks are present ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", rev);
            locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 0);
            
            logger.info("--- open project and verify data was loaded correctly ---");
            result = success(broker, request, response, OPEN, "pid", project, "uid", user, "rev", rev);
            JSONArray result_data = result.getJSONArray("data");
            Assert.assertEquals(result_data.length(),data.getBytes("UTF-8").length);

            JSONArray tt;
            JSONObject t;
            
            logger.info("--- obtain column lock ---");
            String column = "1";
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(COL), "lockvalue", column);
            String col_lock = result.getString("lock");

            logger.info("--- perform column transformation ---");
            t = new JSONObject();
            t.put("op_type", COL);
            t.put("op_value", column); // operate on col 1
            t.put("value", new JSONObject());
            tt = new JSONArray();
            tt.put(t);
            result = success(broker, request, response, TRANSFORM, "pid", project, "uid", user, "lock", col_lock, "transformations", tt.toString());

            logger.info("--- make sure transformation was recorded properly ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "0");
            tt = result.getJSONArray("transformations");
            Assert.assertEquals(tt.length(), 1);
            t = tt.getJSONObject(0);
            assertJSON(t, "op_value", column);

            logger.info("--- make sure revision numbers in state management work as expected ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "1");
            tt = result.getJSONArray("transformations");
            Assert.assertEquals(tt.length(), 0);
            
            logger.info("--- perform cell transformation ---");
            String cell = "1";
            t = new JSONObject();
            t.put("op_type", CELL);
            t.put("op_value", column + "," + cell); // operate on cell at row 1 column 1
            t.put("value", new JSONObject());
            tt = new JSONArray();
            tt.put(t);
            result = success(broker, request, response, TRANSFORM, "pid", project, "uid", user, "lock", col_lock, "transformations", tt.toString());
            
            logger.info("--- make sure transformation was recorded properly ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "0");
            tt = result.getJSONArray("transformations");
            Assert.assertEquals(tt.length(), 2);

            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "1");
            tt = result.getJSONArray("transformations");
            Assert.assertEquals(tt.length(), 1);
            t = tt.getJSONObject(0);
            assertJSON(t, "op_value", column + "," + cell);
            
            logger.info("--- make sure another user fails to acquire ALL lock ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(ALL), "lockvalue", "");

            logger.info("--- make sure another user fails to acquire COL lock on the same column ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", column);

            logger.info("--- make sure another user manages to acquire COL lock on another column ---");
            String column2 = "2";
            result = success(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user2, "locktype", Integer.toString(COL), "lockvalue", column2);
            String col_lock2 = result.getString("lock");
            
            logger.info("--- make sure that both locks are present ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "2");
            locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 2);
            
            logger.info("--- make sure we can't escalate our current COL lock to an ALL lock ---");
            failure(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            
            logger.info("--- release column locks ---");
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", col_lock);
            success(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user2, "lock", col_lock2);
            
            logger.info("--- make sure the project has no locks ---");
            result = success(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", "2");
            locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 0);
            
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    // ------------------------------------------------------------------------------------
    
    private void assertJSON(JSONObject o, String name, String value) throws JSONException {
        Assert.assertEquals(o.get(name), value);
    }
    
    private JSONObject success(RefineBroker broker, HttpServletRequest request, HttpServletResponse response, String service, String... params) throws Exception {
        return call(true, broker, request, response, service, params);
    }

    private JSONObject failure(RefineBroker broker, HttpServletRequest request, HttpServletResponse response, String service, String... params) throws Exception {
        return call(false, broker, request, response, service, params);
    }
    
    private JSONObject call(boolean successful, RefineBroker broker, HttpServletRequest request, HttpServletResponse response, String service, String... params) throws Exception {
        if (params != null) {
            for (int i = 0; i < params.length; ) {
                String name = params[i++];
                String value = params[i++];
                if ("data".equals(name)) {
                    final ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes("UTF-8"));
                    when(request.getInputStream()).thenReturn(new ServletInputStream() {
                        public int read() throws IOException {
                            return inputStream.read();
                        }
                    });                    
                } else {
                    when(request.getParameter(name)).thenReturn(value);
                }
            }
        }

        StringWriter writer = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(writer));
        
        broker.process(service, request, response);

        JSONObject result = new JSONObject(writer.toString());
        
        if (successful) {
            assertJSON(result, "status", "ok");
        } else {
            assertJSON(result, "status", "error");
        }
        
        logger.info(result.toString());

        return result;
    }
}
