
package com.google.refine.tests.importing;

import java.util.LinkedList;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.tests.importers.ImporterTest;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
    }
    
    @Test
    public void createProjectMetadataTest()
            throws Exception {
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
                "{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
        ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        Assert.assertEquals(pm.getName(), "acme");
        Assert.assertEquals(pm.getEncoding(), "UTF-8");
        Assert.assertTrue(pm.getTags().length == 0);
    }
    
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void testZipSlip() {
        // For CVE-2018-19859, issue #1840
    	ImportingUtilities.allocateFile(workspaceDir, "../../script.sh");
    }
    
    private ObjectNode getNestedOptions(ImportingJob job, TreeImportingParserBase parser) {
        ObjectNode options = parser.createParserUIInitializationData(
                job, new LinkedList<>(), "text/json");
        
        ArrayNode path = ParsingUtilities.mapper.createArrayNode();
        path.add("results");
        path.add("result");
        
        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }
}
