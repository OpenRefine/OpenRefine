
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mysql.MySQLDatabaseService;

@Test(groups = { "requiresMySQL" })
public class InitMySQLTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mysqlDbConfig;

    @BeforeSuite
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable" })
    public void beforeSuite(
            @Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName, @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost,
            @Optional(DEFAULT_MYSQL_PORT) String mySqlDbPort, @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD) String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String mySqlTestTable)
            throws DatabaseServiceException, SQLException {

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
