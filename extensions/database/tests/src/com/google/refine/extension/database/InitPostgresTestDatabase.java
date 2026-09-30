
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;

@Test(groups = { "requiresPgSQL" })
public class InitPostgresTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration pgsqlDbConfig;

    @BeforeSuite
    public void beforeSuite() throws DatabaseServiceException, SQLException {
        String pgSqlDbName = getTestParameter("pgSqlDbName", DEFAULT_PGSQL_DB_NAME);
        String pgSqlDbHost = getTestParameter("pgSqlDbHost", DEFAULT_PGSQL_HOST);
        String pgSqlDbPort = getTestParameter("pgSqlDbPort", DEFAULT_PGSQL_PORT);
        String pgSqlDbUser = getTestParameter("pgSqlDbUser", DEFAULT_PGSQL_USER);
        String pgSqlDbPassword = getTestParameter("pgSqlDbPassword", DEFAULT_PGSQL_PASSWORD);
        String pgSqlTestTable = getTestParameter("pgSqlTestTable", DEFAULT_TEST_TABLE);

        pgsqlDbConfig = new DatabaseConfiguration();
        pgsqlDbConfig.setDatabaseHost(pgSqlDbHost);
        pgsqlDbConfig.setDatabaseName(pgSqlDbName);
        pgsqlDbConfig.setDatabasePassword(pgSqlDbPassword);
        pgsqlDbConfig.setDatabasePort(Integer.parseInt(pgSqlDbPort));
        pgsqlDbConfig.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        pgsqlDbConfig.setDatabaseUser(pgSqlDbUser);
        pgsqlDbConfig.setUseSSL(false);

        DBExtensionTestUtils.initTestData(pgsqlDbConfig);
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(pgsqlDbConfig);
    }
}
