package com.google.refine.extension.database.pgsql;

import java.sql.Connection;
import java.util.List;

import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTestUtils;
import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;


public class PgSQLDatabaseServiceTest extends DBExtensionTests{
  
    private DatabaseConfiguration testDbConfig;
    private String testTable;
  
 
    @BeforeTest
    @Parameters({ "pgSqlDbName", "pgSqlDbHost", "pgSqlDbPort", "pgSqlDbUser", "pgSqlDbPassword", "pgSqlTestTable"})
    public void beforeTest(@Optional(DEFAULT_PGSQL_DB_NAME) String pgSqlDbName,  @Optional(DEFAULT_PGSQL_HOST) String pgSqlDbHost, 
           @Optional(DEFAULT_PGSQL_PORT)        String pgSqlDbPort,     @Optional(DEFAULT_PGSQL_USER)   String pgSqlDbUser,
           @Optional(DEFAULT_PGSQL_PASSWORD)    String pgSqlDbPassword, @Optional(DEFAULT_TEST_TABLE)   String pgSqlTestTable) {
       
        MockitoAnnotations.initMocks(this);
       // System.out.println("beforeTest " + pgSqlDbName);
        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(pgSqlDbHost);
        testDbConfig.setDatabaseName(pgSqlDbName);
        testDbConfig.setDatabasePassword(pgSqlDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(pgSqlDbPort));
        testDbConfig.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(pgSqlDbUser);
        testDbConfig.setUseSSL(false);
        
        testTable = pgSqlTestTable;
        //DBExtensionTestUtils.initTestData(testDbConfig);
        
        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
    }
   


    @Test
    public void testGetDatabaseUrl() {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService)DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);
        System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }
 

    @Test
    public void testGetConnection() {
        try {
           
            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService)DatabaseService.get(PgSQLDatabaseService.DB_NAME);
            Connection conn = pgSqlService.getConnection(testDbConfig);
       
            Assert.assertNotNull(conn);
             
         } catch (DatabaseServiceException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
    }

    @Test
    public void testTestConnection() {
        try {
            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService)DatabaseService.get(PgSQLDatabaseService.DB_NAME);
                   
            boolean result = pgSqlService.testConnection(testDbConfig);
            Assert.assertEquals(result, true);

        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testConnect() {
        try {
            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService)DatabaseService.get(PgSQLDatabaseService.DB_NAME);
           DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
           Assert.assertNotNull(databaseInfo);
            
        } catch (DatabaseServiceException e) {
           e.printStackTrace();
        }
    }
    
    @Test
    public void testExecuteQuery() {

        try {

            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                    .get(PgSQLDatabaseService.DB_NAME);
            DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

            Assert.assertNotNull(databaseInfo);

        } catch (DatabaseServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBuildLimitQuery() {
        PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);

        Assert.assertEquals(limitQuery, "SELECT * FROM " + testTable + " LIMIT " + 100 + " OFFSET " + 0 + ";");

    }

    @Test
    public void testGetRows() {
        try {
            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                    .get(PgSQLDatabaseService.DB_NAME);
            List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

            Assert.assertNotNull(dbRows);

        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGetInstance() {
        PgSQLDatabaseService instance = PgSQLDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() {
        List<DatabaseColumn> dbColumns;

        try {
            PgSQLDatabaseService pgSqlService = (PgSQLDatabaseService) DatabaseService
                    .get(PgSQLDatabaseService.DB_NAME);

            dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

            Assert.assertNotNull(dbColumns);

        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
