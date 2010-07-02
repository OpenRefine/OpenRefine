package com.metaweb.gridworks.broker.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.metaweb.gridworks.broker.GridworksBroker;
import com.metaweb.gridworks.broker.GridworksBrokerImpl;

public class GridworksBrokerTests {

    protected Logger logger;

    protected File data;
    
    @BeforeSuite
    public void suite_init() {
        System.setProperty("log4j.configuration", "tests.log4j.properties");
        data = new File("data");
        if (!data.exists()) data.mkdirs();
    }

    @AfterSuite
    public void suite_destroy() {
    }

    @BeforeTest
    public void test_init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
        
    // System under test
    GridworksBroker SUT = null;

    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    ServletConfig config = null;
    StringWriter writer = null;
    
    @BeforeMethod
    public void setup() throws Exception {
        config = mock(ServletConfig.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        writer = new StringWriter();

        when(config.getInitParameter("gridworks.data")).thenReturn(data.getAbsolutePath());
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        SUT = new GridworksBrokerImpl();
        SUT.init(config);
    }

    @AfterMethod
    public void teardown() throws Exception {
        SUT.destroy();
        SUT = null;
    
        writer = null;
        response = null;
        request = null;
        config = null;
    }
    
    // ------------------------------------------------------------------------------------

    @Test
    public void testLifeCycle() {
        logger.info("testing lifecycle");
        Assert.assertTrue(true);
    }

    @Test
    public void testService() {
        try {
            call(SUT, request, response, "expire", null);
            logger.info(writer.toString());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    // ------------------------------------------------------------------------------------
    
    private void call(GridworksBroker broker, HttpServletRequest request, HttpServletResponse response, String service, Map<String,String> params) throws Exception {
        if (params != null) {
            for (Entry<String,String> e : params.entrySet()) {
                when(request.getParameter(e.getKey())).thenReturn(e.getValue());
            }
        }

        broker.process(service, request, response);
        
        if (params != null) {
            for (Entry<String,String> e : params.entrySet()) {
                verify(request,times(1)).getParameter(e.getKey());
            }
        }
    }
}
