package com.google.refine.templating;

import com.google.refine.RefineTest;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.ParsingException;
import com.google.refine.model.Project;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.templating.Parser;

import java.util.Properties;
import java.util.regex.PatternSyntaxException;

public class ParserTests extends RefineTest {

    Project project;
    Properties bindings;

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() {
        project = new Project();
        bindings = ExpressionUtils.createBindings(project);
    }

    @Test
    public void testInvalidSyntax(){
        String tests[] = {
            "    {\n      \"URN\" : {{jso{nize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"U{RN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      {\"URN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}},\\n      \"LA (name)\" : {{{jsonize(cells[\"LA (name)\"].value)}}\n}",
            "    {\n      \"URN\" : {{jsonize(cells[\"URN\"].value)}error},\\n      \"LA (name)\" : {{jsonize(cells[\"LA (name)\"].value)}}\n}"
        };
        for (String test : tests) {
            try{
                Parser.parse(test, bindings);
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
                Parser.parse(test, bindings);
            } catch (ParsingException e) {
                Assert.fail("Expression failed to generate parse syntax error: " + test);
            }
        }
    }

    @Test
    public void testIncorrectFunctions(){
        String tests[] = {
            "    {\n      \"URN\" : {{jsonnize(cells[\"URN\"].value)}}    \n}"
        };
        for (String test : tests) {
            try{
                Parser.parse(test, bindings);
            } catch (ParsingException e) {
                // Test succeeded
                continue;
            }
            Assert.fail("Expression failed to generate parse syntax error: " + test);
        }
    }

}
