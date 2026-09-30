
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Test(groups = { "requiresMariaDB" })
public class InitMariaDBTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mariadbDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        mariadbDbConfig = getMariaDBDatabaseConfiguration();

        DBExtensionTestUtils.initTestData(mariadbDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(mariadbDbConfig);
    }

}
