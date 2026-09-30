
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(groups = { "requiresPgSQL" })
public class InitPostgresTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration pgsqlDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        pgsqlDbConfig = getPgSQLDatabaseConfiguration();

        DBExtensionTestUtils.initTestData(pgsqlDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(pgsqlDbConfig);
    }
}
