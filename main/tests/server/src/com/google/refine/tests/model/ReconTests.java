package com.google.refine.tests.model;

import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.tests.RefineTest;

public class ReconTests extends RefineTest {

    @Override
    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    private class StandardReconConfigTest extends StandardReconConfig {
        public StandardReconConfigTest() {
            super("", "", "", "", "", false, new ArrayList<ColumnDetail>());
        }

        public double wordDistanceTest(String s1, String s2) {
            return wordDistance(s1, s2);
        }
    }

    @Test
    public void wordDistance() {
        StandardReconConfigTest t = new StandardReconConfigTest();
        double r = t.wordDistanceTest("Foo", "Foo bar");

        Assert.assertEquals(r,0.5);
    }

    @Test
    public void wordDistanceOnlyStopwords() {
        StandardReconConfigTest t = new StandardReconConfigTest();
        double r = t.wordDistanceTest("On and On", "On and On and On");

        Assert.assertTrue(!Double.isInfinite(r));
        Assert.assertTrue(!Double.isNaN(r));
    }
}
