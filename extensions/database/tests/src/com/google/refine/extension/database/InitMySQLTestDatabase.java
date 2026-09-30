
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mysql.MySQLDatabaseService;

@Test(groups = { "requiresMySQL" })
public class InitMySQLTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mysqlDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        String mySqlDbName = getTestParameter("mySqlDbName", DEFAULT_MYSQL_DB_NAME);
        String mySqlDbHost = getTestParameter("mySqlDbHost", DEFAULT_MYSQL_HOST);
        String mySqlDbPort = getTestParameter("mySqlDbPort", DEFAULT_MYSQL_PORT);
        String mySqlDbUser = getTestParameter("mySqlDbUser", DEFAULT_MYSQL_USER);
        String mySqlDbPassword = getTestParameter("mySqlDbPassword", DEFAULT_MYSQL_PASSWORD);
        String mySqlTestTable = getTestParameter("mySqlTestTable", DEFAULT_TEST_TABLE);

        // System.out.println("@BeforeSuite\n");
        mysqlDbConfig = new DatabaseConfiguration();
        mysqlDbConfig.setDatabaseHost(mySqlDbHost);
        mysqlDbConfig.setDatabaseName(mySqlDbName);
        mysqlDbConfig.setDatabasePassword(mySqlDbPassword);
        mysqlDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        mysqlDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        mysqlDbConfig.setDatabaseUser(mySqlDbUser);
        mysqlDbConfig.setUseSSL(false);

        DBExtensionTestUtils.initTestData(mysqlDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(mysqlDbConfig);
    }
}
