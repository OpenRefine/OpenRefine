
package com.google.refine.extension.database.mariadb;

import java.sql.Connection;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;

@Test(groups = { "requiresMariaDB" })
public class MariaDBConnectionManagerTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

    @BeforeTest
    public void beforeTest() {
        String mariaDbName = getTestParameter("mariadbDbName", DEFAULT_MARIADB_NAME);
        String mariaDbHost = getTestParameter("mariadbDbHost", DEFAULT_MARIADB_HOST);
        String mariaDbPort = getTestParameter("mariadbDbPort", DEFAULT_MARIADB_PORT);
        String mariaDbUser = getTestParameter("mariadbDbUser", DEFAULT_MARIADB_USER);
        String mariaDbPassword = getTestParameter("mariadbDbPassword", DEFAULT_MARIADB_PASSWORD);
        String mariaDbTestTable = getTestParameter("mariadbTestTable", DEFAULT_TEST_TABLE);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mariaDbHost);
        testDbConfig.setDatabaseName(mariaDbName);
        testDbConfig.setDatabasePassword(mariaDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mariaDbPort));
        testDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mariaDbUser);
        testDbConfig.setUseSSL(false);

//        testTable = mariaDbTestTable;
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());

    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        boolean conn = MariaDBConnectionManager.getInstance().testConnection(testDbConfig);
        Assert.assertEquals(conn, true);
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {
        Connection conn = MariaDBConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);
    }

    @Test
    public void testShutdown() throws DatabaseServiceException, SQLException {
        Connection conn = MariaDBConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);

        MariaDBConnectionManager.getInstance().shutdown();

        if (conn != null) {
            Assert.assertEquals(conn.isClosed(), true);
        }

    }

}
