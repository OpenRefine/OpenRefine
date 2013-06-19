package com.google.refine.tests.expr.functions.strings;

import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


public class TrimTests extends RefineTest {

    static Properties bindings;
    private static String NBSP = "\u00A0";
    private static String ENQUAD = "\u2000";
    private static String EMQUAD = "\u2001";
    private static String ENSPC = "\u2002";
    private static String EMSPC = "\u2003";
    private static String N3PMSPC = "\u2004";
    private static String N4PMSPC = "\u2005";
    private static String N6PMSPC = "\u2006";
    private static String FIGSP = "\u2007";
    private static String PUNCSPC = "\u2008";
    private static String THINSPC = "\u2009";
    private static String HAIRSPC = "\u200A";
    private static String NNBSP = "\u202F";
    private static String MDMMATHSPC = "\u205F";
//    private static String ZWNBSP = "\uFEFF";
//    private static String WDJOIN = "\u2060";
    private static String IDEOSPC = "\u3000";

    private static String WHITESPACE = NBSP+ENQUAD+ENSPC+EMQUAD+EMSPC+N3PMSPC+N4PMSPC+N6PMSPC+FIGSP+PUNCSPC
            +THINSPC+HAIRSPC+NNBSP+MDMMATHSPC
//            +ZWNBSP
//            +WDJOIN
            +IDEOSPC
            ;
    
    private static String[][] testStrings = {
        {" foo ","foo"},  
        {"\tfoo\t","foo"},  
        {"\t \t foo \t \t","foo"},  
//        {WHITESPACE+"foo"+WHITESPACE,"foo"},  
        {"",""},  
    };
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    @BeforeMethod
    public void SetUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void TearDown() {
        bindings = null;
    }
    
    /**
     * Lookup a control function by name and invoke it with a variable number of args
     */
    private static Object invoke(String name,Object... args) {
        // registry uses static initializer, so no need to set it up
        Function function = ControlFunctionRegistry.getFunction(name);
        if (function == null) {
            throw new IllegalArgumentException("Unknown function "+name);
        }
        if (args == null) {
            return function.call(bindings,new Object[0]);
        } else {
            return function.call(bindings,args);
        }
    }
    
    @Test
    public void testInvalidParams() {        
        Assert.assertTrue(invoke("trim") instanceof EvalError);
        Assert.assertTrue(invoke("trim", "one","two","three") instanceof EvalError);
        Assert.assertTrue(invoke("trim", Long.getLong("1")) instanceof EvalError);
    }
    
    @Test
    public void testTrim() {
        for (String[] ss : testStrings) {
            Assert.assertEquals(ss.length,2,"Invalid test"); // Not a valid test
            Assert.assertEquals((String)(invoke("trim", ss[0])),ss[1],"Trim for string: " + ss + " failed");
        }

        for (int i=0; i < WHITESPACE.length(); i++) {
            String c = WHITESPACE.substring(i,i+1);
            Assert.assertEquals((String)(invoke("trim", c+"foo"+c)),"foo","Trim for whitespace char: '" + c + "' at index "+ i+ " failed");
        }

    }
}
