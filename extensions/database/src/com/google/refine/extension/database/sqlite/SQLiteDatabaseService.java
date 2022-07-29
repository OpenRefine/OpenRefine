/*
 * Copyright (c) 2020, Chris Parker
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

package com.google.refine.extension.database.sqlite;

import com.google.refine.extension.database.SQLType;
import com.google.refine.extension.database.*;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDatabaseService extends DatabaseService {

    public static final String DB_NAME = "sqlite";
    public static final String DB_DRIVER = "org.sqlite.JDBC";
    private static final Logger logger = LoggerFactory.getLogger("SQLiteDatabaseService");
    private static SQLiteDatabaseService instance;

    public static SQLiteDatabaseService getInstance() {
        if (instance == null) {
            SQLType.registerSQLDriver(DB_NAME, DB_DRIVER, false);
            instance = new SQLiteDatabaseService();
            if (logger.isDebugEnabled()) {
                logger.debug("SQLiteDatabaseService Instance: {}", instance);
            }
        }
        return instance;
    }

    @Override
    protected String getDatabaseUrl(DatabaseConfiguration dbConfig) {
        return SQLiteConnectionManager.getDatabaseUrl(dbConfig);
    }

    @Override
    public Connection getConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        return SQLiteConnectionManager.getInstance().getConnection(dbConfig);
    }

    @Override
    public boolean testConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        return SQLiteConnectionManager.getInstance().testConnection(dbConfig);
    }

    @Override
    public DatabaseInfo connect(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        return getMetadata(dbConfig);
    }

    /**
     * @param dbConfig
     * @return
     * @throws DatabaseServiceException
     */
    private DatabaseInfo getMetadata(DatabaseConfiguration connectionInfo) throws DatabaseServiceException {
        try {
            Connection connection = SQLiteConnectionManager.getInstance().getConnection(connectionInfo);
            if (connection != null) {
                java.sql.DatabaseMetaData metadata = connection.getMetaData();
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
    public DatabaseInfo executeQuery(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException {
        Connection connection = SQLiteConnectionManager.getInstance().getConnection(dbConfig);
        try (Statement statement = connection.createStatement();
                ResultSet queryResult = statement.executeQuery(query)) {
            ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            ArrayList<DatabaseColumn> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                DatabaseColumn dc = new DatabaseColumn(metadata.getColumnName(i), metadata.getColumnLabel(i),
                        DatabaseUtils.getDbColumnType(metadata.getColumnType(i)),
                        metadata.getColumnDisplaySize(i));
                columns.add(dc);
            }
            int index = 0;
            List<DatabaseRow> rows = new ArrayList<>();
            while (queryResult.next()) {
                DatabaseRow row = new DatabaseRow();
                row.setIndex(index);
                List<String> values = new ArrayList<>(columnCount);
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
            SQLiteConnectionManager.getInstance().shutdown();
        }
    }

    @Override
    public DatabaseInfo testQuery(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException {
        Statement statement = null;
        ResultSet queryResult = null;
        try {
            Connection connection = SQLiteConnectionManager.getInstance().getConnection(dbConfig);
            statement = connection.createStatement();
            queryResult = statement.executeQuery(query);
            return new DatabaseInfo();
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
            SQLiteConnectionManager.getInstance().shutdown();
        }
    }

    @Override
    public List<DatabaseColumn> getColumns(DatabaseConfiguration dbConfig, String query)
            throws DatabaseServiceException {
        Connection connection = SQLiteConnectionManager.getInstance().getConnection(dbConfig);
        try (Statement statement = connection.createStatement();
                ResultSet queryResult = statement.executeQuery(query)) {
            ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            ArrayList<DatabaseColumn> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                DatabaseColumn dc = new DatabaseColumn(metadata.getColumnName(i), metadata.getColumnLabel(i),
                        DatabaseUtils.getDbColumnType(metadata.getColumnType(i)),
                        metadata.getColumnDisplaySize(i));
                columns.add(dc);
            }
            return columns;
        } catch (SQLException e) {
            logger.error("SQLException::", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        }
    }

    @Override
    public List<DatabaseRow> getRows(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException {
        Connection connection = SQLiteConnectionManager.getInstance().getConnection(dbConfig);
        Statement statement = null;
        ResultSet queryResult = null;
        try {
            statement = connection.createStatement();
            statement.setFetchSize(10);
            queryResult = statement.executeQuery(query);
            ResultSetMetaData metadata = queryResult.getMetaData();
            int columnCount = metadata.getColumnCount();
            int index = 0;
            List<DatabaseRow> rows = new ArrayList<>();
            while (queryResult.next()) {
                DatabaseRow row = new DatabaseRow();
                row.setIndex(index);
                List<String> values = new ArrayList<>(columnCount);
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
}
