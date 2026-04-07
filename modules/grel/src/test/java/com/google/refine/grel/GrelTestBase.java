
package com.google.refine.grel;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;

import com.google.refine.RefineTest;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;
import com.google.refine.util.ParsingUtilities;

/**
 * Base class for tests of GREL's functionalities
 */
public class GrelTestBase extends RefineTest {

    protected Logger logger = null;

    protected static Properties bindings = null;

    @BeforeMethod
    public void registerGRELParser() {
        MetaParser.registerLanguageParser("grel", "GREL", Parser.grelParser, "value");
    }

    @AfterMethod
    public void unregisterGRELParser() {
        MetaParser.unregisterLanguageParser("grel");
    }

    @BeforeTest
    public void initLogger() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void tearDown() {
        bindings = null;
    }

    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    protected static Object invoke(String name, Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (bindings == null) {
            bindings = new Properties();
        }
        if (function == null) {
            throw new IllegalArgumentException("Unknown function " + name);
        }
        if (args == null) {
            return function.call(bindings, new Object[0]);
        } else {
            return function.call(bindings, args);
        }
    }

    /**
     * Parse and evaluate a GREL expression and compare the result to the expect value
     *
     * @param bindings
     * @param test
     * @throws ParsingException
     */
    protected void parseEval(Properties bindings, String[] test)
            throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + test[0]);
        Object result = eval.evaluate(bindings);
        if (test[1] != null) {
            assertNotNull(result, "Expected " + test[1] + " for test " + test[0]);
            assertEquals(result.toString(), test[1], "Wrong result for expression: " + test[0]);
        } else {
            Assert.assertNull(result, "Wrong result for expression: " + test[0]);
        }
    }

    /**
     * Parse and evaluate a GREL expression and compare the result an expected type using instanceof
     *
     * @param bindings
     * @param test
     * @throws ParsingException
     */
    protected void parseEvalType(Properties bindings, String test, @SuppressWarnings("rawtypes") Class clazz)
            throws ParsingException {
        Evaluable eval = MetaParser.parse("grel:" + test);
        Object result = eval.evaluate(bindings);
        Assert.assertTrue(clazz.isInstance(result), "Wrong result type for expression: " + test);
    }

    public static void testControlJsonSerialization(Control control, String params, String returns) {
        try {
            String json = ParsingUtilities.defaultWriter.writeValueAsString(control);
            JsonNode obj = ParsingUtilities.mapper.readTree(json);
            assertTrue(obj.isObject(), "Serialized value is not a JSON object: " + json);
            String desc = obj.get("description").asText();
            assertNotNull(desc, "Missing description in JSON");
            assertFalse(desc.isEmpty());
            assertEquals(obj.get("params").asText(), params);
            assertEquals(obj.get("returns").asText(), returns);
        } catch (JsonProcessingException e) {
            fail("Failed to serialize or parse JSON while checking keys", e);
        }
    }

    @AfterMethod
    public void TearDown() throws Exception {
        bindings = null;
    }
}
