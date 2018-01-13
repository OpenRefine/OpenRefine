package com.google.refine.extension.database.mysql;

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



public class MySQLDatabaseServiceTest extends DBExtensionTests{
    
    private DatabaseConfiguration testDbConfig;
    private String testTable;
    
    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable"})
    public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName,  @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost, 
           @Optional(DEFAULT_MYSQL_PORT)    String mySqlDbPort,     @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
           @Optional(DEFAULT_MYSQL_PASSWORD)  String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mySqlTestTable) {
       
        MockitoAnnotations.initMocks(this);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mySqlDbHost);
        testDbConfig.setDatabaseName(mySqlDbName);
        testDbConfig.setDatabasePassword(mySqlDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        testDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mySqlDbUser);
        testDbConfig.setUseSSL(false);
        
        testTable = mySqlTestTable;
        //DBExtensionTestUtils.initTestData(testDbConfig);
        
        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
        
    }
   

    @Test
    public void testGetDatabaseUrl() {
        MySQLDatabaseService pgSqlService = (MySQLDatabaseService)DatabaseService.get(MySQLDatabaseService.DB_NAME);
        String dbUrl = pgSqlService.getDatabaseUrl(testDbConfig);
        //System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }
 

     @Test
     public void testGetConnection() {
         try {
            
             MySQLDatabaseService pgSqlService = (MySQLDatabaseService)DatabaseService.get(MySQLDatabaseService.DB_NAME);
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
             MySQLDatabaseService pgSqlService = (MySQLDatabaseService)DatabaseService.get(MySQLDatabaseService.DB_NAME);
                    
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
             MySQLDatabaseService pgSqlService = (MySQLDatabaseService)DatabaseService.get(MySQLDatabaseService.DB_NAME);
            DatabaseInfo databaseInfo = pgSqlService.connect(testDbConfig);
            Assert.assertNotNull(databaseInfo);
             
         } catch (DatabaseServiceException e) {
            e.printStackTrace();
         }
     }
     
     @Test
     public void testExecuteQuery() {

         try {

             MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                     .get(MySQLDatabaseService.DB_NAME);
             DatabaseInfo databaseInfo = pgSqlService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(databaseInfo);

         } catch (DatabaseServiceException e) {
             e.printStackTrace();
         }
     }

     @Test
     public void testBuildLimitQuery() {
         MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService.get(MySQLDatabaseService.DB_NAME);
         String limitQuery = pgSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
         Assert.assertNotNull(limitQuery);

         Assert.assertEquals(limitQuery, "SELECT * FROM " + testTable + " LIMIT " + 100 + " OFFSET " + 0 + ";");

     }

     @Test
     public void testGetRows() {
         try {
             MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                     .get(MySQLDatabaseService.DB_NAME);
             List<DatabaseRow> dbRows = pgSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(dbRows);

         } catch (DatabaseServiceException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }

     @Test
     public void testGetInstance() {
         MySQLDatabaseService instance = MySQLDatabaseService.getInstance();
         Assert.assertNotNull(instance);
     }

     @Test
     public void testGetColumns() {
         List<DatabaseColumn> dbColumns;

         try {
             MySQLDatabaseService pgSqlService = (MySQLDatabaseService) DatabaseService
                     .get(MySQLDatabaseService.DB_NAME);

             dbColumns = pgSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

             Assert.assertNotNull(dbColumns);

         } catch (DatabaseServiceException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
     }
     


}
