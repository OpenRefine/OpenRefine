
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = { "requiresMariaDB" })
public class InitSQLiteTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration sqliteDbConfig;

    @BeforeSuite
    @Parameters({ "sqliteDbName", "sqliteTestTable" })
    public void beforeSuite(
            @Optional(DEFAULT_SQLITE_DB_NAME) String sqliteDbName, @Optional(DEFAULT_TEST_TABLE) String sqliteTestTable)
            throws DatabaseServiceException, SQLException {

        sqliteDbConfig = new DatabaseConfiguration();
        sqliteDbConfig.setDatabaseName(sqliteDbName);

        DBExtensionTestUtils.initTestData(sqliteDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(sqliteDbConfig);
    }
}
