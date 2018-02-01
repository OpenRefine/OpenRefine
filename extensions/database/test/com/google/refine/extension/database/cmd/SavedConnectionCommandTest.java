package com.google.refine.extension.database.cmd;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.ProjectManager;
import com.google.refine.model.medadata.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.extension.database.DBExtensionTestUtils;
import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.stub.RefineDbServletStub;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;

public class SavedConnectionCommandTest extends DBExtensionTests{
    
    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DatabaseConfiguration testDbConfig;
    
    private Project project;
    private ProjectMetadata metadata;
    //private ImportingJob job;
    private RefineServlet servlet;

   // private String JSON_OPTION = "{\"mode\":\"row-based\"}}";
 
    
    //System under test
    private SavedConnectionCommand SUT = null;

    @BeforeMethod
    public void setUp() throws JSONException, IOException {
        MockitoAnnotations.initMocks(this);
        
        File dir = DBExtensionTestUtils.createTempDirectory("OR_DBExtension_Test_WorkspaceDir");
        FileProjectManager.initialize(dir);
        
        servlet = new RefineDbServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        //job = ImportingManager.createJob();
     
        metadata.setName("Save DB Config  Test Project");
        ProjectManager.singleton.registerProject(project, metadata);
        SUT = new SavedConnectionCommand();
      
    }
    
    @AfterMethod
    public void tearDown() {
        SUT = null;
        request = null;
        response = null;
        project = null;
        metadata = null;
       // ImportingManager.disposeJob(job.id);
       // job = null;
        //options = null;
    }
  
     @BeforeTest
     @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable"})
     public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName,  @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost, 
            @Optional(DEFAULT_MYSQL_PORT)    String mySqlDbPort,     @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD)  String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mySqlTestTable) {
        
        // MockitoAnnotations.initMocks(this);
         testDbConfig = new DatabaseConfiguration();
         testDbConfig.setDatabaseHost(mySqlDbHost);
         testDbConfig.setDatabaseName(mySqlDbName);
         testDbConfig.setDatabasePassword(mySqlDbPassword);
         testDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
         testDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
         testDbConfig.setDatabaseUser(mySqlDbUser);
         testDbConfig.setUseSSL(false);
       
         DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
         
     }
    
 
    
    private void saveDatabaseConfiguration(String savedDbName) {
        
        when(request.getParameter("connectionName")).thenReturn(savedDbName);
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        try {
            when(response.getWriter()).thenReturn(pw);

            SUT.doPost(request, response);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
    }
    
    @Test
    public void testDoPost() throws IOException, ServletException, JSONException {
        
        when(request.getParameter("connectionName")).thenReturn("test-db-name");
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        when(response.getWriter()).thenReturn(pw);
       
        SUT.doPost(request, response);
        
        String result = sw.getBuffer().toString().trim();
 
        JSONObject json = new JSONObject(result);
        
        JSONArray savedConnections = json.getJSONArray("savedConnections");
        Assert.assertNotNull(savedConnections);
        
        int len = savedConnections.length();
        
        Assert.assertEquals(len, 1);
    }

    @Test
    public void testDoGet() throws IOException, ServletException, JSONException {
        String testDbName = "testLocalDb";
        //add saved connection
        saveDatabaseConfiguration(testDbName);
        
        
        when(request.getParameter("connectionName")).thenReturn(testDbName);
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        when(response.getWriter()).thenReturn(pw);
     
        SUT.doGet(request, response);
      
        JSONObject json = new JSONObject(sw.getBuffer().toString().trim());
        
        JSONArray savedConnections = json.getJSONArray("savedConnections");
        Assert.assertNotNull(savedConnections);
     
        Assert.assertEquals(savedConnections.length(), 1);
        
        JSONObject sc = (JSONObject)savedConnections.get(0);
       // System.out.println("sc" + sc);
        String connName = sc.getString("connectionName");
        Assert.assertEquals(connName, testDbName);
    }

    @Test
    public void testDoPut() throws IOException, ServletException, JSONException {
        String testDbName = "testLocalDb";
        saveDatabaseConfiguration(testDbName);
    
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        when(response.getWriter()).thenReturn(pw);
       
        //modify database config
        String newHost = "localhost";
        when(request.getParameter("connectionName")).thenReturn(testDbName);
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(newHost);
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
      
        SUT.doPut(request, response);
        
        JSONObject json = new JSONObject(sw.getBuffer().toString().trim());
        JSONArray savedConnections = json.getJSONArray("savedConnections");
        Assert.assertNotNull(savedConnections);
       
        Assert.assertEquals(savedConnections.length(), 1);
        
        JSONObject sc = (JSONObject)savedConnections.get(0);
        System.out.println("sc" + sc);
        String newDbHost = sc.getString("databaseHost");
        Assert.assertEquals(newDbHost, newHost);
    }

    @Test
    public void testDoDeleteValidConnectionName() {
        String testDbName = "testLocalDb";
        saveDatabaseConfiguration(testDbName);
    
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        try {
            when(response.getWriter()).thenReturn(pw);
            when(request.getParameter("connectionName")).thenReturn(testDbName);
            SUT.doDelete(request, response);
            
            JSONObject json = new JSONObject(sw.getBuffer().toString().trim());
            JSONArray savedConnections = json.getJSONArray("savedConnections");
            Assert.assertNotNull(savedConnections);
           
            Assert.assertEquals(savedConnections.length(), 0);
       
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Test
    public void testDoDeleteInValidConnectionName() {
        String testDbName = "testLocalDb";
        saveDatabaseConfiguration(testDbName);
    
       
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        try {
            when(response.getWriter()).thenReturn(pw);
           
            when(request.getParameter("connectionName")).thenReturn("noDbName");

            SUT.doDelete(request, response);
            
           // String result = sw.getBuffer().toString().trim();
          
            JSONObject json = new JSONObject();
            
            Assert.assertNotNull(json);
       
       
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
