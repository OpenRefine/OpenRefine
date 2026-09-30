
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;

@Test(groups = { "requiresMariaDB" })
public class InitMariaDBTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration mariadbDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        String mariadbDbName = getTestParameter("mariadbDbName", DEFAULT_MARIADB_NAME);
        String mariadbDbHost = getTestParameter("mariadbDbHost", DEFAULT_MARIADB_HOST);
        String mariadbDbPort = getTestParameter("mariadbDbPort", DEFAULT_MARIADB_PORT);
        String mariadbDbUser = getTestParameter("mariadbDbUser", DEFAULT_MARIADB_USER);
        String mariadbDbPassword = getTestParameter("mariadbDbPassword", DEFAULT_MARIADB_PASSWORD);
        String mariadbTestTable = getTestParameter("mariadbTestTable", DEFAULT_TEST_TABLE);

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
