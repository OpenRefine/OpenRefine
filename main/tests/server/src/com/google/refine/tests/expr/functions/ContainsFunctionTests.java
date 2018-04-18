
package com.google.refine.tests.expr.functions;

import com.google.refine.RefineServlet;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.RefineTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Test cases for find function.
 */
public class ContainsFunctionTests extends RefineTest {
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
    public void testContainsFunction() {
        String value = "rose is a rose";
        Assert.assertEquals(invoke("contains", value, "rose"),true);

        // Test if it does not interpret regex passed in String as Pattern
        Assert.assertEquals(invoke("contains", value, "$"),false);
        Assert.assertEquals(invoke("contains", value, "r.se"),false);
        Assert.assertEquals(invoke("contains", value, "\\s+"),false);
        // Input regex pattern in UI with : "/ /" , is intepreted as Pattern
        Assert.assertEquals(invoke("contains", value, Pattern.compile("$")),true);
        Assert.assertEquals(invoke("contains", value, Pattern.compile("\\s+")),true);
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
