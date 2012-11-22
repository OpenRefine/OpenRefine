package com.google.refine.crowdsourcing.tests;

import org.testng.Assert;
import java.util.ArrayList;
import org.testng.annotations.Test;


import com.google.refine.crowdsourcing.CrowdsourcingUtil;

@Test
public class CrowdsourcingUtilTest extends CrowdsourcingUtil {

    public void testParseCmlFields() {
        
        ArrayList<String> result = new ArrayList<String>();
        result.add("id");
        result.add("anchor");
        result.add("link");
        String cml = "Id: \n{{id}}\nAnchor: \n{{anchor}}\nLink: \n{{link}}";
        
        Assert.assertEquals(result, CrowdsourcingUtil.parseCmlFields(cml));
    }

}
