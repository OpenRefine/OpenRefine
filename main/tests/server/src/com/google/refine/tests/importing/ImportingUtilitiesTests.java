package com.google.refine.tests.importing;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.ProjectMetadata;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.tests.RefineTest;

public class ImportingUtilitiesTests extends RefineTest {

	@Test
    public void createProjectMetadataTest() throws Exception {
		JSONObject optionObj = new JSONObject("{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
		ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        Assert.assertEquals(pm.getName(), "acme");
        Assert.assertEquals(pm.getEncoding(), "UTF-8");
        Assert.assertTrue(pm.getTags().length == 0);
    }
}
