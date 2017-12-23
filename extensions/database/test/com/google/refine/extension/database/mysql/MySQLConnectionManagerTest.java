package com.google.refine.extension.database.mysql;

import java.sql.Connection;
import java.sql.SQLException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;

public class MySQLConnectionManagerTest {
    
    
    private static final int DATABASE_PORT = 3306;

    @BeforeTest
    public void beforeTest() {
        DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
    }

  
    @Test
    public void testTestConnection() {
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost("127.0.0.1");
        dc.setDatabaseName("mysql");
        dc.setDatabasePassword("secret");
        dc.setDatabasePort(DATABASE_PORT);
        dc.setDatabaseType(MySQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("root");
        dc.setUseSSL(false);
        try {
            boolean conn = MySQLConnectionManager.getInstance().testConnection(dc);
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
        dc.setDatabaseName("mysql");
        dc.setDatabasePassword("secret");
        dc.setDatabasePort(DATABASE_PORT);
        dc.setDatabaseType(MySQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("root");
        dc.setUseSSL(false);
        try {
             Connection conn = MySQLConnectionManager.getInstance().getConnection(dc, true);
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
        dc.setDatabaseName("mysql");
        dc.setDatabasePassword("secret");
        dc.setDatabasePort(DATABASE_PORT);
        dc.setDatabaseType(MySQLDatabaseService.DB_NAME);
        dc.setDatabaseUser("root");
        dc.setUseSSL(false);
        try {
             Connection conn = MySQLConnectionManager.getInstance().getConnection(dc, true);
             Assert.assertNotNull(conn);
             
             MySQLConnectionManager.getInstance().shutdown();
             
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
