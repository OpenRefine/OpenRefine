
package com.google.refine.expr.functions.strings;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Map;

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
        Map<String, String> m = (Map<String, String>) invoke("parseUri", sampleUri);
        Assert.assertEquals(m.get("path"), "/documentation");
        Assert.assertEquals(m.get("host"), "www.openrefine.org");
        Assert.assertEquals(m.get("port"), "80");
        Assert.assertEquals(m.get("query"), "");
        Assert.assertEquals(m.get("fragment"), "download");
        Assert.assertEquals(m.get("scheme"), "https");
    }

    @Test
    public void testParseUriInvalidParams() {
        Assert.assertTrue(invoke("parseUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", "sampleUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", sampleUri, sampleUri) instanceof EvalError);
    }

}
