
package com.google.refine.extension.database.pgsql;

import java.sql.Connection;
import java.sql.SQLException;

import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;

@Test(groups = { "requiresPgSQL" })
public class PgSQLConnectionManagerTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

    @BeforeTest
    @Parameters({ "pgSqlDbName", "pgSqlDbHost", "pgSqlDbPort", "pgSqlDbUser", "pgSqlDbPassword", "pgSqlTestTable" })
    public void beforeTest(@Optional(DEFAULT_PGSQL_DB_NAME) String pgSqlDbName, @Optional(DEFAULT_PGSQL_HOST) String pgSqlDbHost,
            @Optional(DEFAULT_PGSQL_PORT) String pgSqlDbPort, @Optional(DEFAULT_PGSQL_USER) String pgSqlDbUser,
            @Optional(DEFAULT_PGSQL_PASSWORD) String pgSqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String pgSqlTestTable) {

        MockitoAnnotations.initMocks(this);

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
