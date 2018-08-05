package com.google.refine.tests.expr.functions.date;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Properties;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


public class NowTests extends RefineTest {
    private static Properties bindings;
    private DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.BASIC_ISO_DATE).appendLiteral('-')
            .appendPattern("HH:mm:ss")
            .appendPattern("[.SSSSSSSSS][.SSSSSS][.SSS]")       // optional nanos, with 9, 6 or 3 digits
            .appendOffset("+HH:mm", "Z")
            .toFormatter();
    
    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    @BeforeMethod
    public void setUp() {
        bindings = new Properties();
    }

    @AfterMethod
    public void tearDown() {
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
    public void testNow() {        
        // 2018-4-30 23:55:44
        OffsetDateTime source = OffsetDateTime.parse("20180430-23:55:44.000789000Z",
                formatter);
        
        Assert.assertTrue(invoke("now") instanceof OffsetDateTime);
        Assert.assertTrue(((OffsetDateTime)invoke("now")).isAfter(source));
    }
}