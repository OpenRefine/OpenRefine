
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(groups = { "requiresMariaDB" })
public class InitSQLiteTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration sqliteDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        sqliteDbConfig = getSQLiteDatabaseConfiguration();
        DBExtensionTestUtils.initTestData(sqliteDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(sqliteDbConfig);
    }
}
