package com.google.refine.tests.expr.functions.date;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.tests.RefineTest;


public class DatePartTests extends RefineTest {

    static Properties bindings;
    
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
    
    private DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.BASIC_ISO_DATE).appendLiteral('-')
            .appendPattern("HH:mm:ss")
            .appendPattern("[.SSSSSSSSS][.SSSSSS][.SSS]")       // optional nanos, with 9, 6 or 3 digits
            .appendOffset("+HH:mm", "Z")
            .toFormatter();
    
    @Test
    public void testOffsetDateTimeDatePart() {        
        // 2018-4-30 23:55:44
        OffsetDateTime source = OffsetDateTime.parse("20180430-23:55:44.000789000Z",
                formatter);
        
        // hours
        Assert.assertEquals(invoke("datePart", source, "hours"), 23);
        Assert.assertEquals(invoke("datePart", source, "hour"), 23);
        Assert.assertEquals(invoke("datePart", source, "h"), 23);
        
        // minutes
        Assert.assertEquals(invoke("datePart", source, "minutes"), 55);
        Assert.assertEquals(invoke("datePart", source, "minute"), 55);
        Assert.assertEquals(invoke("datePart", source, "min"), 55);
        
        // seconds
        Assert.assertEquals(invoke("datePart", source, "seconds"), 44);
        Assert.assertEquals(invoke("datePart", source, "sec"), 44);
        Assert.assertEquals(invoke("datePart", source, "s"), 44);
        
        // milliseconds
        Assert.assertEquals(invoke("datePart", source, "milliseconds"), 789);
        Assert.assertEquals(invoke("datePart", source, "ms"), 789);
        Assert.assertEquals(invoke("datePart", source, "S"), 789);
        
        // nanos
        Assert.assertEquals(invoke("datePart", source, "nanos"), 789000);
        Assert.assertEquals(invoke("datePart", source, "nano"), 789000);
        Assert.assertEquals(invoke("datePart", source, "n"), 789000);
        
        // years
        Assert.assertEquals(invoke("datePart", source, "years"), 2018);
        Assert.assertEquals(invoke("datePart", source, "year"), 2018);
        
        // months
        Assert.assertEquals(invoke("datePart", source, "months"), 4);
        Assert.assertEquals(invoke("datePart", source, "month"), 4);
        
        // weeks
        Assert.assertEquals(invoke("datePart", source, "weeks"), 5);
        Assert.assertEquals(invoke("datePart", source, "week"), 5);
        Assert.assertEquals(invoke("datePart", source, "w"), 5);
        
        // days, day, d
        Assert.assertEquals(invoke("datePart", source, "days"), 30);
        Assert.assertEquals(invoke("datePart", source, "day"), 30);
        Assert.assertEquals(invoke("datePart", source, "d"), 30);
        
        // weekday
        Assert.assertEquals(invoke("datePart", source, "weekday"), "MONDAY");
        
        // time
        Assert.assertEquals(invoke("datePart", source, "time"), 1525132544000l);
    }
    
    // Convert Date to Calendar
    private Calendar dateToCalendar(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        return calendar;
    }
}