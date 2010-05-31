package com.metaweb.gridworks.tests.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.metaweb.gridworks.tests.GridworksTest;
import com.metaweb.gridworks.util.ParsingUtilities;

public class ParsingUtilitiesTests extends GridworksTest {
    
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    //--------------evaluateJsonStringToObject tests-----------------------

    @Test
    public void evaluateJsonStringToObjectRegressionTest(){
        try {
            JSONObject o = ParsingUtilities.evaluateJsonStringToObject("{\"foo\":\"bar\"}");
            Assert.assertNotNull(o);
            Assert.assertEquals("bar", o.getString("foo"));
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void evaluateJsonStringToObjectWithNullParameters(){
        try {
            Assert.assertNull(ParsingUtilities.evaluateJsonStringToObject(null));
            Assert.fail();
        } catch (IllegalArgumentException e){
            //expected
        } catch (JSONException e) {
            Assert.fail();
        }
    }

    @Test
    public void evaluateJsonStringToObjectWithMalformedParameters(){
        try {
            ParsingUtilities.evaluateJsonStringToObject("malformed");
            Assert.fail();
        } catch (JSONException e) {
            //expected
        }
    }
}
