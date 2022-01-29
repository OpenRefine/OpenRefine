
package com.google.refine.extension.database.cmd;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.commands.Command;
import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.util.ParsingUtilities;

@Test(groups = { "requiresMySQL" })
public class TestQueryCommandTest extends DBExtensionTests {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable" })
    public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName, @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost,
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

        testTable = mySqlTestTable;
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());

    }

    @Test
    public void testDoPost() throws IOException, ServletException {

        when(request.getParameter("databaseType")).thenReturn(testDbConfig.getDatabaseType());
        when(request.getParameter("databaseServer")).thenReturn(testDbConfig.getDatabaseHost());
        when(request.getParameter("databasePort")).thenReturn("" + testDbConfig.getDatabasePort());
        when(request.getParameter("databaseUser")).thenReturn(testDbConfig.getDatabaseUser());
        when(request.getParameter("databasePassword")).thenReturn(testDbConfig.getDatabasePassword());
        when(request.getParameter("initialDatabase")).thenReturn(testDbConfig.getDatabaseName());
        when(request.getParameter("query")).thenReturn("SELECT count(*) FROM " + testTable);
        when(request.getParameter("csrf_token")).thenReturn(Command.csrfFactory.getFreshToken());

        StringWriter sw = new StringWriter();

        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);
        TestQueryCommand executeQueryCommand = new TestQueryCommand();

        executeQueryCommand.doPost(request, response);

        String result = sw.getBuffer().toString().trim();
        ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);

        String code = json.get("code").asText();
        Assert.assertEquals(code, "ok");

        String queryResult = json.get("QueryResult").asText();
        Assert.assertNotNull(queryResult);

    }

    @Test
    public void testCsrfProtection() throws ServletException, IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        when(response.getWriter()).thenReturn(pw);
        TestQueryCommand connectCommand = new TestQueryCommand();

        connectCommand.doPost(request, response);
        Assert.assertEquals(
                ParsingUtilities.mapper.readValue("{\"code\":\"error\",\"message\":\"Missing or invalid csrf_token parameter\"}",
                        ObjectNode.class),
                ParsingUtilities.mapper.readValue(sw.toString(), ObjectNode.class));
    }

}
