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

package com.google.refine.expr.functions.html;

import org.jsoup.Jsoup;
import org.testng.annotations.Test;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;

import com.google.refine.RefineTest;
import com.google.refine.expr.EvalError;
import com.google.refine.util.TestUtils;

public class ParseHtmlTests extends RefineTest {

    static Properties bindings;
    static String h = "<html>\n" +
            "<head>\n" +
            "<script type=\"application/json\">One Two</script>" +
            "</head>\n" +
            "    <body>\n" +
            "        <h1>head1</h1>\n" +
            "        <div class=\"class1\">\n" +
            "            <p>para1 <strong>strong text</strong></p>\n" +
            "            <p>para2</p>\n" +
            "        </div>\n" +
            "        <div class=\"commentthread_comment_text\" id=\"comment_content_257769\">\n" +
            "  Me : Make a 2nd game ?\n" +
            " <br>Dev : Nah man , too much work.\n" +
            " <br>Me : So what's it gonna be ?\n" +
            " <br>Dev : REMASTER !!!!\n" +
            " <br>" +
            "</div>" +
            "<div><p type=\"child\">childtext</p></div>" +
            "    </body>\n" +
            "</html>";

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Test
    public void testParseHtml() {
        Assert.assertTrue(invoke("parseHtml") instanceof EvalError);
        Assert.assertTrue(invoke("parseHtml", "h") instanceof org.jsoup.nodes.Document);
        Assert.assertTrue(invoke("select", Jsoup.parse(h), "p") instanceof org.jsoup.select.Elements);
        Assert.assertTrue(invoke("innerHtml", Jsoup.parse(h).select("p").first()) instanceof String);
        Assert.assertEquals(invoke("innerHtml", Jsoup.parse(h).select("p").first()), "para1 <strong>strong text</strong>");
        Assert.assertEquals(invoke("htmlAttr", Jsoup.parse(h).select("div").first(), "class"), "class1");
        Assert.assertEquals(invoke("htmlText", Jsoup.parse(h).select("div").first()), "para1 strong text para2");
        Assert.assertEquals(invoke("ownText", Jsoup.parse(h).select("p").first()), "para1");
        Assert.assertTrue(invoke("wholeText", Jsoup.parse(h).select("div.commentthread_comment_text").first()) instanceof String);
        Assert.assertEquals(invoke("wholeText", Jsoup.parse(h).select("div.commentthread_comment_text").first()),
                "\n  Me : Make a 2nd game ?\n \nDev : Nah man , too much work.\n \nMe : So what's it gonna be ?\n \nDev : REMASTER !!!!\n \n");
        Assert.assertEquals(invoke("parent", Jsoup.parse(h).select("p[type*=child]").first()).toString(),
                "<div>\n <p type=\"child\">childtext</p>\n</div>");
        Assert.assertEquals(invoke("scriptText", Jsoup.parse(h).select("script").first()), "One Two");
        Assert.assertEquals(invoke("scriptText", Jsoup.parse(h).select("h1").first()), "");
        Assert.assertTrue(invoke("scriptText", Jsoup.parse(h).select("p")) instanceof EvalError);
    }
}
