
package com.google.refine.extension.database;

import java.sql.Connection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;
import com.google.refine.extension.database.sqlite.SQLiteDatabaseService;

public class DatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    public void beforeTest() {
        testDbConfig = getMySQLDatabaseConfiguration();
        testTable = getDatabaseTestTable("mySql", DEFAULT_TEST_TABLE);
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

    @Test
    public void testBuildCountQuery() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String basicSelectQuery = "SELECT * FROM " + testTable + ";";
        String countQuery = dbService.buildCountQuery(basicSelectQuery);
        Assert.assertNotNull(countQuery);
        Assert.assertEquals(countQuery, "SELECT COUNT(*) FROM (SELECT * FROM " + testTable + ") AS TOTAL;");
    }

    @Test
    public void testBuildCountQueryFromSpaciousBasicQuery() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String basicSelectQuery = " SELECT * FROM " + testTable + "  ;   ";
        String countQuery = dbService.buildCountQuery(basicSelectQuery);
        Assert.assertNotNull(countQuery);
        Assert.assertEquals(countQuery, "SELECT COUNT(*) FROM (SELECT * FROM " + testTable + ") AS TOTAL;");
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

    @Test(groups = { "requiresMySQL" })
    public void testGetCount() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        Integer count = dbService.getCount(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(count);
        Assert.assertEquals(count, 1);
    }
}
