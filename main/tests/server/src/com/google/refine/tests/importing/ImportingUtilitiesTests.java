
package com.google.refine.tests.importing;

import java.io.InputStream;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.importers.XmlImporter;
import com.google.refine.importers.tree.TreeImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Column;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.tests.importers.ImporterTest;
import com.google.refine.util.JSONUtilities;

public class ImportingUtilitiesTests extends ImporterTest {

    @Override
    @BeforeMethod
    public void setUp(){
        super.setUp();
    }
    
    @Test
    public void createProjectMetadataTest()
            throws Exception {
        JSONObject optionObj = new JSONObject(
                "{\"projectName\":\"acme\",\"projectTags\":[],\"created\":\"2017-12-18T13:28:40.659\",\"modified\":\"2017-12-20T09:28:06.654\",\"creator\":\"\",\"contributors\":\"\",\"subject\":\"\",\"description\":\"\",\"rowCount\":50,\"customMetadata\":{}}");
        ProjectMetadata pm = ImportingUtilities.createProjectMetadata(optionObj);
        Assert.assertEquals(pm.getName(), "acme");
        Assert.assertEquals(pm.getEncoding(), "UTF-8");
        Assert.assertTrue(pm.getTags().length == 0);
    }

    @Test
    public void inferColumnTypeTest()
            throws Exception {
        ImportingManager.registerFormat("text/xml", "XML files", "XmlParserUI", new com.google.refine.importers.XmlImporter());
        XmlImporter xmlImporter = new XmlImporter();
        String fileName = "jorf.xml";
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(fileName);
        options = getNestedOptions(job, xmlImporter);
        job.getRetrievalRecord();
        
        parseOneInputStream(new XmlImporter(),
                in,
                options);
        
        ImportingUtilities.inferColumnType(project);
        
        Assert.assertTrue(project.columnModel.columns.size() == 58);
        Assert.assertTrue(project.columnModel.getColumnByName("result - source_id").getType().equals("string"));
        Assert.assertTrue(project.columnModel.getColumnByName("result - person - sexe").getType().equals("boolean"));
    }
    
    private JSONObject getNestedOptions(ImportingJob job, TreeImportingParserBase parser) {
        JSONObject options = parser.createParserUIInitializationData(
                job, new LinkedList<JSONObject>(), "text/json");
        
        JSONArray path = new JSONArray();
        JSONUtilities.append(path, "results");
        JSONUtilities.append(path, "result");
//        JSONUtilities.append(path, "object");
        
        JSONUtilities.safePut(options, "recordPath", path);
        return options;
    }
}
