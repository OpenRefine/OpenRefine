package com.google.refine.extension.database;

import java.sql.SQLException;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;

public class DatabaseTestConfig extends DBExtensionTests {
    
    private DatabaseConfiguration mysqlDbConfig;
    private DatabaseConfiguration pgsqlDbConfig;
    private DatabaseConfiguration mariadbDbConfig;

    @BeforeSuite
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable",
                  "pgSqlDbName", "pgSqlDbHost", "pgSqlDbPort", "pgSqlDbUser", "pgSqlDbPassword", "pgSqlTestTable",
                  "mariadbDbName", "mariadbDbHost", "mariadbDbPort", "mariadbyDbUser", "mariadbDbPassword", "mariadbTestTable"})
    public void beforeSuite(
            @Optional(DEFAULT_MYSQL_DB_NAME)   String mySqlDbName,     @Optional(DEFAULT_MYSQL_HOST)  String mySqlDbHost, 
            @Optional(DEFAULT_MYSQL_PORT)      String mySqlDbPort,     @Optional(DEFAULT_MYSQL_USER)  String mySqlDbUser,
            @Optional(DEFAULT_MYSQL_PASSWORD)  String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mySqlTestTable,
            
            @Optional(DEFAULT_PGSQL_DB_NAME)   String pgSqlDbName,     @Optional(DEFAULT_PGSQL_HOST)  String pgSqlDbHost, 
            @Optional(DEFAULT_PGSQL_PORT)      String pgSqlDbPort,     @Optional(DEFAULT_PGSQL_USER)  String pgSqlDbUser,
            @Optional(DEFAULT_PGSQL_PASSWORD)  String pgSqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String pgSqlTestTable,
            
            @Optional(DEFAULT_MARIADB_NAME)      String mariadbDbName,     @Optional(DEFAULT_MARIADB_HOST)  String mariadbDbHost, 
            @Optional(DEFAULT_MARIADB_PORT)      String mariadbDbPort,     @Optional(DEFAULT_MARIADB_USER)  String mariadbyDbUser,
            @Optional(DEFAULT_MARIADB_PASSWORD)  String mariadbDbPassword, @Optional(DEFAULT_TEST_TABLE)    String mariadbTestTable)
                    throws DatabaseServiceException, SQLException {
        
        //System.out.println("@BeforeSuite\n");
         mysqlDbConfig = new DatabaseConfiguration();
        mysqlDbConfig.setDatabaseHost(mySqlDbHost);
        mysqlDbConfig.setDatabaseName(mySqlDbName);
        mysqlDbConfig.setDatabasePassword(mySqlDbPassword);
        mysqlDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        mysqlDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        mysqlDbConfig.setDatabaseUser(mySqlDbUser);
        mysqlDbConfig.setUseSSL(false);
        
         pgsqlDbConfig = new DatabaseConfiguration();
        pgsqlDbConfig.setDatabaseHost(pgSqlDbHost);
        pgsqlDbConfig.setDatabaseName(pgSqlDbName);
        pgsqlDbConfig.setDatabasePassword(pgSqlDbPassword);
        pgsqlDbConfig.setDatabasePort(Integer.parseInt(pgSqlDbPort));
        pgsqlDbConfig.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        pgsqlDbConfig.setDatabaseUser(pgSqlDbUser);
        pgsqlDbConfig.setUseSSL(false);
        
         mariadbDbConfig = new DatabaseConfiguration();
        mariadbDbConfig.setDatabaseHost(mariadbDbHost);
        mariadbDbConfig.setDatabaseName(mariadbDbName);
        mariadbDbConfig.setDatabasePassword(mariadbDbPassword);
        mariadbDbConfig.setDatabasePort(Integer.parseInt(mariadbDbPort));
        mariadbDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        mariadbDbConfig.setDatabaseUser(mariadbyDbUser);
        mariadbDbConfig.setUseSSL(false);
    
        DBExtensionTestUtils.initTestData(mysqlDbConfig);
        DBExtensionTestUtils.initTestData(pgsqlDbConfig);
        DBExtensionTestUtils.initTestData(mariadbDbConfig);
    }
  
    @AfterSuite
    public void afterSuite() {
       // System.out.println("@AfterSuite");
       
        DBExtensionTestUtils.cleanUpTestData(mysqlDbConfig);
        DBExtensionTestUtils.cleanUpTestData(pgsqlDbConfig);
        DBExtensionTestUtils.cleanUpTestData(mariadbDbConfig);
    }

}

