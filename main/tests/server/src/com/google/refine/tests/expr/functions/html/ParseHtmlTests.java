package com.google.refine.tests.expr.functions.html;

import org.testng.annotations.Test;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.html.ParseHtml;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;
import com.google.refine.tests.util.TestUtils;

public class ParseHtmlTests extends RefineTest  {
    
    static Properties bindings;
    static String h =   "<html>\n" +
                        "<head>\n" +
                        "</head>\n" +
                        "    <body>\n" +
                        "        <h1>head1</h1>\n" +
                        "        <div class=\"class1\">\n" +
                        "            <p>para1</p>\n" +
                        "            <p>para2</p>\n" +
                        "        </div>\n" +
                        "    </body>\n" +
                        "</html>";
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void TearDown() {
        bindings = null;
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
    
    @Test
    public void serializeParseHtml() {
        String json = "{\"description\":\"Parses a string as HTML\",\"params\":\"string s\",\"returns\":\"HTML object\"}";
        TestUtils.isSerializedTo(new ParseHtml(), json);
    }
    
    @Test
    public void testParseHtml() {
        Assert.assertTrue(invoke("parseHtml") instanceof EvalError);
        Assert.assertTrue(invoke("parseHtml","h") instanceof org.jsoup.nodes.Document);
    }
}

