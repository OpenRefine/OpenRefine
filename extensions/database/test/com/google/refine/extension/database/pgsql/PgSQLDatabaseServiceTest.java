package com.google.refine.extension.database.pgsql;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;

public class PgSQLDatabaseServiceTest {

    
    @BeforeTest
    public void beforeTest() {
        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
    }

    @Test
    @Parameters("dbName")
    public void testGetDatabaseUrl(@Optional("postgres") String dbName) {
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost("127.0.0.1");
        dc.setDatabaseName(dbName);
        dc.setDatabasePort(5432);
        dc.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("postgres");
        dc.setUseSSL(false);
        
        String dbURL = PgSQLDatabaseService.getInstance().getDatabaseUrl(dc);
        Assert.assertEquals(dbURL, "jdbc:postgresql://127.0.0.1:5432/" + dbName + "?useSSL=false");
       
    }

    @Test
    @Parameters("dbName")
    public void testGetConnection(@Optional("postgres") String dbName) {
        System.out.println("dbName::" + dbName);
    }

    @Test
    @Parameters("dbName")
    public void testTestConnection(@Optional("postgres") String dbName) {
  
    }

    @Test
    @Parameters("dbName")
    public void testConnect(@Optional("postgres") String dbName) {
       
    }

    @Test
    @Parameters("dbName")
    public void testExecuteQuery(@Optional("postgres") String dbName) {
      
    }

    @Test
    public void testBuildLimitQuery() {
        String query = PgSQLDatabaseService.getInstance().buildLimitQuery(100, 10, "SELECT * FROM TEST");
        Assert.assertEquals(query, "SELECT * FROM TEST LIMIT 100 OFFSET 10");

    }

    @Test
    @Parameters("dbName")
    public void testGetRows(@Optional("postgres") String dbName) {
     
    }

    @Test
    @Parameters("dbName")
    public void testGetInstance(@Optional("postgres") String dbName) {
       
    }

    @Test
    @Parameters("dbName")
    public void testGetColumns(@Optional("postgres") String dbName) {
        
    }

}
