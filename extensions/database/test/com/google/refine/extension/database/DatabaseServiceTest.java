package com.google.refine.extension.database;


import java.sql.Connection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;




public class DatabaseServiceTest extends DBExtensionTests{
    
    private DatabaseConfiguration testDbConfig;
    private String testTable;
    
  
    @BeforeTest
    @Parameters({ "mySqlDbName", "mySqlDbHost", "mySqlDbPort", "mySqlDbUser", "mySqlDbPassword", "mySqlTestTable"})
    public void beforeTest(@Optional(DEFAULT_MYSQL_DB_NAME) String mySqlDbName,  @Optional(DEFAULT_MYSQL_HOST) String mySqlDbHost, 
           @Optional(DEFAULT_MYSQL_PORT)    String mySqlDbPort,     @Optional(DEFAULT_MYSQL_USER) String mySqlDbUser,
           @Optional(DEFAULT_MYSQL_PASSWORD)  String mySqlDbPassword, @Optional(DEFAULT_TEST_TABLE)  String mySqlTestTable) {
       
 
        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseHost(mySqlDbHost);
        testDbConfig.setDatabaseName(mySqlDbName);
        testDbConfig.setDatabasePassword(mySqlDbPassword);
        testDbConfig.setDatabasePort(Integer.parseInt(mySqlDbPort));
        testDbConfig.setDatabaseType(MySQLDatabaseService.DB_NAME);
        testDbConfig.setDatabaseUser(mySqlDbUser);
        testDbConfig.setUseSSL(false);
        
        testTable = mySqlTestTable;
        DBExtensionTestUtils.initTestData(testDbConfig);
        
        DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());
        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
        
    }

    @AfterSuite
    public void afterSuite() {
        DBExtensionTestUtils.cleanUpTestData(testDbConfig);
      
    }

    @Test
    public void testGetDatabaseUrl() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String dbUrl = dbService.getDatabaseUrl(testDbConfig);
        System.out.println("dbUrl:" + dbUrl);
        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, DBExtensionTestUtils.getJDBCUrl(testDbConfig));
    }
  
    

    @Test
    public void testGetPgSQLDBService() {
       
        DatabaseService dbService = DatabaseService.get(PgSQLDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);  
        Assert.assertEquals(dbService.getClass(), PgSQLDatabaseService.class);
    }
    
    @Test
    public void testGetMySQLDBService() {
       
        DatabaseService dbService = DatabaseService.get(MySQLDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);  
        Assert.assertEquals(dbService.getClass(), MySQLDatabaseService.class);
    }
    
    @Test
    public void testGetMariaDBSQLDBService() {
       
        DatabaseService dbService = DatabaseService.get(MariaDBDatabaseService.DB_NAME);
        Assert.assertNotNull(dbService);  
        Assert.assertEquals(dbService.getClass(), MariaDBDatabaseService.class);
    }

    @Test
    public void testGetConnection() {
  
        try {
           
           DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
           Connection conn = dbService.getConnection(testDbConfig);
           //System.out.println("conn:" + conn);
           Assert.assertNotNull(conn);
            
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testTestConnection() {

        try {
            DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
            boolean result = dbService.testConnection(testDbConfig);
            Assert.assertEquals(result, true);

        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Test
    public void testConnect() {

        try {
            DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
            DatabaseInfo databaseInfo = dbService.connect(testDbConfig);
            Assert.assertNotNull(databaseInfo);

        } catch (DatabaseServiceException e) {
            e.printStackTrace();
        }
        
    }

    @Test
    public void testExecuteQuery() {

        try {
            DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
            DatabaseInfo databaseInfo = dbService.testQuery(testDbConfig,
                    "SELECT * FROM " + testTable);

            Assert.assertNotNull(databaseInfo);

        } catch (DatabaseServiceException e) {
            e.printStackTrace();
        }
        
    }

    @Test
    public void testBuildLimitQuery() {
        DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
        String limitQuery = dbService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);

        Assert.assertNotNull(limitQuery);

        Assert.assertEquals(limitQuery, "SELECT * FROM " + testTable + " LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetColumns() {
        List<DatabaseColumn> dbColumns;
      
        try {
            DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
            dbColumns = dbService.getColumns(testDbConfig,"SELECT * FROM " + testTable);
            Assert.assertNotNull(dbColumns);
            
            int cols = dbColumns.size();
            Assert.assertEquals(cols, 3);
       
            
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
       
    }

    @Test
    public void testGetRows() {
        
        try {
       
            DatabaseService dbService = DatabaseService.get(testDbConfig.getDatabaseType());
            List<DatabaseRow> dbRows = dbService.getRows(testDbConfig,
                    "SELECT * FROM " + testTable);
            
            Assert.assertNotNull(dbRows);
            Assert.assertEquals(dbRows.size(), 1);
            
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
    }

}
