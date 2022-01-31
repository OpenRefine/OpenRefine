
package com.google.refine.extension.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBExtensionTestUtils {

    private static final Logger logger = LoggerFactory.getLogger("DBExtensionTestUtils");

    private static final String MYSQL_DB_NAME = "mysql";
    private static final String DEFAULT_MYSQL_HOST = "127.0.0.1";
    private static final int DEFAULT_MYSQL_PORT = 3306;
    private static final String DEFAULT_MYSQL_USER = "root";
    private static final String DEFAULT_MYSQL_PASSWORD = "secret";
    private static final String DEFAULT_MYSQL_DB_NAME = "testdb";

    private static final String PGSQL_DB_NAME = "postgresql";
    private static final String DEFAULT_PGSQL_HOST = "127.0.0.1";
    private static final int DEFAULT_PGSQL_PORT = 5432;
    private static final String DEFAULT_PGSQL_USER = "postgres";
    private static final String DEFAULT_PGSQL_PASSWORD = "";
    private static final String DEFAULT_PGSQL_DB_NAME = "openrefine";

    private static final String DEFAULT_TEST_TABLE = "test_data";

    private static final int SAMPLE_SIZE = 500000;
    private static final int BATCH_SIZE = 1000;

    private static Random rand = new Random();

    private Map<Integer, Integer> mncMap;
    private Map<Integer, Integer> mccMap;

    /**
     * Create Test Table with one row of Data
     * 
     * @param dbConfig
     *            DatabaseConfiguration to test
     * @param tableName
     * @throws DatabaseServiceException
     * @throws SQLException
     */
    public static void initTestData(DatabaseConfiguration dbConfig, String tableName)
            throws DatabaseServiceException, SQLException {

        Statement stmt = null;
        Connection conn = null;
        try {
            DatabaseService dbService = DatabaseService.get(dbConfig.getDatabaseType());
            conn = dbService.getConnection(dbConfig);
            stmt = conn.createStatement();

            DatabaseMetaData dbm = conn.getMetaData();
            // check if "employee" table is there
            ResultSet tables = dbm.getTables(null, null, tableName, null);
            boolean dropTable = false;
            if (tables.next()) {
                dropTable = true;
                // System.out.println("Drop Table Result::" + dropResult);
            }
            tables.close();
            if (dropTable) {
                stmt.executeUpdate("DROP TABLE " + tableName);
            }

            String createSQL = " CREATE TABLE " + tableName + " ( "
                    + " ID   INT  NOT NULL, "
                    + " NAME VARCHAR (20) NOT NULL, "
                    + " CITY  VARCHAR (20) NOT NULL,"
                    + " PRIMARY KEY (ID)  );";

            stmt.executeUpdate(createSQL);
            // System.out.println("Create Table Result::" + createResult);

            String insertTableSQL = "INSERT INTO " + tableName
                    + "(ID, NAME, CITY) " + "VALUES"
                    + "(1,'frank lens','Dallas')";

            stmt.executeUpdate(insertTableSQL);
            // System.out.println("Insert Data Result::" + insertResult);

            logger.info("Database Test Init Data Created!!!");

        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void initTestData(DatabaseConfiguration dbConfig)
            throws DatabaseServiceException, SQLException {

        Statement stmt = null;
        Connection conn = null;
        try {
            DatabaseService dbService = DatabaseService.get(dbConfig.getDatabaseType());
            conn = dbService.getConnection(dbConfig);
            stmt = conn.createStatement();

            DatabaseMetaData dbm = conn.getMetaData();
            // check if "employee" table is there
            ResultSet tables = dbm.getTables(null, null, DEFAULT_TEST_TABLE, null);
            boolean dropTable = false;
            if (tables.next()) {
                dropTable = true;
                // System.out.println("Drop Table Result::" + dropResult);
            }
            tables.close();
            if (dropTable) {
                stmt.executeUpdate("DROP TABLE " + DEFAULT_TEST_TABLE);
                // System.out.println("Drop Table Result::" + dropResult);
            }

            String createSQL = " CREATE TABLE TEST_DATA( "
                    + " ID   INT  NOT NULL, "
                    + " NAME VARCHAR (20) NOT NULL, "
                    + " CITY  VARCHAR (20) NOT NULL,"
                    + " PRIMARY KEY (ID)  );";

            stmt.executeUpdate(createSQL);
            // System.out.println("Create Table Result::" + createResult);

            String insertTableSQL = "INSERT INTO TEST_DATA"
                    + "(ID, NAME, CITY) " + "VALUES"
                    + "(1,'frank lens','Dallas')";

            stmt.executeUpdate(insertTableSQL);
            // System.out.println("Insert Data Result::" + insertResult);

            logger.info("Database Test Init Data Created!!!");

        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * CREATE test data in MySQL Table name: test_data
     * 
     * @param sampleSize
     * @param batchSize
     * @throws DatabaseServiceException
     * @throws SQLException
     */
    public void generateMySQLTestData(int sampleSize, int batchSize)
            throws DatabaseServiceException, SQLException {
        mncMap = new HashMap<Integer, Integer>();
        mccMap = new HashMap<Integer, Integer>();
        mccMap.put(0, 302);
        mccMap.put(1, 311);
        mccMap.put(2, 730);
        mccMap.put(1, 622);

        mncMap.put(0, 006);
        mncMap.put(1, 140);
        mncMap.put(2, 380);
        mncMap.put(3, 710);

        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost(DEFAULT_MYSQL_HOST);
        dc.setDatabaseName(DEFAULT_MYSQL_DB_NAME);
        dc.setDatabasePassword(DEFAULT_MYSQL_PASSWORD);
        dc.setDatabasePort(DEFAULT_MYSQL_PORT);
        dc.setDatabaseType(MYSQL_DB_NAME);
        dc.setDatabaseUser(DEFAULT_MYSQL_USER);
        dc.setUseSSL(false);

        String truncateTableSQL = "TRUNCATE test_data";

        String insertTableSQL = "INSERT INTO test_data("
                + "id, ue_id, start_time, end_date, bytes_upload, bytes_download, cell_id, mcc, mnc, lac, imei)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Connection conn = DatabaseService.get(MYSQL_DB_NAME).getConnection(dc);

        Statement truncateStmt = conn.createStatement();
        int result = truncateStmt.executeUpdate(truncateTableSQL);
        System.out.println("Truncate Table Result::" + result);
        truncateStmt.close();

        conn.setAutoCommit(false);

        PreparedStatement stmt = conn.prepareStatement(insertTableSQL);

        int counter = 1;
        for (int i = 0; i < sampleSize; i++) {
            stmt.setLong(1, i);
            stmt.setString(2, getNextUeId());
            stmt.setDate(3, getNextStartDate());
            stmt.setDate(4, getNextEndDate());
            stmt.setInt(5, rand.nextInt());
            stmt.setInt(6, rand.nextInt());
            stmt.setInt(7, rand.nextInt(10));
            stmt.setInt(8, getMCC());
            stmt.setInt(9, getMNC());
            stmt.setInt(10, rand.nextInt(100));
            stmt.setString(11, getNextIMEI());

            stmt.addBatch();

            // Execute batch of 1000 records
            if (i % batchSize == 0) {
                stmt.executeBatch();
                conn.commit();
                System.out.println("Batch " + (counter++) + " executed successfully");
            }
        }
        // execute final batch
        stmt.executeBatch();
        System.out.println("Final Batch Executed " + (counter++) + " executed successfully");
        conn.commit();
        conn.close();
    }

    /**
     * 
     * @param sampleSize
     * @param batchSize
     * @throws DatabaseServiceException
     * @throws SQLException
     */
    public void generatePgSQLTestData(int sampleSize, int batchSize) throws DatabaseServiceException, SQLException {
        mncMap = new HashMap<Integer, Integer>();
        mccMap = new HashMap<Integer, Integer>();
        mccMap.put(0, 302);
        mccMap.put(1, 311);
        mccMap.put(2, 730);
        mccMap.put(1, 622);

        mncMap.put(0, 006);
        mncMap.put(1, 140);
        mncMap.put(2, 380);
        mncMap.put(3, 710);

        DatabaseConfiguration dc = new DatabaseConfiguration();
        dc.setDatabaseHost(DEFAULT_PGSQL_HOST);
        dc.setDatabaseName(DEFAULT_PGSQL_DB_NAME);
        dc.setDatabasePassword(DEFAULT_PGSQL_PASSWORD);
        dc.setDatabasePort(DEFAULT_PGSQL_PORT);
        dc.setDatabaseType(PGSQL_DB_NAME);
        dc.setDatabaseUser(DEFAULT_PGSQL_USER);
        dc.setUseSSL(false);

        String truncateTableSQL = "TRUNCATE public.test_data";

        String insertTableSQL = "INSERT INTO public.test_data("
                + "id, ue_id, start_time, end_date, bytes_upload, bytes_download, cell_id, mcc, mnc, lac, imei)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        Connection conn = DatabaseService.get(PGSQL_DB_NAME).getConnection(dc);

        Statement truncateStmt = conn.createStatement();
        int result = truncateStmt.executeUpdate(truncateTableSQL);
        System.out.println("Truncate Table Result::" + result);
        truncateStmt.close();

        conn.setAutoCommit(false);

        PreparedStatement stmt = conn.prepareStatement(insertTableSQL);

        int counter = 1;
        for (int i = 0; i < sampleSize; i++) {
            stmt.setLong(1, i);
            stmt.setString(2, getNextUeId());
            stmt.setDate(3, getNextStartDate());
            stmt.setDate(4, getNextEndDate());
            stmt.setInt(5, rand.nextInt());
            stmt.setInt(6, rand.nextInt());
            stmt.setInt(7, rand.nextInt(10));
            stmt.setInt(8, getMCC());
            stmt.setInt(9, getMNC());
            stmt.setInt(10, rand.nextInt(100));
            stmt.setString(11, getNextIMEI());

            stmt.addBatch();

            // Execute batch of 1000 records
            if (i % batchSize == 0) {
                stmt.executeBatch();
                conn.commit();
                System.out.println("Batch " + (counter++) + " executed successfully");
            }
        }
        // execute final batch
        stmt.executeBatch();
        System.out.println("Final Batch Executed " + (counter++) + " executed successfully");
        conn.commit();
        conn.close();

    }

    private String getNextIMEI() {
        int n = 1000000000 + rand.nextInt(900000000);
        return "" + n;
    }

    private int getMNC() {

        return mncMap.get(rand.nextInt(3));
    }

    private int getMCC() {

        return mccMap.get(rand.nextInt(3));
    }

    private Date getNextEndDate() {

        return new Date(System.currentTimeMillis() + 1);
    }

    private Date getNextStartDate() {

        return new Date(System.currentTimeMillis());
    }

    private String getNextUeId() {

        int n = 300000000 + rand.nextInt(900000000);

        return "" + n;
    }

    public static void main(String[] args) throws DatabaseServiceException, SQLException {
        DBExtensionTestUtils testUtil = new DBExtensionTestUtils();
        testUtil.generatePgSQLTestData(SAMPLE_SIZE, BATCH_SIZE);
        // testUtil.generateMySQLTestData();
    }

    public static void cleanUpTestData(DatabaseConfiguration dbConfig) {
        Statement stmt = null;
        Connection conn = null;
        try {
            DatabaseService dbService = DatabaseService.get(dbConfig.getDatabaseType());
            conn = dbService.getConnection(dbConfig);
            stmt = conn.createStatement();

            DatabaseMetaData dbm = conn.getMetaData();
            // check if "employee" table is there
            ResultSet tables = dbm.getTables(null, null, DEFAULT_TEST_TABLE, null);
            if (tables.next()) {
                stmt.executeUpdate("DROP TABLE " + DEFAULT_TEST_TABLE);
                // System.out.println("Drop Table Result::" + dropResult);
            }

            logger.info("Database Test Cleanup Done");

        } catch (DatabaseServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    public static File createTempDirectory(String name)
            throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }

    public static String getJDBCUrl(DatabaseConfiguration dbConfig) {
        Map<String, Object> substitutes = new HashMap<String, Object>();
        substitutes.put("dbType", dbConfig.getDatabaseType());
        substitutes.put("host", dbConfig.getDatabaseHost());
        substitutes.put("port", "" + dbConfig.getDatabasePort());
        substitutes.put("dbName", dbConfig.getDatabaseName());
        substitutes.put("useSSL", dbConfig.isUseSSL());
        String urlTemplate = "jdbc:${dbType}://${host}:${port}/${dbName}?useSSL=${useSSL}";
        StringSubstitutor strSub = new StringSubstitutor(substitutes);
        return strSub.replace(urlTemplate);
    }

}
