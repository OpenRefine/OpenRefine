package com.google.refine.extension.database.pgsql;

import java.sql.Connection;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;



public class PgSQLConnectionManagerTest {
    
    
    @BeforeTest
    public void beforeTest() {
        DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
    }

  
    @Test
    public void testTestConnection() {
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost("127.0.0.1");
        dc.setDatabaseName("enmsso"); 
        dc.setDatabasePort(5432);
        dc.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("postgres");
        dc.setUseSSL(false);
        try {
            boolean conn = PgSQLConnectionManager.getInstance().testConnection(dc);
            Assert.assertEquals(conn, true);
            
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGetConnection() {
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost("127.0.0.1");
        dc.setDatabaseName("enmsso");
        //dc.setDatabasePassword("secret");
        dc.setDatabasePort(5432);
        dc.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("postgres");
        dc.setUseSSL(false);
        try {
             Connection conn = PgSQLConnectionManager.getInstance().getConnection(dc, true);
             Assert.assertNotNull(conn);
            
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testShutdown() {
        
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost("127.0.0.1");
        dc.setDatabaseName("enmsso");
        //dc.setDatabasePassword("secret");
        dc.setDatabasePort(5432);
        dc.setDatabaseType(PgSQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("postgres");
        dc.setUseSSL(false);
        try {
             Connection conn = PgSQLConnectionManager.getInstance().getConnection(dc, true);
             Assert.assertNotNull(conn);
             
             PgSQLConnectionManager.getInstance().shutdown();
             
             if(conn != null) {
                 Assert.assertEquals(conn.isClosed(), true);
             }
             
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     
    }

}
