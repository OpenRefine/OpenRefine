/*******************************************************************************
 * Copyright (C) 2018, OpenRefine contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package com.google.refine.expr.functions.xml;

import org.jsoup.parser.Parser;
import org.jsoup.Jsoup;
import org.testng.annotations.Test;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class ParseXmlTests extends RefineTest {

    static Properties bindings;
    static String x = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<root xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
            "    <foaf:Person>\n" +
            "        <foaf:name>John Doe</foaf:name>\n" +
            "        <head>head1</head>\n" +
            "        <head>head2</head>\n" +
            "        <BODY>body1</BODY>\n" +
            "        <foaf:homepage rdf:resource=\"http://www.example.com\"/>\n" +
            "    </foaf:Person>\n" +
            "    <foaf:Person>\n" +
            "        <foaf:name>Héloïse Dupont</foaf:name>\n" +
            "        <head>head3</head>\n" +
            "        <BODY>body2</BODY>\n" +
            "        <foaf:title/>\n" +
            "    </foaf:Person>\n" +
            "</root>";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testParseXml() {
        Assert.assertTrue(invoke("parseXml") instanceof EvalError);
        Assert.assertTrue(invoke("parseXml", "x") instanceof org.jsoup.nodes.Document);
        Assert.assertTrue(invoke("select", Jsoup.parse(x, "", Parser.xmlParser()), "foaf|Person") instanceof org.jsoup.select.Elements);
        Assert.assertEquals(invoke("innerXml", Jsoup.parse(x, "", Parser.xmlParser()).select("foaf|Person").first()),
                "<foaf:name>John Doe</foaf:name>\n<head>head1</head>\n<head>head2</head>\n<BODY>body1</BODY>\n<foaf:homepage rdf:resource=\"http://www.example.com\" />");
        Assert.assertEquals(invoke("xmlAttr", Jsoup.parse(x, "", Parser.xmlParser()).select("foaf|homepage").first(), "rdf:resource"),
                "http://www.example.com");
        Assert.assertEquals(invoke("ownText", Jsoup.parse(x, "", Parser.xmlParser()).select("BODY").first()), "body1");
        Assert.assertEquals(invoke("xmlText", Jsoup.parse(x, "", Parser.xmlParser()).select("foaf|Person").first()),
                "John Doe head1 head2 body1");
    }
}
