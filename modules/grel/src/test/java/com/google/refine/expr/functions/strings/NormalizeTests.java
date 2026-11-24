
package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.GrelTestBase;

public class NormalizeTests extends GrelTestBase {

    static Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void normalizeFunctionWithDiacriticsTest() throws Exception {
        Object result = invoke("normalize", "Café");
        Assert.assertEquals(result, "Cafe");
    }

    @Test
    public void normalizeFunctionWithExtendedCharactersTest() throws Exception {
        Object result = invoke("normalize", "Ångström");
        Assert.assertEquals(result, "Angstrom");
    }

    @Test
    public void normalizeFunctionWithNullTest() throws Exception {
        Object result = invoke("normalize", (Object) null);
        Assert.assertTrue(result instanceof EvalError);
    }

    @Test
    public void normalizeFunctionWithEmptyStringTest() throws Exception {
        Object result = invoke("normalize", "");
        Assert.assertEquals(result, "");
    }

    @DataProvider(name = "unicodeForms")
    public Object[][] unicodeForms() {
        return new Object[][] {
                { "e\u0301\u2460", "NFC", "\u00e9\u2460" },
                { "e\u0301\u2460", "nfc", "\u00e9\u2460" },
                { "\u00e9\u2460", "NFD", "e\u0301\u2460" },
                { "\u00e9\u2460", "nFd", "e\u0301\u2460" },
                { "e\u0301\u2460", "NFKC", "\u00e91" },
                { "e\u0301\u2460", "nfkc", "\u00e91" },
                { "\u00e9\u2460", "NFKD", "e\u03011" },
                { "\u00e9\u2460", "NfKd", "e\u03011" },
                { "", "NFC", "" }
        };
    }

    @Test(dataProvider = "unicodeForms")
    public void normalizeFunctionWithUnicodeFormTest(String input, String form, String expected) {
        Assert.assertEquals(invoke("normalize", input, form), expected);
    }

    @Test
    public void normalizeFunctionWithNonStringInputTest() {
        Assert.assertEquals(invoke("normalize", 123), "123");
        Assert.assertEquals(invoke("normalize", 123, "nfc"), "123");
    }

    @DataProvider(name = "invalidArguments")
    public Object[][] invalidArguments() {
        return new Object[][] {
                { new Object[0] },
                { new Object[] { "Caf\u00e9", "NFC", "extra" } },
                { new Object[] { null, "NFC" } },
                { new Object[] { "Caf\u00e9", null } },
                { new Object[] { "Caf\u00e9", 1 } },
                { new Object[] { "Caf\u00e9", "" } },
                { new Object[] { "Caf\u00e9", "invalid" } }
        };
    }

    @Test(dataProvider = "invalidArguments")
    public void normalizeFunctionWithInvalidArgumentsTest(Object[] args) {
        Assert.assertTrue(invoke("normalize", args) instanceof EvalError);
    }

    @Test
    public void normalizeFunctionInExpressionTest() throws Exception {
        Properties expressionBindings = new Properties();
        expressionBindings.put("value", "e\u0301\u2460");
        parseEval(expressionBindings, new String[] { "value.normalize('nfkc')", "\u00e91" });
    }

}
