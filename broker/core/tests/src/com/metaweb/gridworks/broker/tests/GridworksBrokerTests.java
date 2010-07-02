package com.metaweb.gridworks.broker.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

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
        //System.setProperty("log4j.configuration", "tests.log4j.properties");
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
    
    @BeforeMethod
    public void setup() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        config = mock(ServletConfig.class);

        when(config.getInitParameter("gridworks.data")).thenReturn(data.getAbsolutePath());

        SUT = new GridworksBrokerImpl();
        SUT.init(config);
    }

    @AfterMethod
    public void teardown() {
        SUT = null;
        
        request = null;
        response = null;
        config = null;
    }
    
    // ------------------------------------------------------------------------------------

    @Test
    public void testLifeCycle() {
        Assert.assertTrue(true);
    }

    
}
