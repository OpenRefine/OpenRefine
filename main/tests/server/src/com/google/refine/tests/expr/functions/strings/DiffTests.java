package com.google.refine.tests.expr.functions.strings;

import java.util.Calendar;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.util.CalendarParser;
import com.google.refine.expr.util.CalendarParserException;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


public class DiffTests extends RefineTest {

    static Properties bindings;
    private Calendar date1;
    private Calendar date2;
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        try {
            date1 = CalendarParser.parse("2012-08-02");
            date2 = CalendarParser.parse("2012-10-02");
        } catch (CalendarParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Assert.fail();
        }
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
        Assert.assertTrue(invoke("diff", date1,date2) instanceof EvalError);
        Assert.assertTrue(invoke("diff", date1,date2,"foo") instanceof EvalError);
    }
    
    @Test
    public void testDiff() {        
        Assert.assertEquals((String)(invoke("diff", "onetwo","onetwothree")),"three");
        Assert.assertEquals(invoke("diff",date2,date1,"days"),Long.valueOf(61));
        Assert.assertEquals(invoke("diff",date2,date1,"weeks"),Long.valueOf(8));
        Assert.assertEquals(invoke("diff",date2,date1,"months"),Long.valueOf(2));
        Assert.assertEquals(invoke("diff",date2,date1,"hours"),Long.valueOf(1464));
        Assert.assertEquals(invoke("diff",date2,date1,"seconds"),Long.valueOf(5270400));

    }
}
