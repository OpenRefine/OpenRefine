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

package com.google.refine.extension.database;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.refine.extension.database.sqlite.SQLiteDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;

public abstract class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger("DatabaseService");

    public static class DBType {

        private static Map<String, DatabaseService> databaseServiceMap = new HashMap<String, DatabaseService>();

        static {
            try {

                DatabaseService.DBType.registerDatabase(MySQLDatabaseService.DB_NAME, MySQLDatabaseService.getInstance());
                DatabaseService.DBType.registerDatabase(PgSQLDatabaseService.DB_NAME, PgSQLDatabaseService.getInstance());
                DatabaseService.DBType.registerDatabase(MariaDBDatabaseService.DB_NAME, MariaDBDatabaseService.getInstance());
                DatabaseService.DBType.registerDatabase(SQLiteDatabaseService.DB_NAME, SQLiteDatabaseService.getInstance());

            } catch (Exception e) {
                logger.error("Exception occurred while trying to prepare databases!", e);
            }
        }

        public static void registerDatabase(String name, DatabaseService db) {

            if (!databaseServiceMap.containsKey(name)) {
                // throw new DatabaseServiceException(name + " cannot be registered. Database Type already exists");
                databaseServiceMap.put(name, db);
                logger.info(String.format("Registered %s Database", name));
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug(name + " Database Type already exists");
                }

            }

        }

        public static DatabaseService getJdbcServiceFromType(String name) {
            return databaseServiceMap.get(name);
        }

    }

    protected String getDatabaseUrl(DatabaseConfiguration dbConfig) {
        int port = dbConfig.getDatabasePort();
        return "jdbc:" + dbConfig.getDatabaseType() + "://" + dbConfig.getDatabaseHost()
                + ((port == 0) ? "" : (":" + port)) + "/" + dbConfig.getDatabaseName();
    }

    /**
     * get Database
     * 
     * @param dbType
     * @return
     */
    public static DatabaseService get(String dbType) {
        logger.debug("get called on DatabaseService with, {}", dbType);
        DatabaseService databaseService = DatabaseService.DBType.getJdbcServiceFromType(dbType.toLowerCase());

        logger.debug("DatabaseService found: {}", databaseService.getClass());
        return databaseService;

    }

    // Database Service APIs
    public abstract Connection getConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException;

    public abstract boolean testConnection(DatabaseConfiguration dbConfig) throws DatabaseServiceException;

    public abstract DatabaseInfo connect(DatabaseConfiguration dbConfig) throws DatabaseServiceException;

    public abstract DatabaseInfo executeQuery(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException;

    public abstract DatabaseInfo testQuery(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException;

    public String buildLimitQuery(Integer limit, Integer offset, String query) {
        if (logger.isDebugEnabled()) {
            logger.info("<<< original input query::{} >>>", query);
        }

        final int len = query.length();
        String parsedQuery = len > 0 && query.endsWith(";") ? query.substring(0, len - 1) : query;

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM (");
        sb.append(parsedQuery);
        sb.append(") data");

        if (limit != null) {
            sb.append(" LIMIT" + " " + limit);
        }

        if (offset != null) {
            sb.append(" OFFSET" + " " + offset);
        }
        sb.append(";");
        String parsedQueryOut = sb.toString();

        if (logger.isDebugEnabled()) {
            logger.info("<<<Final input query::{} >>>", parsedQueryOut);
        }

        return parsedQueryOut;
    }

    public abstract List<DatabaseColumn> getColumns(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException;

    public abstract List<DatabaseRow> getRows(DatabaseConfiguration dbConfig, String query) throws DatabaseServiceException;

}
