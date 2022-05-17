package com.google.refine.expr.functions.math;

import com.google.refine.RefineTest;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Properties;

public class ModTest extends RefineTest {

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
        Object result1 = invoke("mod", 5, 3);
        Assert.assertEquals(2 , result1);
    }
    
    @Test
    public void testCall_2(){
        Object result2 = invoke("mod", -5, 3);
        Assert.assertEquals(1 , result2);
    }

    @Test
    public void testCall_3(){
        Object result3 = invoke("mod", 5, -3);
        Assert.assertEquals(-1 , result3);
    }
    @Test
    public void testCall_4(){
        Object result4 = invoke("mod", 10, 10);
        Assert.assertEquals(0 , result4);
    }
    @Test
    public void testCall_5(){
        Object result5 = invoke("mod", 0, 10);
        Assert.assertEquals(10 , result5);
    }
    @Test
    public void testCall_6(){
        Object result6= invoke("mod", -9.1, 5.5);
        Assert.assertTrue(result6 instanceof Integer);
    }
    @Test
    public void testCall_7(){
        Object result7= invoke("mod", 5, 200);
        Assert.assertTrue(result7 instanceof Integer);
    }
}