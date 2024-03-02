
package com.google.refine.extension.database;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
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
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.stub.RefineDbServletStub;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

@Test(groups = { "requiresMySQL" })
public class DatabaseImportControllerTest extends DBExtensionTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Project project;
    private ProjectMetadata metadata;
    private ImportingJob job;
    private RefineServlet servlet;

    private String JSON_OPTION = "{\"mode\":\"row-based\"}}";

    private DatabaseConfiguration testDbConfig;

    private String query;

    // System under test
    private DatabaseImportController SUT = null;

    @BeforeMethod
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        File dir = DBExtensionTestUtils.createTempDirectory("OR_DBExtension_Test_WorkspaceDir");
        FileProjectManager.initialize(dir);

        servlet = new RefineDbServletStub();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();
        job = ImportingManager.createJob();

        metadata.setName("Database Import Test Project");
        ProjectManager.singleton.registerProject(project, metadata);
        SUT = new DatabaseImportController();

    }

    @AfterMethod
    public void tearDown() {
        SUT = null;
        request = null;
        response = null;
        project = null;
        metadata = null;
        ImportingManager.disposeJob(job.id);
        job = null;
        // options = null;
    }

    @Test
    public void testDoGet() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            when(response.getWriter()).thenReturn(pw);

            SUT.doGet(request, response);

            String result = sw.getBuffer().toString().trim();
            ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);
            String code = json.get("status").asText();
            String message = json.get("message").asText();
            Assert.assertNotNull(code);
            Assert.assertNotNull(message);
            Assert.assertEquals(code, "error");
            Assert.assertEquals(message, "GET not implemented");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testDoPostInvalidSubCommand() throws IOException, ServletException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(request.getQueryString()).thenReturn(
                "http://127.0.0.1:3333/command/core/importing-controller?controller=database/database-import-controller&subCommand=invalid-sub-command");

        when(response.getWriter()).thenReturn(pw);
        // test
        SUT.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);

        String code = json.get("status").asText();
        String message = json.get("message").asText();
        Assert.assertNotNull(code);
        Assert.assertNotNull(message);
        Assert.assertEquals(code, "error");
        Assert.assertEquals(message, "No such sub command");
    }

    @Test
    public void testDoPostInitializeParser() throws ServletException, IOException {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Map<String, String[]> queryParams = getQueryParams("initialize-parser-ui", -1);
        when(request.getQueryString()).thenReturn(getQueryString(queryParams));
        when(response.getWriter()).thenReturn(pw);

        when(request.getParameterMap()).thenReturn(queryParams);
        when(request.getParameter(anyString()))
                .thenAnswer(i -> queryParams.get(i.getArgument(0)) == null ? null : queryParams.get(i.getArgument(0))[0]);

        SUT.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);

        String status = json.get("status").asText();
        // System.out.println("json::" + json);
        Assert.assertEquals(status, "ok");
    }

    @Test
    public void testDoPostParsePreview() throws IOException, ServletException {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        long jobId = job.id;
        Map queryParams = getQueryParams("parse-preview", jobId);
        when(request.getQueryString()).thenReturn(getQueryString(queryParams));
        when(response.getWriter()).thenReturn(pw);

        Map<String, String[]> allParams = getAllParams(queryParams);
        when(request.getParameterMap()).thenReturn(allParams);
        when(request.getParameter(anyString()))
                .thenAnswer(i -> allParams.get(i.getArgument(0)) == null ? null : allParams.get(i.getArgument(0))[0]);

        SUT.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);

        String status = json.get("status").asText();
        // System.out.println("json::" + json);
        Assert.assertEquals(status, "ok");
    }

    @Test
    public void testDoPostCreateProject() throws IOException, ServletException {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        long jobId = job.id;
        Map<String, String[]> queryParams = getQueryParams("create-project", jobId);
        when(request.getQueryString()).thenReturn(getQueryString(queryParams));
        when(response.getWriter()).thenReturn(pw);

        Map<String, String[]> allParams = getAllParams(queryParams);
        when(request.getParameterMap()).thenReturn(allParams);
        when(request.getParameter(anyString()))
                .thenAnswer(i -> allParams.get(i.getArgument(0)) == null ? null : allParams.get(i.getArgument(0))[0]);

        SUT.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);

        String status = json.get("status").asText();
        // System.out.println("json::" + json);
        Assert.assertEquals(status, "ok");
    }

    @NotNull
    private Map<String, String[]> getAllParams(Map<String, String[]> queryParams) {
        Map<String, String[]> allParams = new HashMap<>(queryParams);
        allParams.putAll(Map.of(
                "databaseType", new String[] { testDbConfig.getDatabaseType() },
                "databaseServer", new String[] { testDbConfig.getDatabaseHost() },
                "databasePort", new String[] { String.valueOf(testDbConfig.getDatabasePort()) },
                "databaseUser", new String[] { testDbConfig.getDatabaseUser() },
                "databasePassword", new String[] { testDbConfig.getDatabasePassword() },
                "initialDatabase", new String[] { testDbConfig.getDatabaseName() },
                "query", new String[] { query },
                "options", new String[] { JSON_OPTION }));
        return Collections.unmodifiableMap(allParams);
    }

    @NotNull
    private Map<String, String[]> getQueryParams(String subCommand, long jobId) {
        Map<String, String[]> queryParams = new HashMap<>();
        queryParams.put("controller", new String[] { "database/database-import-controller" });
        queryParams.put("subCommand", new String[] { subCommand });
        if (jobId >= 0) {
            queryParams.put("jobID", new String[] { String.valueOf(jobId) });
        }
        return Collections.unmodifiableMap(queryParams);
    }

    private String getQueryString(Map<String, String[]> params) {
        return params.entrySet()
                .stream()
                .map(stringEntry -> stringEntry.getValue() + "=" + stringEntry.getValue())
                .collect(Collectors.joining("&"));
    }

    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable" })
    public void beforeTest(
            @Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName, @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost,
            @Optional(DEFAULT_MYSQL_PORT) String mySqlDbPort, @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD) String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String mySqlTestTable) {

        MockitoAnnotations.initMocks(this);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mySqlDbHost);
        testDbConfig.setDatabaseName(mySqlDbName);
        testDbConfig.setDatabasePassword(mySqlDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        testDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mySqlDbUser);
        testDbConfig.setUseSSL(false);
        query = "SELECT count(*) FROM " + mySqlTestTable;

        // testTable = mySqlTestTable;

        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());

    }

}
