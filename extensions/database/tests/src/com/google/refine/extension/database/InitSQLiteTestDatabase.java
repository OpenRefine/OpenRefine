
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
        String sqliteDbName = getTestParameter("sqliteDbName", DEFAULT_SQLITE_DB_NAME);
        String sqliteTestTable = getTestParameter("sqliteTestTable", DEFAULT_SQLITE_TEST_TABLE);

        sqliteDbConfig = new DatabaseConfiguration();
        sqliteDbConfig.setDatabaseName(sqliteDbName);

        DBExtensionTestUtils.initTestData(sqliteDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(sqliteDbConfig);
    }
}
