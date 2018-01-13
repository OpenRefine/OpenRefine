package com.google.refine.extension.database.mariadb;

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


public class MariaDBDatabaseServiceTest extends DBExtensionTests{
    
    
    
   private DatabaseConfiguration testDbConfig;
   
   private String testTable;
   
    
    @BeforeTest
    @Parameters({ "mariaDbName", "mariaDbHost", "mariaDbPort", "mariaDbUser", "mariaDbPassword", "mariaDbTestTable"})
    public void beforeTest(@Optional(DEFAULT_MARIADB_NAME) String mariaDbName,  @Optional(DEFAULT_MARIADB_HOST) String mariaDbHost, 
           @Optional(DEFAULT_MARIADB_PORT)    String mariaDbPort,     @Optional(DEFAULT_MARIADB_USER) String mariaDbUser,
           @Optional(DEFAULT_MARIADB_PASSWORD)  String mariaDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mariaDbTestTable) {
       
        MockitoAnnotations.initMocks(this);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mariaDbHost);
        testDbConfig.setDatabaseName(mariaDbName);
        testDbConfig.setDatabasePassword(mariaDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mariaDbPort));
        testDbConfig.setDatabaseType(MariaDBDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mariaDbUser);
        testDbConfig.setUseSSL(false);
        
        testTable = mariaDbTestTable;
        //DBExtensionTestUtils.initTestData(testDbConfig);
        
        DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());
        
    }
   
    @Test
    public void testGetDatabaseUrl() {
        MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService)DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);
        //System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }

     @Test
     public void testGetConnection() {
         try {
            
             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService)DatabaseService.get(MariaDBDatabaseService.DB_NAME);
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
             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService)DatabaseService.get(MariaDBDatabaseService.DB_NAME);
                    
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
             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService)DatabaseService.get(MariaDBDatabaseService.DB_NAME);
            DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
            Assert.assertNotNull(databaseInfo);
             
         } catch (DatabaseServiceException e) {
            e.printStackTrace();
         }
     }
     
     @Test
     public void testExecuteQuery() {

         try {

             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                     .get(MariaDBDatabaseService.DB_NAME);
             DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(databaseInfo);

         } catch (DatabaseServiceException e) {
             e.printStackTrace();
         }
     }

     @Test
     public void testBuildLimitQuery() {
         MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService.get(MariaDBDatabaseService.DB_NAME);
         String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
         Assert.assertNotNull(limitQuery);

         Assert.assertEquals(limitQuery, "SELECT * FROM " + testTable + " LIMIT " + 100 + " OFFSET " + 0 + ";");

     }

     @Test
     public void testGetRows() {
         try {
             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                     .get(MariaDBDatabaseService.DB_NAME);
             List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(dbRows);

         } catch (DatabaseServiceException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }

     @Test
     public void testGetInstance() {
         MariaDBDatabaseService instance = MariaDBDatabaseService.getInstance();
         Assert.assertNotNull(instance);
     }

     @Test
     public void testGetColumns() {
         List<DatabaseColumn> dbColumns;

         try {
             MariaDBDatabaseService pgSqlService = (MariaDBDatabaseService) DatabaseService
                     .get(MariaDBDatabaseService.DB_NAME);

             dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(dbColumns);

         } catch (DatabaseServiceException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }
     

}
