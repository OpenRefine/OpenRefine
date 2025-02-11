package com.google.refine.expr.functions.strings;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Ignore;
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



}
