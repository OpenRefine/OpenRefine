/*
 * Copyright (c) 2026, Mark G. Bilby (mgbilby)
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

package com.google.refine.extension.database.duckdb;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.SQLType;

public class DuckDBConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger("DuckDBConnectionManager");
    private static DuckDBConnectionManager instance;
    private final SQLType type;

    private DuckDBConnectionManager() {
        type = SQLType.forName(DuckDBDatabaseService.DB_NAME);
    }

    /**
     * Create a new instance of this connection manager.
     *
     * @return an instance of the manager
     */
    public static DuckDBConnectionManager getInstance() {
        if (instance == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("::Creating new DuckDB ConnectionManager ::");
            }
            instance = new DuckDBConnectionManager();
        }
        return instance;
    }

    public static String getDatabaseUrl(DatabaseConfiguration dbConfig) {
        String dbPath = dbConfig.getDatabaseName();
        if (dbPath == null || dbPath.isEmpty()) {
            throw new IllegalArgumentException("DuckDB database file path must not be empty.");
        }
        if (dbPath.contains("?")) {
            throw new IllegalArgumentException("Paths to DuckDB databases are not allowed to contain '?'");
        }
        if (dbPath.startsWith("//") || dbPath.startsWith("\\\\") || dbPath.startsWith("\\/") || dbPath.startsWith("/\\")) {
            throw new IllegalArgumentException("File path starts with illegal prefix; only local files are accepted.");
        }
        if (!new File(dbPath).isFile()) {
            throw new IllegalArgumentException("File could not be read: " + dbPath);
        }
        try {
            URI uri = new URI(
                    "jdbc:" + dbConfig.getDatabaseType().toLowerCase(),
                    dbPath,
                    null);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
        Connection conn = getConnection(dbConfig);
        return conn != null;
    }

    /**
     * Get a connection from the connection pool.
     *
     * @return connection from the pool
     */
    public Connection getConnection(DatabaseConfiguration databaseConfiguration) throws DatabaseServiceException {
        try {
            String dbURL = getDatabaseUrl(databaseConfiguration);
            Class.forName(type.getClassPath());
            Properties props = new Properties();
            props.setProperty("duckdb.read_only", "true");
            Connection connection = DriverManager.getConnection(dbURL, props);
            logger.debug("*** Acquired New  connection for ::{} **** ", dbURL);
            return connection;
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Couldn't get a Connection!", e);
            throw new DatabaseServiceException(e);
        }
    }
}
