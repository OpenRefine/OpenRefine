package com.google.refine.tests.commands.project;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.google.refine.ProjectManager;
import com.google.refine.commands.project.ValidateSchemaCommand;
import com.google.refine.importers.SeparatorBasedImporter;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.model.medadata.DataPackageMetadata;
import com.google.refine.model.medadata.MetadataFactory;
import com.google.refine.model.medadata.MetadataFormat;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.tests.RefineServletStub;
import com.google.refine.tests.importers.TsvCsvImporterTests;

import io.frictionlessdata.tableschema.Schema;
import io.frictionlessdata.tableschema.exceptions.ForeignKeyException;
import io.frictionlessdata.tableschema.exceptions.PrimaryKeyException;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MetadataFactory.class)
public class ValidateSchemaCommandTests extends TsvCsvImporterTests  {
    
    Logger logger = LoggerFactory.getLogger(ValidateSchemaCommandTests.class.getClass());
    
    // System Under Test
    ValidateSchemaCommand SUT = null;
    
    SeparatorBasedImporter parser = null;

    // variables
    long PROJECT_ID_LONG = 1234;
    String PROJECT_ID = "1234";
    private DataPackageMetadata dataPackageMetadata;
//    private ProjectMetadata projectMetadata;

    // mocks
    HttpServletRequest request = null;
    HttpServletResponse response = null;
    ProjectManager projMan = null;
    Project project = null;
    PrintWriter pw = null;
    
    //dependencies
    ByteArrayInputStream inputStream = null;
    
    @Before
    public void SetUp() throws JSONException, IOException, ValidationException, PrimaryKeyException, ForeignKeyException {
        
        /** 
        servlet = new RefineServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        job = ImportingManager.createJob();
        
        options = Mockito.mock(JSONObject.class);
        */
        super.setUp();
        
        
        // mockup
        projMan = mock(ProjectManager.class);
        ProjectManager.singleton = projMan;
        
        // reseat project object
//        projectMock = mock(Project.class);
        
        pw = mock(PrintWriter.class);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SUT = new ValidateSchemaCommand();
        
        dataPackageMetadata = new DataPackageMetadata();
        String content = getJSONContent("datapackage-sample.json");
        
        // mock dependencies
        when(request.getParameter("project")).thenReturn(PROJECT_ID);
        String options = "{\"columnNames\": [\"Country Name\",\"Country Code\",\"Year\",\"Value\"]}";
        when(request.getParameter("options")).thenReturn(options);
        
//        when(projMan.getProject(anyLong())).thenReturn(projectMock);
        
        // below two should return the same object
/*        when(projectMock.getMetadata(MetadataFormat.DATAPACKAGE_METADATA)).thenReturn(dataPackageMetadata);
        when(projectMock.getDataPackageMetadata()).thenReturn(dataPackageMetadata);
        when(projectMock.getSchema()).thenReturn(
                new Schema(dataPackageMetadata.getPackage().getResources().get(0).getSchema(), true));*/
        //given
        PowerMockito.mockStatic(MetadataFactory.class);
        BDDMockito.given(MetadataFactory.buildMetadata(MetadataFormat.DATAPACKAGE_METADATA)).willReturn(dataPackageMetadata);
        
        try {
            when(response.getWriter()).thenReturn(pw);
        } catch (IOException e1) {
            Assert.fail();
        }
        
        parser = new SeparatorBasedImporter();
        readData();
    }

    @After
    public void TearDown() {
        SUT = null;

        projMan = null;
        ProjectManager.singleton = null;
        project = null;
        pw = null;
        request = null;
        response = null;
    }

    /**
     *  Contract for a complete working post
     * @throws IOException 
     */
    @Test
    public void setStartProcess()  {
        // run
        try {
            SUT.doPost(request, response);
        } catch (ServletException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }

        // verify
        try {
            verify(response, times(1)).getWriter();
        } catch (IOException e) {
            Assert.fail();
        }
        verify(pw, times(1)).write("{ \"code\" : \"ok\" }");
        
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
         String inputSeparator =  sep == null ? "\t" : sep;
         String input =  getFileContent("gdp.csv");
         
         prepareOptions(sep, -1, 0, 0, 2, false, false);
         parseOneFile(parser, new StringReader(input));

         Assert.assertEquals(project.columnModel.columns.size(), 4);
         Assert.assertNotNull(project.getProjectMetadata());
         Assert.assertNotNull(project.getSchema());
         /**
         Assert.assertEquals(project.columnModel.columns.get(0).getName(), "col1 sub1");
         Assert.assertEquals(project.columnModel.columns.get(1).getName(), "col2 sub2");
         Assert.assertEquals(project.columnModel.columns.get(2).getName(), "col3 sub3");
         Assert.assertEquals(project.rows.size(), 1);
         Assert.assertEquals(project.rows.get(0).cells.size(), 3);
         Assert.assertEquals(project.rows.get(0).cells.get(0).value, "data1");
         Assert.assertEquals(project.rows.get(0).cells.get(1).value, "data2");
         Assert.assertEquals(project.rows.get(0).cells.get(2).value, "data3");
         */
     }
     
     private String getFileContent(String fileName) throws IOException {
         InputStream in = this.getClass().getClassLoader()
                 .getResourceAsStream(fileName);
         String content = org.apache.commons.io.IOUtils.toString(in);
         
         return content;
     }
}
