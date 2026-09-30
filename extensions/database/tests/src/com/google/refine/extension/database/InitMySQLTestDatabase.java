
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(groups = { "requiresMySQL" })
public class InitMySQLTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mysqlDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        // System.out.println("@BeforeSuite\n");
        mysqlDbConfig = getMySQLDatabaseConfiguration();

        DBExtensionTestUtils.initTestData(mysqlDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(mysqlDbConfig);
    }
}
