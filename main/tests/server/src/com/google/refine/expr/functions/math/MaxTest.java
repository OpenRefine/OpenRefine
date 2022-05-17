package com.google.refine.expr.functions.math;

import com.google.refine.RefineTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Properties;

public class MaxTest extends RefineTest {

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
        Object result1 = invoke("max", 3, 4);
        Assert.assertEquals(4.0 , result1);
    }
    @Test
    public void testCall_2(){
        Object result2 = invoke("max", 1.2, 8);
        Assert.assertEquals(8.0 , result2);
    }
    @Test
    public void testCall_3(){
        Object result3 = invoke("max", 0, 10000);
        Assert.assertEquals(10000.0 , result3);
    }
    @Test
    public void testCall_4(){
        Object result4 = invoke("max", -9, 0);
        Assert.assertEquals(0.0 , result4);
    }
    @Test
    public void testCall_5(){
        Object result5 = invoke("max", -9, -3);
        Assert.assertEquals(-3.0 , result5);
    }
    @Test
    public void testCall_6(){
        Object result6= invoke("max", -9.1, 0.5);
        Assert.assertTrue(result6 instanceof Double);
    }
    @Test
    public void testCall_7(){
        Object result7= invoke("max", 5, 200);
        Assert.assertTrue(result7 instanceof Double);
    }
}