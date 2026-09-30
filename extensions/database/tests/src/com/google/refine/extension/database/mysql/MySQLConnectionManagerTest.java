
package com.google.refine.extension.database.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;

@Test(groups = { "requiresMySQL" })
public class MySQLConnectionManagerTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

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

        // testTable = mySqlTestTable;
        // DBExtensionTestUtils.initTestData(testDbConfig);

        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());

    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {

        boolean conn = MySQLConnectionManager.getInstance().testConnection(testDbConfig);
        Assert.assertEquals(conn, true);
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        Connection conn = MySQLConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);
    }

    @Test
    public void testShutdown() throws DatabaseServiceException, SQLException {

        Connection conn = MySQLConnectionManager.getInstance().getConnection(testDbConfig, true);
        Assert.assertNotNull(conn);

        MySQLConnectionManager.getInstance().shutdown();

        if (conn != null) {
            Assert.assertEquals(conn.isClosed(), true);
        }
    }

}
