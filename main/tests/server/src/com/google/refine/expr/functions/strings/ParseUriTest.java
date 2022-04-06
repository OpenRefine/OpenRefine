
package com.google.refine.expr.functions.strings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.ParsingUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;

public class ParseUriTest extends RefineTest {

    private String sampleUri;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        sampleUri = "https://www.openrefine.org:80/documentation#download";
    }

    @Test
    public void testParseUriValidParams() {
        Object res = invoke("parseUri", sampleUri);
        HashMap<String, String> resMap = ParsingUtilities.mapper.convertValue(res, new TypeReference<>() {
        });
        Assert.assertNotNull(res);
        Assert.assertEquals(resMap.get("path"), "/documentation");
        Assert.assertEquals(resMap.get("host"), "www.openrefine.org");
        Assert.assertEquals(resMap.get("port"), "80");
        Assert.assertEquals(resMap.get("query"), "");
        Assert.assertEquals(resMap.get("fragment"), "download");
        Assert.assertEquals(resMap.get("scheme"), "https");

    }

    @Test
    public void testParseUriInvalidParams() {
        Assert.assertTrue(invoke("parseUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", "sampleUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", sampleUri, sampleUri) instanceof EvalError);
    }

}
