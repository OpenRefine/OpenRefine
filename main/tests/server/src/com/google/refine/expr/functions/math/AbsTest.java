package com.google.refine.expr.functions.math;

import com.google.refine.RefineTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Properties;

public class AbsTest extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @BeforeMethod
    public void setUp() throws Exception {
        bindings = new Properties();
    }

    @Test
    public void testCall_1(){
        Object result1 = invoke("abs", 0);
        Assert.assertEquals(0.0 , result1);
    }
    @Test
    public void testCall_2(){
        Object result2 = invoke("abs", -5);
        Assert.assertEquals(5.0 , result2);
    }
    @Test
    public void testCall_3(){
        Object result3 = invoke("abs", 5);
        Assert.assertEquals(5.0, result3);
    }
    @Test
    public void testCall_4(){
        Object result4 = invoke("abs", 5.5);
        Assert.assertEquals(5.5 , result4);
    }
    @Test
    public void testCall_5(){
        Object result5 = invoke("abs", -5.5);
        Assert.assertEquals(5.5 , result5);
    }
    @Test
    public void testCall_6(){
        Object result6= invoke("abs", -100.5);
        Assert.assertTrue(result6 instanceof Double);
    }
    @Test
    public void testCall_7(){
        Object result7= invoke("abs", 200.8);
        Assert.assertTrue(result7 instanceof Double);
    }
}