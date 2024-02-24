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

package com.google.refine.extension.database.generic;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.SQLType;

public class DatabaseConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger("mysql");
    private static DatabaseConnectionManager instance;
    private final SQLType type;
    private Connection connection;
    private static final Map<String, String> dbTypeToDriverMap = Map.of(
            "sqlite", "org.sqlite.JDBC",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "postgresql", "org.postgresql.Driver",
            "mssql", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "oracle", "oracle.jdbc.driver.OracleDriver");

    private DatabaseConnectionManager() {
        type = SQLType.forName(GenericDatabaseService.DB_NAME);
    }

    /**
     * Create a new instance of this connection manager.
     *
     * @return an instance of the manager
     */
    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("::Creating new SQLite ConnectionManager ::");
            }
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    public static String getDatabaseUrl(DatabaseConfiguration dbConfig) throws URISyntaxException {
        String DB_NAME = dbConfig.getDatabaseType().toLowerCase();
        String DB_DRIVER = dbTypeToDriverMap.get(DB_NAME);
        String jdbcUrl;
        int port = dbConfig.getDatabasePort();
        switch (DB_NAME) {
            case "sqlite":
                SQLType.registerSQLDriver(DB_NAME, DB_DRIVER, false);
                jdbcUrl = new URI("jdbc:" + dbConfig.getDatabaseType().toLowerCase(), dbConfig.getDatabaseName(), null).toASCIIString();
                break;
            case "mysql":
            case "postgresql":
            case "mssql":
                SQLType.registerSQLDriver(DB_NAME, DB_DRIVER, false);
                jdbcUrl = dbConfig.toURI().toString();
                break;
            case "oracle":
                jdbcUrl = "jdbc:oracle:thin:@" + dbConfig.getDatabaseHost() + ":" + port + ":" + dbConfig.getDatabaseName();
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + DB_NAME);
        }
        if (dbConfig.getDatabaseType().equalsIgnoreCase("sqlite")) {
            try {
                URI uri = new URI(jdbcUrl);
                return uri.toASCIIString();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return jdbcUrl;
    }

    /**
     * Get the SQL Database type.
     *
     * @return the type
     */
    public SQLType getType() {
        return this.type;
    }

    /**
     * testConnection
     *
     * @param dbConfig
     * @return boolean
     */
    public boolean testConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException {
        try {
            boolean connResult = false;

            Connection conn = getConnection(dbConfig);
            if (conn != null) {
                connResult = true;
                conn.close();
            }

            return connResult;
        } catch (SQLException e) {
            logger.error("Test connection Failed!", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        }
    }

    /**
     * Get a connection form the connection pool.
     *
     * @return connection from the pool
     */
    public Connection getConnection(DatabaseConfiguration databaseConfiguration) throws DatabaseServiceException {
        try {
            if (connection != null) {
                connection.close();
            }

            Class.forName(type.getClassPath());
            String dbURL = getDatabaseUrl(databaseConfiguration);
            if (databaseConfiguration.getDatabaseType().equalsIgnoreCase("sqlite")) {
                connection = DriverManager.getConnection(dbURL);
            }
            connection = DriverManager.getConnection(dbURL, databaseConfiguration.getDatabaseUser(),
                    databaseConfiguration.getDatabasePassword());

            logger.debug("*** Acquired New  connection for ::{} **** ", dbURL);

            return connection;
        } catch (ClassNotFoundException e) {
            logger.error("Jdbc Driver not found", e);
            throw new DatabaseServiceException(e.getMessage());
        } catch (SQLException e) {
            logger.error("SQLException::Couldn't get a Connection!", e);
            throw new DatabaseServiceException(true, e.getSQLState(), e.getErrorCode(), e.getMessage());
        } catch (URISyntaxException e) {
            logger.error("URISyntaxException::Couldn't get a Connection!", e);
            throw new DatabaseServiceException(e.getMessage());
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.warn("Non-Managed connection could not be closed. Whoops!", e);
            }
        }
    }
}
