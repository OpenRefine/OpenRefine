
package com.google.refine.tests.expr.functions;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.RefineServlet;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;

/**
 * Test cases for find function.
 */
public class FindFunctionTests extends RefineTest {
    static Properties bindings;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    // dependencies
    RefineServlet servlet;

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
        
        servlet = new RefineServletStub();
    }
    
    @AfterMethod
    public void TearDown() {
    }

    
    @Test
    public void findFunctionFindAllTest() throws Exception {
        String[] matches = (String[]) invoke("find", "This is a test string for testing find.", "test");
        Assert.assertEquals(matches[0], "test");
        Assert.assertEquals(matches[1], "test");
    }
    
    @Test
    public void findFunctionFindAllTest2() throws Exception {
        String[] matches = (String[]) invoke("find", "hello 123456 goodbye.", "\\d{6}|hello");
        Assert.assertEquals(matches[0], "hello");
        Assert.assertEquals(matches[1], "123456");
    }
    
    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }
}
