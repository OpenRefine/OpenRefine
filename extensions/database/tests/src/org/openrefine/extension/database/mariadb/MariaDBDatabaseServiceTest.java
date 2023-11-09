
package org.openrefine.extension.database.mariadb;

import java.sql.Connection;
import java.util.List;

import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.openrefine.extension.database.DBExtensionTestUtils;
import org.openrefine.extension.database.DBExtensionTests;
import org.openrefine.extension.database.DatabaseConfiguration;
import org.openrefine.extension.database.DatabaseService;
import org.openrefine.extension.database.DatabaseServiceException;
import org.openrefine.extension.database.mariadb.MariaDBDatabaseService;
import org.openrefine.extension.database.model.DatabaseColumn;
import org.openrefine.extension.database.model.DatabaseInfo;
import org.openrefine.extension.database.model.DatabaseRow;

@Test(groups = { "requiresMariaDB" })
public class MariaDBDatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

    private String testTable;

    @BeforeTest
    @Parameters({ "mariaDbName", "mariaDbHost", "mariaDbPort", "mariaDbUser", "mariaDbPassword", "mariaDbTestTable" })
    public void beforeTest(@Optional(DEFAULT_MARIADB_NAME) String mariaDbName, @Optional(DEFAULT_MARIADB_HOST) String mariaDbHost,
            @Optional(DEFAULT_MARIADB_PORT) String mariaDbPort, @Optional(DEFAULT_MARIADB_USER) String mariaDbUser,
            @Optional(DEFAULT_MARIADB_PASSWORD) String mariaDbPassword, @Optional(DEFAULT_TEST_TABLE) String mariaDbTestTable) {

        MockitoAnnotations.initMocks(this);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mariaDbHost);
        testDbConfig.setDatabaseName(mariaDbName);
        testDbConfig.setDatabasePassword(mariaDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mariaDbPort));
        testDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mariaDbUser);
        testDbConfig.setUseSSL(false);

        testTable = mariaDbTestTable;
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());

    }

    @Test
    public void testGetDatabaseUrl() {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);
        // System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        Connection conn = pgSqlService.getConnection(testDbConfig);

        Assert.assertNotNull(conn);
    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);

        boolean result = pgSqlService.testConnection(testDbConfig);
        Assert.assertEquals(result, true);
    }

    @Test
    public void testConnect() throws DatabaseServiceException {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testExecuteQuery() throws DatabaseServiceException {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                .get(MariaDBDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery, "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetRows() throws DatabaseServiceException {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                .get(MariaDBDatabaseService.DB_NAME);
        List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
    }

    @Test
    public void testGetInstance() {
        MariaDBDatabaseService instance = MariaDBDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;

        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                .get(MariaDBDatabaseService.DB_NAME);

        dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbColumns);
    }

}
