
package com.google.refine.expr.functions.strings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.ParsingUtilities;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ParseUriTest extends RefineTest {

    private String sampleUri;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        sampleUri = "https://www.openrefine.org:80/documentation#download?format=xml&os=mac";
    }

    @Test
    public void testParseUriValidParams() {
        ObjectNode resNode = (ObjectNode) invoke("parseUri", sampleUri);

        Assert.assertNotNull(resNode);
        Assert.assertEquals(resNode.get("scheme").asText(), "https");
        Assert.assertEquals(resNode.get("host").asText(), "www.openrefine.org");
        Assert.assertEquals(resNode.get("port").asInt(), 80);
        Assert.assertEquals(resNode.get("path").asText(), "/documentation");
        Assert.assertEquals(resNode.get("fragment").asText(), "download");

        ObjectNode qpExpected = ParsingUtilities.mapper.createObjectNode();
        qpExpected.put("format", "xml");
        qpExpected.put("os", "mac");
        Assert.assertEquals(resNode.get("query_params").toString(), qpExpected.toString());
    }

    @Test
    public void testParseUriInvalidParams() {
        Assert.assertTrue(invoke("parseUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", "sampleUri") instanceof EvalError);
        Assert.assertTrue(invoke("parseUri", sampleUri, sampleUri) instanceof EvalError);
    }

}
