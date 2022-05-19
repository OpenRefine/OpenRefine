
package com.google.refine.extension.database.cmd;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hc.core5.http.HttpStatus;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;
import com.google.refine.extension.database.DBExtensionTestUtils;
import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.stub.RefineDbServletStub;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class SavedConnectionCommandTest extends DBExtensionTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DatabaseConfiguration testDbConfig;

    private Project project;
    private ProjectMetadata metadata;
    // private ImportingJob job;
    private RefineServlet servlet;

    // private String JSON_OPTION = "{\"mode\":\"row-based\"}}";

    // System under test
    private SavedConnectionCommand SUT = null;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        File dir = DBExtensionTestUtils.createTempDirectory("OR_DBExtension_Test_WorkspaceDir");
        FileProjectManager.initialize(dir);

        servlet = new RefineDbServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        // job = ImportingManager.createJob();

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
        // options = null;
    }

    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable" })
    public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName, @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost,
            @Optional(DEFAULT_MYSQL_PORT) String mySqlDbPort, @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD) String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String mySqlTestTable) {

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
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());
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
    public void testDoPost() throws IOException, ServletException {

        when(request.getParameter("connectionName")).thenReturn("test-db-name");
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        SUT.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Valid response Message expected!");

        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);
        // System.out.println("json:" + json);

        ArrayNode savedConnections = (ArrayNode) json.get("savedConnections");
        Assert.assertNotNull(savedConnections);

        int len = savedConnections.size();

        Assert.assertEquals(len, 1);
    }

    @Test
    public void testDoGet() throws IOException, ServletException {
        String testDbName = "testLocalDb";
        // add saved connection
        saveDatabaseConfiguration(testDbName);

        when(request.getParameter("connectionName")).thenReturn(testDbName);
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        SUT.doGet(request, response);

        ObjectNode json = ParsingUtilities.mapper.readValue(sw.getBuffer().toString().trim(), ObjectNode.class);

        ArrayNode savedConnections = (ArrayNode) json.get("savedConnections");
        Assert.assertNotNull(savedConnections);

        Assert.assertEquals(savedConnections.size(), 1);

        ObjectNode sc = (ObjectNode) savedConnections.get(0);
        String connName = sc.get("connectionName").asText();
        Assert.assertEquals(connName, testDbName);
    }

    @Test
    public void testDoPut() throws IOException, ServletException {
        String testDbName = "testLocalDb";
        saveDatabaseConfiguration(testDbName);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        // modify database config
        String newHost = "localhost";
        when(request.getParameter("connectionName")).thenReturn(testDbName);
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(newHost);
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        SUT.doPut(request, response);

        ObjectNode json = ParsingUtilities.mapper.readValue(sw.getBuffer().toString().trim(), ObjectNode.class);
        ArrayNode savedConnections = (ArrayNode) json.get("savedConnections");
        Assert.assertNotNull(savedConnections);

        Assert.assertEquals(savedConnections.size(), 1);

        ObjectNode sc = (ObjectNode) savedConnections.get(0);
        String newDbHost = sc.get("databaseHost").asText();
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

            ObjectNode json = ParsingUtilities.mapper.readValue(sw.getBuffer().toString().trim(), ObjectNode.class);
            ArrayNode savedConnections = (ArrayNode) json.get("savedConnections");
            Assert.assertNotNull(savedConnections);

            Assert.assertEquals(savedConnections.size(), 0);

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

            ObjectNode json = ParsingUtilities.mapper.createObjectNode();

            Assert.assertNotNull(json);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Added to check XSS invalid tokens
     * 
     * @throws IOException
     * @throws ServletException
     */
    @Test
    public void testDoPostInvalidConnectionName() throws IOException, ServletException {

        when(request.getParameter("connectionName")).thenReturn("<img></img>");
        when(request.getParameter("databaseType")).thenReturn(MySQLDatabaseService.DB_NAME);
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        SUT.doPost(request, response);

        verify(response, times(1)).sendError(HttpStatus.SC_BAD_REQUEST, "Connection Name is Invalid. Expecting [a-zA-Z0-9._-]");
    }

    @Test
    public void testCsrfProtection() throws ServletException, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);

        SUT.doPost(request, response);
        Assert.assertEquals(
                ParsingUtilities.mapper.readValue("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}",
                        ObjectNode.class),
                ParsingUtilities.mapper.readValue(sw.toString(), ObjectNode.class));
    }

}
