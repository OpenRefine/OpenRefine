
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;

@Test(groups = { "requiresMariaDB" })
public class InitMariaDBTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mariadbDbConfig;

    @BeforeSuite
    @Parameters({ "mariadbDbName", "mariadbDbHost", "mariadbDbPort", "mariadbDbUser", "mariadbDbPassword", "mariadbTestTable" })
    public void beforeSuite(
            @Optional(DEFAULT_MARIADB_NAME) String mariadbDbName, @Optional(DEFAULT_MARIADB_HOST) String mariadbDbHost,
            @Optional(DEFAULT_MARIADB_PORT) String mariadbDbPort, @Optional(DEFAULT_MARIADB_USER) String mariadbDbUser,
            @Optional(DEFAULT_MARIADB_PASSWORD) String mariadbDbPassword, @Optional(DEFAULT_TEST_TABLE) String mariadbTestTable)
            throws DatabaseServiceException, SQLException {

        mariadbDbConfig = new DatabaseConfiguration();
        mariadbDbConfig.setDatabaseHost(mariadbDbHost);
        mariadbDbConfig.setDatabaseName(mariadbDbName);
        mariadbDbConfig.setDatabasePassword(mariadbDbPassword);
        mariadbDbConfig.setDatabasePort(Integer.parseInt(mariadbDbPort));
        mariadbDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        mariadbDbConfig.setDatabaseUser(mariadbDbUser);
        mariadbDbConfig.setUseSSL(false);

        DBExtensionTestUtils.initTestData(mariadbDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(mariadbDbConfig);
    }

}
