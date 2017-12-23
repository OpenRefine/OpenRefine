package com.google.refine.extension.database;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DBExtensionTestUtils {
    
    private static final String DB_TYPE_MYSQL = "mysql";
    private static final String DB_HOST_MYSQL = "127.0.0.1";
    private static final int DB_PORT_MYSQL = 3306;
    private static final String DB_USER_MYSQL = "root";
    private static final String DB_PASSWORD_MYSQL = "secret";
    private static final String DB_NAME_MYSQL = "rxhub";
    
    private static final String DB_TYPE_PGSQL = "postgresql";
    private static final String DB_HOST_PGSQL = "127.0.0.1";
    private static final int DB_PORT_PGSQL = 5432;
    private static final String DB_USER_PGSQL = "postgres";
    private static final String DB_PASSWORD_PGSQL = "";
    private static final String DB_NAME_PGSQL = "openrefine";
    
    
    private static final int SAMPLE_SIZE = 500000;
    private static final int BATCH_SIZE = 1000;
    
    private static Random rand = new Random();
    
    private  Map<Integer, Integer> mncMap;
    private  Map<Integer, Integer> mccMap;
    
    
    public void generateMySQLTestData() {
        mncMap =  new HashMap<Integer, Integer>();
        mccMap =  new HashMap<Integer, Integer>();
        mccMap.put(0, 302);
        mccMap.put(1, 311);
        mccMap.put(2, 730);
        mccMap.put(1, 622);
        
        mncMap.put(0, 006);
        mncMap.put(1, 140);
        mncMap.put(2, 380);
        mncMap.put(3, 710);
        
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost(DB_HOST_MYSQL);
        dc.setDatabaseName(DB_NAME_MYSQL);
        dc.setDatabasePassword(DB_PASSWORD_MYSQL);
        dc.setDatabasePort(DB_PORT_MYSQL);
        dc.setDatabaseType(DB_TYPE_MYSQL);
        dc.setDatabaseUser(DB_USER_MYSQL);
        dc.setUseSSL(false);
        
        String truncateTableSQL = "TRUNCATE test_data";
       
        String insertTableSQL = "INSERT INTO test_data("
                + "id, ue_id, start_time, end_date, bytes_upload, bytes_download, cell_id, mcc, mnc, lac, imei)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        
        Connection conn;
        try {
            conn = DatabaseService.get(DB_TYPE_MYSQL).getConnection(dc);
            
            Statement truncateStmt = conn.createStatement();
            int result = truncateStmt.executeUpdate(truncateTableSQL);
            System.out.println("Truncate Table Result::" + result);
            truncateStmt.close();
            
            
            conn.setAutoCommit(false);
            
            PreparedStatement stmt = conn.prepareStatement(insertTableSQL);
           
            int counter=1;
            for (int i = 0; i < SAMPLE_SIZE; i++) {
               stmt.setLong(1, i);
               stmt.setString(2,  getNextUeId(i));
               stmt.setDate(3, getNextStartDate(i));
               stmt.setDate(4, getNextEndDate(i));
               stmt.setInt(5, rand.nextInt());
               stmt.setInt(6, rand.nextInt());
               stmt.setInt(7, rand.nextInt(10));
               stmt.setInt(8, getMCC(i));
               stmt.setInt(9, getMNC(i));
               stmt.setInt(10, rand.nextInt(100));
               stmt.setString(11, getNextIMEI(i));
             
               stmt.addBatch();
               
               //Execute batch of 1000 records
               if(i%BATCH_SIZE==0){
                  stmt.executeBatch();
                  conn.commit();
                  System.out.println("Batch "+(counter++)+" executed successfully");
               }
            }
            //execute final batch
            stmt.executeBatch();
            System.out.println("Final Batch Executed "+(counter++)+" executed successfully");
            conn.commit();
            conn.close();
         
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
 
    }
   
    
    public void generatePgSQLTestData() {
        mncMap =  new HashMap<Integer, Integer>();
        mccMap =  new HashMap<Integer, Integer>();
        mccMap.put(0, 302);
        mccMap.put(1, 311);
        mccMap.put(2, 730);
        mccMap.put(1, 622);
        
        mncMap.put(0, 006);
        mncMap.put(1, 140);
        mncMap.put(2, 380);
        mncMap.put(3, 710);
        
        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost(DB_HOST_PGSQL);
        dc.setDatabaseName(DB_NAME_PGSQL);
        dc.setDatabasePassword(DB_PASSWORD_PGSQL);
        dc.setDatabasePort(DB_PORT_PGSQL);
        dc.setDatabaseType(DB_TYPE_PGSQL);
        dc.setDatabaseUser(DB_USER_PGSQL);
        dc.setUseSSL(false);
        
        String truncateTableSQL = "TRUNCATE public.test_data";
       
        String insertTableSQL = "INSERT INTO public.test_data("
                + "id, ue_id, start_time, end_date, bytes_upload, bytes_download, cell_id, mcc, mnc, lac, imei)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        
        Connection conn;
        try {
            conn = DatabaseService.get(DB_TYPE_PGSQL).getConnection(dc);
            
            Statement truncateStmt = conn.createStatement();
            int result = truncateStmt.executeUpdate(truncateTableSQL);
            System.out.println("Truncate Table Result::" + result);
            truncateStmt.close();
            
            
            conn.setAutoCommit(false);
            
            PreparedStatement stmt = conn.prepareStatement(insertTableSQL);
           
            int counter=1;
            for (int i = 0; i < SAMPLE_SIZE; i++) {
               stmt.setLong(1, i);
               stmt.setString(2,  getNextUeId(i));
               stmt.setDate(3, getNextStartDate(i));
               stmt.setDate(4, getNextEndDate(i));
               stmt.setInt(5, rand.nextInt());
               stmt.setInt(6, rand.nextInt());
               stmt.setInt(7, rand.nextInt(10));
               stmt.setInt(8, getMCC(i));
               stmt.setInt(9, getMNC(i));
               stmt.setInt(10, rand.nextInt(100));
               stmt.setString(11, getNextIMEI(i));
             
               stmt.addBatch();
               
               //Execute batch of 1000 records
               if(i%BATCH_SIZE==0){
                  stmt.executeBatch();
                  conn.commit();
                  System.out.println("Batch "+(counter++)+" executed successfully");
               }
            }
            //execute final batch
            stmt.executeBatch();
            System.out.println("Final Batch Executed "+(counter++)+" executed successfully");
            conn.commit();
            conn.close();
         
        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
 
    }

    private String getNextIMEI(int i) {
        
//        byte[] array = new byte[16]; // length is bounded by 7
//        new Random().nextBytes(array);
//        String generatedString = new String(array, Charset.forName("UTF-8"));
        
        int n = 1000000000 + rand.nextInt(900000000);
        return "" + n;
    }

  

    private  int getMNC(int i) {
        // TODO Auto-generated method stub
        return mncMap.get(rand.nextInt(3));
    }

    private  int getMCC(int i) {
       // System.out.println(rand.nextInt(3));
        return mccMap.get(rand.nextInt(3));
    }

    private Date getNextEndDate(int i) {
        // TODO Auto-generated method stub
        return  new Date(System.currentTimeMillis() + 1);
    }

    private Date getNextStartDate(int i) {
        // TODO Auto-generated method stub
        return new Date(System.currentTimeMillis());
    }

    private String getNextUeId(int i) {
        int n = 300000000 + rand.nextInt(900000000);
        
        return "" + n;
    }
    
    public static void main(String[] args) {
        DBExtensionTestUtils testUtil = new DBExtensionTestUtils();
       // testUtil.generatePgSQLTestData();
        testUtil.generateMySQLTestData();
    }

}
