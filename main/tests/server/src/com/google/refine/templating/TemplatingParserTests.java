package com.google.refine.templating;

import com.google.refine.RefineTest;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


public class TemplatingParserTests extends RefineTest {

    Project project;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() {
        project = new Project();
    }

    @Test
    public void testInvalidSyntax(){
        String tests[] = {
            "    {\n      \"U{RN\" : {{json{ize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      {\"URN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\"} : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}error},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\" : {{json}ize(cells[\"URN\"].value)}}    \n}",
            "    {\n      \"URN\" : {{{jsonize(cells[\"URN\"].value)}}    \n}",
            "{\"URN\" : {{jsoni{ze(cells[\"URN\"].value)}}}"
        };
        for (String test : tests) {
            try{
                Parser.parse(test);
            } catch (ParsingException e) {
                // Test succeeded
                continue;
            }
            Assert.fail("Expression failed to generate parse syntax error: " + test);
        }
    }

    @Test
    public void testCorrectFunctions(){
        String tests[] = {
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}}    \n}"
        };
        for (String test : tests) {
            try{
                Parser.parse(test);
            } catch (ParsingException e) {
                Assert.fail("Expression failed to generate parse syntax error: " + test);
            }
        }
    }

    @Test
    public void testIncorrectFunctions(){
        String tests[] = {
            "    {\n      \"URN\" : {{jsonnize(cells[\"URN\"].value)}}    \n}",
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{json(cells[\"LA (name)\"].value)}}\n}",
        };
        for (String test : tests) {
            try{
                Parser.parse(test);
            } catch (ParsingException e) {
                // Test succeeded
                continue;
            }
            Assert.fail("Expression failed to generate parse syntax error: " + test);
        }
    }

}
