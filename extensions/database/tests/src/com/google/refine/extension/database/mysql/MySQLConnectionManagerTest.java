
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
        testDbConfig = getMySQLDatabaseConfiguration();

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
