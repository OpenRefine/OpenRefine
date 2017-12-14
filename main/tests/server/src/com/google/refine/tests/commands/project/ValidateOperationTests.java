package com.google.refine.tests.commands.project;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.google.refine.ProjectManager;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.model.medadata.DataPackageMetadata;
import com.google.refine.model.medadata.MetadataFactory;
import com.google.refine.model.medadata.MetadataFormat;
import com.google.refine.model.medadata.validator.ValidateOperation;
import com.google.refine.tests.importers.TsvCsvImporterTests;

import io.frictionlessdata.tableschema.exceptions.ForeignKeyException;
import io.frictionlessdata.tableschema.exceptions.PrimaryKeyException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetadataFactory.class)
public class ValidateOperationTests extends TsvCsvImporterTests  {
    
    Logger logger = LoggerFactory.getLogger(ValidateOperationTests.class.getClass());
    
    SeparatorBasedImporter parser = null;

    // variables
    private DataPackageMetadata dataPackageMetadata;

    // mocks
    ProjectManager projMan = null;
    PrintWriter pw = null;
    
    //dependencies
    ByteArrayInputStream inputStream = null;
    
    @Before
    public void SetUp() throws JSONException, IOException, ValidationException, PrimaryKeyException, ForeignKeyException {
        super.setUp();
        
        // mockup
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        
        pw = mock(PrintWriter.class);

        dataPackageMetadata = new DataPackageMetadata();
        String content = getJSONContent("datapackage-sample.json");
        dataPackageMetadata.loadFromStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name())));
        
        // mock dependencies
        //given
        PowerMockito.mockStatic(MetadataFactory.class);
        BDDMockito.given(MetadataFactory.buildMetadata(MetadataFormat.DATAPACKAGE_METADATA)).willReturn(dataPackageMetadata);
        
        parser = new SeparatorBasedImporter();
        readData();
    }

    @After
    public void TearDown() {
        projMan = null;
        ProjectManager.singleton = null;
        project = null;
        pw = null;
    }

    /**
     *  Contract for a complete working post
     * @throws IOException 
     */
    @Test
    public void setStartProcessTest()  {
        // run
        String options = "{\"columnNames\": [\"Country Name\",\"Country Code\",\"Year\",\"Value\"]}";
        JSONObject optionObj = new JSONObject(options);
        
        // SUT
        JSONObject report = new ValidateOperation(project, optionObj).startProcess();
        
        System.out.println("validation report:" + report.toString(2));
    }
    
     
     private String getJSONContent(String fileName) throws IOException {
         InputStream in = this.getClass().getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
     

     public void readData() throws IOException{
         String sep = ",";
         //create input to test with
          String input =  getFileContent("gdp.csv");
         // create an data type issue on the fly
          input.replace("28434203615.4795", "XXXXXXXXXXXXX");
         
/*         String input = "Country Name,Country Code,Year,Value\n" + 
                 "Arab World,ARB,1968,25760683041.0826\n" + 
                 "Arab World,ARB,1969,28434203615.4795XXX\n";*/
         
         prepareOptions(sep, -1, 0, 0, 1, false, false);
         parseOneFile(parser, new StringReader(input));

         setDataPackageMetaData();
         
         Assert.assertEquals(project.columnModel.columns.size(), 4);
         Assert.assertNotNull(project.getSchema());
     }
     
     private void setDataPackageMetaData() {
         project.setMetadata(MetadataFormat.DATAPACKAGE_METADATA, dataPackageMetadata);
    }

    private String getFileContent(String fileName) throws IOException {
         InputStream in = this.getClass().getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
}
