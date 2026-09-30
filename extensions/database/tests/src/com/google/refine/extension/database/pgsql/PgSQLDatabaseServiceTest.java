
package com.google.refine.extension.database.pgsql;

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

@Test(groups = { "requiresPgSQL" })
public class PgSQLDatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    public void beforeTest() {
        testDbConfig = getPgSQLDatabaseConfiguration();
        testTable = getDatabaseTestTable("pgSql", DEFAULT_TEST_TABLE);
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
    }

    @Test
    public void testGetDatabaseUrl() {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);

        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        Connection conn = pgSqlService.getConnection(testDbConfig);

        Assert.assertNotNull(conn);
    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);

        boolean result = pgSqlService.testConnection(testDbConfig);
        Assert.assertEquals(result, true);
    }

    @Test
    public void testConnect() throws DatabaseServiceException {

        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testExecuteQuery() throws DatabaseServiceException {

        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                .get(PgSQLDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery, "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetRows() throws DatabaseServiceException {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                .get(PgSQLDatabaseService.DB_NAME);
        List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
    }

    @Test
    public void testGetInstance() {
        PgSQLDatabaseService instance = PgSQLDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;

        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                .get(PgSQLDatabaseService.DB_NAME);

        dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbColumns);

    }

}
