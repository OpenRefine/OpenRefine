
package com.google.refine.extension.database;

import java.sql.Connection;
import java.util.List;

import com.google.refine.extension.database.sqlite.SQLiteDatabaseService;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;

public class DatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable" })
    public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName, @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost,
            @Optional(DEFAULT_MYSQL_PORT) String mySqlDbPort, @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD) String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String mySqlTestTable) {

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

        DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());
        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());

    }

    @Test
    public void testGetDatabaseUrl() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String dbUrl = dbService.getDatabaseUrl(testDbConfig);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }

    @Test(groups = { "requiresPgSQL" })
    public void testGetPgSQLDBService() {
        DatabaseService dbService = DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);
        Assert.assertEquals(dbService.getClass(), PgSQLDatabaseService.class);
    }

    @Test(groups = { "requiresMySQL" })
    public void testGetMySQLDBService() {

        DatabaseService dbService = DatabaseService.get(MySQLDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);
        Assert.assertEquals(dbService.getClass(), MySQLDatabaseService.class);
    }

    @Test(groups = { "requiresMariaDB" })
    public void testGetMariaDBSQLDBService() {

        DatabaseService dbService = DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);
        Assert.assertEquals(dbService.getClass(), MariaDBDatabaseService.class);
    }

    @Test(groups = { "requiresSQLite" })
    public void testGetSQLiteDBService() {

        DatabaseService dbService = DatabaseService.get(SQLiteDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);
        Assert.assertEquals(dbService.getClass(), SQLiteDatabaseService.class);
    }

    @Test(groups = { "requiresMySQL" })
    public void testGetConnection() throws DatabaseServiceException {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        Connection conn = dbService.getConnection(testDbConfig);
        Assert.assertNotNull(conn);
    }

    @Test(groups = { "requiresMySQL" })
    public void testTestConnection() throws DatabaseServiceException {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        boolean result = dbService.testConnection(testDbConfig);
        Assert.assertEquals(result, true);
    }

    @Test(groups = { "requiresMySQL" })
    public void testConnect() throws DatabaseServiceException {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        DatabaseInfo databaseInfo = dbService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test(groups = { "requiresMySQL" })
    public void testExecuteQuery() throws DatabaseServiceException {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        DatabaseInfo databaseInfo = dbService.testQuery(testDbConfig,
                "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String limitQuery = dbService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery, "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test(groups = { "requiresMySQL" })
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;

        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        dbColumns = dbService.getColumns(testDbConfig, "SELECT * FROM " + testTable);
        Assert.assertNotNull(dbColumns);

        int cols = dbColumns.size();
        Assert.assertEquals(cols, 10);
    }

    @Test(groups = { "requiresMySQL" })
    public void testGetRows() throws DatabaseServiceException {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        List<DatabaseRow> dbRows = dbService.getRows(testDbConfig,
                "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
        Assert.assertEquals(dbRows.size(), 1);
    }

}
