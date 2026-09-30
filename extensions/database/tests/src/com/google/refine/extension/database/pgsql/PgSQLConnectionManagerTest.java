
package com.google.refine.extension.database.pgsql;

import java.sql.Connection;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;

@Test(groups = { "requiresPgSQL" })
public class PgSQLConnectionManagerTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

    @BeforeTest
    public void beforeTest() {
        String pgSqlDbName = getTestParameter("pgSqlDbName", DEFAULT_PGSQL_DB_NAME);
        String pgSqlDbHost = getTestParameter("pgSqlDbHost", DEFAULT_PGSQL_HOST);
        String pgSqlDbPort = getTestParameter("pgSqlDbPort", DEFAULT_PGSQL_PORT);
        String pgSqlDbUser = getTestParameter("pgSqlDbUser", DEFAULT_PGSQL_USER);
        String pgSqlDbPassword = getTestParameter("pgSqlDbPassword", DEFAULT_PGSQL_PASSWORD);
        String pgSqlTestTable = getTestParameter("pgSqlTestTable", DEFAULT_TEST_TABLE);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(pgSqlDbHost);
        testDbConfig.setDatabaseName(pgSqlDbName);
        testDbConfig.setDatabasePassword(pgSqlDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(pgSqlDbPort));
        testDbConfig.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(pgSqlDbUser);
        testDbConfig.setUseSSL(false);

        // testTable = mySqlTestTable;
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());

    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {

        boolean isConnected = PgSQLConnectionManager.getInstance().testConnection(testDbConfig);
        Assert.assertEquals(isConnected, true);
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        Connection conn = PgSQLConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);
    }

    @Test
    public void testShutdown() throws DatabaseServiceException, SQLException {

        Connection conn = PgSQLConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);

        PgSQLConnectionManager.getInstance().shutdown();

        if (conn != null) {
            Assert.assertEquals(conn.isClosed(), true);
        }

    }

}
