package com.google.refine.tests.expr.functions.strings;

import static org.junit.Assert.assertArrayEquals;
import static org.testng.Assert.assertTrue;
import java.util.Properties;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.strings.SmartSplit;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.util.TestUtils;

public class SmartSplitTests {

    private static Properties bindings;
    private static String FUNCTION_NAME = "smartSplit";

    @BeforeSuite
    public void setUp() {
        bindings = new Properties();
    }

    @AfterSuite
    public void tearDown() {
        bindings = null;
    }

    @Test
    public void testSmartSplitInvalidParams() {
        assertTrue(invoke(FUNCTION_NAME) instanceof EvalError);
        assertTrue(invoke(FUNCTION_NAME, "teststring1", 1, "teststring2", 2) instanceof EvalError);
    }

    @Test
    public void testSmartSplitGuessComma() {
        String testString = "teststring1,teststring2,teststring3,teststring4";
        String[] expected = { "teststring1", "teststring2", "teststring3", "teststring4" };
        String[] actual = (String[]) invoke(FUNCTION_NAME, testString);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSmartSplitGuessTab() {
        String testString = "teststring1	teststring2	teststring3	teststring4";
        String[] expected = { "teststring1", "teststring2", "teststring3", "teststring4" };
        String[] actual = (String[]) invoke(FUNCTION_NAME, testString);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testSmartSplitCharSepGiven() {
        String testString = "teststring1#teststring2#teststring3#teststring4";
        String[] expected = { "teststring1", "teststring2", "teststring3", "teststring4" };
        String[] actual = (String[]) invoke(FUNCTION_NAME, testString, '#');
        assertArrayEquals(expected, actual);
    }
    
    @Test
    public void testSmartSplitCharSepSpace() {
        String testString = "teststring1 teststring2 teststring3 teststring4";
        String[] expected = { "teststring1", "teststring2", "teststring3", "teststring4" };
        String[] actual = (String[]) invoke(FUNCTION_NAME, testString, ' ');
        assertArrayEquals(expected, actual);
    }
    
    @Test
    public void testSmartSplitStringSepGiven() {
        String testString = "teststring1#@$teststring2#@$teststring3#@$teststring4";
        String[] expected = { "teststring1", "teststring2", "teststring3", "teststring4" };
        String[] actual = (String[]) invoke(FUNCTION_NAME, testString, "#@$");
        assertArrayEquals(expected, actual);
    }

    /**
     * Lookup a control function by name and invoke it with a variable number of
     * args
     */
    private static Object invoke(String name, Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function " + name);
        }
        if (args == null) {
            return function.call(bindings, new Object[0]);
        } else {
            return function.call(bindings, args);
        }
    }

    @Test
    public void serializeSmartSplit() {
        String json = "{\"description\":\"Returns the array of strings obtained by splitting s with separator sep. Handles quotes properly. Guesses tab or comma separator if \\\"sep\\\" is not given.\",\"params\":\"string s, optional string sep\",\"returns\":\"array\"}";
        TestUtils.isSerializedTo(new SmartSplit(), json);
    }

}
