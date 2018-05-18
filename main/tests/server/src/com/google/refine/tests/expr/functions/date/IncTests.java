package com.google.refine.tests.expr.functions.date;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
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


public class IncTests extends RefineTest {
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
    public void testInc() {        
        OffsetDateTime source = OffsetDateTime.parse("20180510-23:55:44.000789000Z",
                formatter);
        
        // add hours
        Assert.assertTrue(invoke("inc",  source, 2, "hours") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "hours"), source.plus(2, ChronoUnit.HOURS));
        Assert.assertEquals(invoke("inc",  source, 2, "hour"), source.plus(2, ChronoUnit.HOURS));
        Assert.assertEquals(invoke("inc",  source, 2, "h"), source.plus(2, ChronoUnit.HOURS));
        
        // add years
        Assert.assertTrue(invoke("inc",  source, 2, "year") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "years"), source.plus(2, ChronoUnit.YEARS));
        Assert.assertEquals(invoke("inc",  source, 2, "year"), source.plus(2, ChronoUnit.YEARS));
        
        // add months
        Assert.assertTrue(invoke("inc",  source, 2, "months") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "months"), source.plus(2, ChronoUnit.MONTHS));
        Assert.assertEquals(invoke("inc",  source, 2, "month"), source.plus(2, ChronoUnit.MONTHS));
        
        // add minutes
        Assert.assertTrue(invoke("inc",  source, 2, "minutes") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "minutes"), source.plus(2, ChronoUnit.MINUTES));
        Assert.assertEquals(invoke("inc",  source, 2, "minute"), source.plus(2, ChronoUnit.MINUTES));
        Assert.assertEquals(invoke("inc",  source, 2, "min"), source.plus(2, ChronoUnit.MINUTES));
        
        // add weeks
        Assert.assertTrue(invoke("inc",  source, 2, "weeks") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "weeks"), source.plus(2, ChronoUnit.WEEKS));
        Assert.assertEquals(invoke("inc",  source, 2, "week"), source.plus(2, ChronoUnit.WEEKS));
        Assert.assertEquals(invoke("inc",  source, 2, "w"), source.plus(2, ChronoUnit.WEEKS));
        
        // add seconds
        Assert.assertTrue(invoke("inc",  source, 2, "seconds") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "seconds"), source.plus(2, ChronoUnit.SECONDS));
        Assert.assertEquals(invoke("inc",  source, 2, "sec"), source.plus(2, ChronoUnit.SECONDS));
        Assert.assertEquals(invoke("inc",  source, 2, "s"), source.plus(2, ChronoUnit.SECONDS));
        
        // add milliseconds
        Assert.assertTrue(invoke("inc",  source, 2, "milliseconds") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "milliseconds"), source.plus(2, ChronoUnit.MILLIS));
        Assert.assertEquals(invoke("inc",  source, 2, "ms"), source.plus(2, ChronoUnit.MILLIS));
        Assert.assertEquals(invoke("inc",  source, 2, "S"), source.plus(2, ChronoUnit.MILLIS));
        
        // add nanos
        Assert.assertTrue(invoke("inc",  source, 2, "nanos") instanceof OffsetDateTime);
        Assert.assertEquals(invoke("inc",  source, 2, "nanos"), source.plus(2, ChronoUnit.NANOS));
        Assert.assertEquals(invoke("inc",  source, 2, "nano"), source.plus(2, ChronoUnit.NANOS));
        Assert.assertEquals(invoke("inc",  source, 2, "n"), source.plus(2, ChronoUnit.NANOS));
        
        // exception
        Assert.assertTrue(invoke("inc", source, 99) instanceof EvalError);
        Assert.assertTrue(invoke("inc", source.toInstant().toEpochMilli(), 99, "h") instanceof EvalError);
    }
}