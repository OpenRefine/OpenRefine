/*
 * Copyright (c) 2017, Tony Opara
 *        All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this 
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * 
 * Neither the name of Google nor the names of its contributors may be used to 
 * endorse or promote products derived from this software without specific 
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.google.refine.extension.database.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.DatabaseUtils;
import com.google.refine.extension.database.SQLType;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;

public class MySQLDatabaseService extends DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger("MySQLDatabaseService");
    public static final String DB_NAME = "mysql";
    public static final String DB_DRIVER = "com.mysql.jdbc.Driver";

    private static MySQLDatabaseService instance;

    private MySQLDatabaseService() {
    }

    public static MySQLDatabaseService getInstance() {
        if (instance == null) {
            SQLType.registerSQLDriver(DB_NAME, DB_DRIVER, false);
            instance = new MySQLDatabaseService();
            if (logger.isDebugEnabled()) {
                logger.debug("MySQLDatabaseService Instance: {}", instance);
            }
        }
        return instance;
    }

    @Override
    public boolean testConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        return MySQLConnectionManager.getInstance().testConnection(dbConfig);

    }

    @Override
    public DatabaseInfo connect(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        return getMetadata(dbConfig);
    }

    @Override
    public DatabaseInfo executeQuery(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException {
        Connection connection = MySQLConnectionManager.getInstance().getConnection(dbConfig, false);
        try (Statement statement = connection.createStatement();
                ResultSet queryResult = statement.executeQuery(query)) {
            java.sql.ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            ArrayList<DatabaseColumn> columns = new ArrayList<DatabaseColumn>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                DatabaseColumn dc = new DatabaseColumn(
                        metadata.getColumnName(i),
                        metadata.getColumnLabel(i),
                        DatabaseUtils.getDbColumnType(metadata.getColumnType(i)),
                        metadata.getColumnDisplaySize(i));
                columns.add(dc);
            }
            int index = 0;
            List<DatabaseRow> rows = new ArrayList<DatabaseRow>();
            while (queryResult.next()) {
                DatabaseRow row = new DatabaseRow();
                row.setIndex(index);
                List<String> values = new ArrayList<String>(columnCount);
                for (int i = 1; i <= columnCount; i++) {

                    values.add(queryResult.getString(i));

                }
                row.setValues(values);
                rows.add(row);
                index++;

            }
            DatabaseInfo dbInfo = new DatabaseInfo();
            dbInfo.setColumns(columns);
            dbInfo.setRows(rows);
            return dbInfo;
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        } finally {
            MySQLConnectionManager.getInstance().shutdown();
        }
    }

    /**
     * @param connectionInfo
     * @return
     * @throws DatabaseServiceException
     */
    private DatabaseInfo getMetadata(DatabaseConfiguration connectionInfo) throws DatabaseServiceException {
        try {
            Connection connection = MySQLConnectionManager.getInstance().getConnection(connectionInfo, true);
            if (connection != null) {
                java.sql.DatabaseMetaData metadata;
                metadata = connection.getMetaData();
                int dbMajorVersion = metadata.getDatabaseMajorVersion();
                int dbMinorVersion = metadata.getDatabaseMinorVersion();
                String dbProductVersion = metadata.getDatabaseProductVersion();
                String dbProductName = metadata.getDatabaseProductName();
                DatabaseInfo dbInfo = new DatabaseInfo();
                dbInfo.setDatabaseMajorVersion(dbMajorVersion);
                dbInfo.setDatabaseMinorVersion(dbMinorVersion);
                dbInfo.setDatabaseProductVersion(dbProductVersion);
                dbInfo.setDatabaseProductName(dbProductName);
                return dbInfo;
            }
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        }
        return null;
    }

    @Override
    public ArrayList<DatabaseColumn> getColumns(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException {
        Connection connection = MySQLConnectionManager.getInstance().getConnection(dbConfig, true);
        try (Statement statement = connection.createStatement();
                ResultSet queryResult = statement.executeQuery(query)) {
            java.sql.ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            ArrayList<DatabaseColumn> columns = new ArrayList<DatabaseColumn>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                DatabaseColumn dc = new DatabaseColumn(metadata.getColumnName(i), metadata.getColumnLabel(i),
                        DatabaseUtils.getDbColumnType(metadata.getColumnType(i)), metadata.getColumnDisplaySize(i));
                columns.add(dc);
            }
            return columns;
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public List<DatabaseRow> getRows(DatabaseConfiguration dbConfig, String query)
            throws DatabaseServiceException {
        Connection connection = MySQLConnectionManager.getInstance().getConnection(dbConfig, false);
        Statement statement = null;
        ResultSet queryResult = null;
        try {
            statement = connection.createStatement();
            statement.setFetchSize(10);
            queryResult = statement.executeQuery(query);
            java.sql.ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            int index = 0;
            List<DatabaseRow> rows = new ArrayList<DatabaseRow>();
            while (queryResult.next()) {
                DatabaseRow row = new DatabaseRow();
                row.setIndex(index);
                List<String> values = new ArrayList<String>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    values.add(queryResult.getString(i));
                }
                row.setValues(values);
                rows.add(row);
                index++;
            }
            return rows;
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    protected String getDatabaseUrl(DatabaseConfiguration dbConfig) {
        int port = dbConfig.getDatabasePort();
        return "jdbc:" + dbConfig.getDatabaseType() + "://" + dbConfig.getDatabaseHost()
                + ((port == 0) ? "" : (":" + port)) + "/" + dbConfig.getDatabaseName() + "?useSSL=" + dbConfig.isUseSSL();
    }

    @Override
    public Connection getConnection(DatabaseConfiguration dbConfig)
            throws DatabaseServiceException {
        // TODO Auto-generated method stub
        return MySQLConnectionManager.getInstance().getConnection(dbConfig, true);
    }

    @Override
    public DatabaseInfo testQuery(DatabaseConfiguration dbConfig, String query)
            throws DatabaseServiceException {
        Statement statement = null;
        ResultSet queryResult = null;
        try {
            Connection connection = MySQLConnectionManager.getInstance().getConnection(dbConfig, true);
            statement = connection.createStatement();
            queryResult = statement.executeQuery(query);
            DatabaseInfo dbInfo = new DatabaseInfo();
            return dbInfo;
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        } finally {
            try {
                if (queryResult != null) {
                    queryResult.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            MySQLConnectionManager.getInstance().shutdown();
        }
    }
}
