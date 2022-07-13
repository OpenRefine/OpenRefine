
package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;

@Test(groups = { "requiresPgSQL" })
public class InitPostgresTestDatabase extends DBExtensionTests {

    private DatabaseConfiguration pgsqlDbConfig;

    @BeforeSuite
    @Parameters({ "pgSqlDbName", "pgSqlDbHost", "pgSqlDbPort", "pgSqlDbUser", "pgSqlDbPassword", "pgSqlTestTable" })
    public void beforeSuite(
            @Optional(DEFAULT_PGSQL_DB_NAME) String pgSqlDbName, @Optional(DEFAULT_PGSQL_HOST) String pgSqlDbHost,
            @Optional(DEFAULT_PGSQL_PORT) String pgSqlDbPort, @Optional(DEFAULT_PGSQL_USER) String pgSqlDbUser,
            @Optional(DEFAULT_PGSQL_PASSWORD) String pgSqlDbPassword, @Optional(DEFAULT_TEST_TABLE) String pgSqlTestTable)
            throws DatabaseServiceException, SQLException {

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
