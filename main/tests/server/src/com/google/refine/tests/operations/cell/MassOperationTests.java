package com.google.refine.tests.operations.cell;

import java.util.Properties;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import com.google.refine.process.Process;
import com.google.refine.tests.RefineTest;

public class MassOperationTests extends RefineTest {
    
    List editList = null;
    String editsString = null;
    
    @BeforeMethod
    public void setUp() {
    }
    
    @AfterMethod
    public void tearDown() {
    }
    
    @Test
    public void testReconstructEditString() throws Exception {
        editsString = "[{\"from\":[\"String\"],\"to\":\"newString\",\"type\":\"text\"}]";
        
        editList = MassEditOperation.reconstructEdits(ParsingUtilities.evaluateJsonStringToArray(editsString));
        
        Assert.assertEquals(editList.from, "String");
        Assert.assertEquals(editList.to,"newString" );
        Assert.assertEquals(editList.fromBlank, false);
        Assert.assertEquals(editList.fromError, false);
    }

    @Test
    public void testReconstructEditNumber() throws Exception {
        
    }

    @Test
    public void testReconstructEditEmpty() throws Exception {

    }  

}
