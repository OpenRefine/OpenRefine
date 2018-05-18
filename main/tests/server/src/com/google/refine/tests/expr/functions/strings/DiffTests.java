package com.google.refine.tests.expr.functions.strings;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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


public class DiffTests extends RefineTest {

    static Properties bindings;
    
    private OffsetDateTime odt1;
    private OffsetDateTime odt2;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        odt1 = OffsetDateTime.parse("2011-09-01T10:15:30.123456+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        odt2 = OffsetDateTime.parse("2011-12-02T10:16:30.123467+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
    public void testDiffInvalidParams() {        
        Assert.assertTrue(invoke("diff") instanceof EvalError);
        Assert.assertTrue(invoke("diff", "one","two","three") instanceof EvalError);
    }
    
    @Test
    public void testDiffString() {        
        Assert.assertEquals((String)(invoke("diff", "onetwo","onetwothree")),"three");
    }    
     
    @Test
    public void testDiffOffsetDateTime() {  
        // OffsetDateTime diff:
        Assert.assertEquals(invoke("diff",odt2,odt1,"days"),Long.valueOf(92));
        Assert.assertEquals(invoke("diff",odt2,odt1,"weeks"),Long.valueOf(13));
        Assert.assertEquals(invoke("diff",odt2,odt1,"months"),Long.valueOf(3));
        Assert.assertEquals(invoke("diff",odt2,odt1,"hours"),Long.valueOf(2208));
        Assert.assertEquals(invoke("diff",odt2,odt1,"seconds"),Long.valueOf(7948860));
        Assert.assertEquals(invoke("diff",odt2,odt1,"milliseconds"),Long.valueOf(7948860000011l));
        Assert.assertEquals(invoke("diff",odt2,odt1,"nanos"),Long.valueOf(7948860000011000l));
    }
}
