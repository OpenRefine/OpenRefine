package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = { "requiresMariaDB" })
public class InitDuckDBTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration duckdbDbConfig;

    @BeforeSuite
    @Parameters({ "duckdbDbName", "duckdbTestTable" })
    public void beforeSuite(
            @Optional(DEFAULT_DUCKDB_DB_NAME) String duckdbDbName, @Optional(DEFAULT_TEST_TABLE) String duckdbTestTable)
            throws DatabaseServiceException, SQLException {

        duckdbDbConfig = new DatabaseConfiguration();
        duckdbDbConfig.setDatabaseName(duckdbDbName);
        duckdbDbConfig.setDatabaseType(DUCKDB_DB_NAME);

        DBExtensionTestUtils.initTestData(duckdbDbConfig, duckdbTestTable);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(duckdbDbConfig);
    }
}
