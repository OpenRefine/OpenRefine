package com.metaweb.gridworks.broker.tests;

import static com.metaweb.gridworks.broker.GridworksBroker.ALL;
import static com.metaweb.gridworks.broker.GridworksBroker.EXPIRE;
import static com.metaweb.gridworks.broker.GridworksBroker.GET_STATE;
import static com.metaweb.gridworks.broker.GridworksBroker.OBTAIN_LOCK;
import static com.metaweb.gridworks.broker.GridworksBroker.RELEASE_LOCK;
import static com.metaweb.gridworks.broker.GridworksBroker.START;
import static com.metaweb.gridworks.broker.GridworksBroker.OPEN;
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

import com.metaweb.gridworks.broker.GridworksBroker;
import com.metaweb.gridworks.broker.GridworksBrokerImpl;

public class GridworksBrokerTests {

    Logger logger;
    File data;
    
    @BeforeSuite
    public void suite_init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        data = new File("data");
        if (!data.exists()) data.mkdirs();
    }

    @AfterSuite
    public void suite_destroy() {
        for (File f : data.listFiles()) {
            f.delete();
        }
        data.delete();
    }

    // ------------------------------------------------------------------------------------
    
    ServletConfig config = null;
    GridworksBroker broker = null;
    
    @BeforeTest
    public void test_init() throws Exception {
        logger = LoggerFactory.getLogger(this.getClass());
        config = mock(ServletConfig.class);
        when(config.getInitParameter("gridworks.data")).thenReturn(data.getAbsolutePath());
        when(config.getInitParameter("gridworks.development")).thenReturn("true");

        broker = new GridworksBrokerImpl();
        broker.init(config);
    }

    @AfterTest
    public void test_destroy() throws Exception {
        broker.destroy();
        broker = null;
        config = null;
    }
    
    // ------------------------------------------------------------------------------------

    HttpServletRequest request = null;
    HttpServletResponse response = null;
    StringWriter writer = null;
    
    @BeforeMethod
    public void setup() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @AfterMethod
    public void teardown() throws Exception {
        response = null;
        request = null;
    }
    
    // ------------------------------------------------------------------------------------

    @Test
    public void testLifeCycle() {
        Assert.assertTrue(true);
    }

    @Test
    public void testService() {
        try {
            JSONObject result = call(broker, request, response, EXPIRE);
            assertJSON(result, "status", "ok");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testObtainLockFailure() {
        try {
            JSONObject result = call(broker, request, response, OBTAIN_LOCK);
            assertJSON(result, "status", "error");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testReleaseLockFailure() {
        try {
            JSONObject result = call(broker, request, response, RELEASE_LOCK);
            assertJSON(result, "status", "error");
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testStartProject() {
        try {
            String project = "1";
            String user = "testuser";
            String data = "blah";
            String metadata = "{}";
            String rev = "0";
            
            JSONObject result = call(broker, request, response, OBTAIN_LOCK, "pid", project, "uid", user, "locktype", Integer.toString(ALL), "lockvalue", "");
            assertJSON(result, "uid", "testuser");
            String lock = result.getString("lock");

            result = call(broker, request, response, START, "pid", project, "uid", user, "lock", lock, "data", data, "metadata", metadata, "rev", rev);
            assertJSON(result, "status", "ok");

            result = call(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", rev);
            JSONArray locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 1);
            JSONObject l = locks.getJSONObject(0);
            assertJSON(l, "uid", "testuser");
            Assert.assertEquals(l.getInt("type"), ALL);
            
            result = call(broker, request, response, RELEASE_LOCK, "pid", project, "uid", user, "lock", lock);
            assertJSON(result, "status", "ok");
            
            result = call(broker, request, response, GET_STATE, "pid", project, "uid", user, "rev", rev);
            locks = result.getJSONArray("locks");
            Assert.assertEquals(locks.length(), 0);
            
            result = call(broker, request, response, OPEN, "pid", project, "uid", user, "rev", rev);
            assertJSON(result, "status", "ok");
            JSONArray result_data = result.getJSONArray("data");
            Assert.assertEquals(result_data.length(),data.getBytes("UTF-8").length);
        } catch (Exception e) {
            Assert.fail();
        }
    }
    
    // ------------------------------------------------------------------------------------
    
    private void assertJSON(JSONObject o, String name, String value) throws JSONException {
        Assert.assertEquals(o.get(name), value);
    }
    
    private JSONObject call(GridworksBroker broker, HttpServletRequest request, HttpServletResponse response, String service, String... params) throws Exception {
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
        
        logger.info(result.toString());
        
        return result;
    }
}
