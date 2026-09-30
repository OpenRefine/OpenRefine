
package com.google.refine.extension.database.mysql;

import java.sql.Connection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTestUtils;
import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;

@Test(groups = { "requiresMySQL" })
public class MySQLDatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    public void beforeTest() {
        String mySqlDbName = getTestParameter("mySqlDbName", DEFAULT_MYSQL_DB_NAME);
        String mySqlDbHost = getTestParameter("mySqlDbHost", DEFAULT_MYSQL_HOST);
        String mySqlDbPort = getTestParameter("mySqlDbPort", DEFAULT_MYSQL_PORT);
        String mySqlDbUser = getTestParameter("mySqlDbUser", DEFAULT_MYSQL_USER);
        String mySqlDbPassword = getTestParameter("mySqlDbPassword", DEFAULT_MYSQL_PASSWORD);
        String mySqlTestTable = getTestParameter("mySqlTestTable", DEFAULT_TEST_TABLE);

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
    public void testGetDatabaseUrl() {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);
        // System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);
        Connection conn = pgSqlService.getConnection(testDbConfig);

        Assert.assertNotNull(conn);
    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);

        boolean result = pgSqlService.testConnection(testDbConfig);
        Assert.assertEquals(result, true);
    }

    @Test
    public void testConnect() throws DatabaseServiceException {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testExecuteQuery() throws DatabaseServiceException {

        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                .get(MySQLDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);
        String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery, "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetRows() throws DatabaseServiceException {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                .get(MySQLDatabaseService.DB_NAME);
        List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
    }

    @Test
    public void testGetInstance() {
        MySQLDatabaseService instance = MySQLDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                .get(MySQLDatabaseService.DB_NAME);

        dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbColumns);
    }

}
