/*

Copyright 2010,2011 Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.extension.database;

import java.util.Properties;

import org.slf4j.Logger;

import com.google.refine.extension.database.mariadb.MariaDBDatabaseService;
import com.google.refine.extension.database.mysql.MySQLDatabaseService;
import com.google.refine.extension.database.pgsql.PgSQLDatabaseService;
import com.google.refine.extension.database.sqlite.SQLiteDatabaseService;

public class DBExtensionTests {

    protected final String MYSQL_DB_NAME = "mysql";
    protected final String DEFAULT_MYSQL_HOST = "127.0.0.1";
    protected final String DEFAULT_MYSQL_PORT = "3306";
    protected final String DEFAULT_MYSQL_USER = "root";
    protected final String DEFAULT_MYSQL_PASSWORD = "";
    protected final String DEFAULT_MYSQL_DB_NAME = "test_db";

    protected final String PGSQL_DB_NAME = "postgresql";
    protected final String DEFAULT_PGSQL_HOST = "127.0.0.1";
    protected final String DEFAULT_PGSQL_PORT = "5432";
    protected final String DEFAULT_PGSQL_USER = "postgres";
    protected final String DEFAULT_PGSQL_PASSWORD = "";
    protected final String DEFAULT_PGSQL_DB_NAME = "test_db";

    protected final String MARIA_DB_NAME = "mariadb";
    protected final String DEFAULT_MARIADB_HOST = "127.0.0.1";
    protected final String DEFAULT_MARIADB_PORT = "3306";
    protected final String DEFAULT_MARIADB_USER = "root";
    protected final String DEFAULT_MARIADB_PASSWORD = "";
    protected final String DEFAULT_MARIADB_NAME = "test_db";

    protected final String SQLITE_DB_NAME = "sqlite";
    protected final String DEFAULT_SQLITE_DB_NAME = "tests/resources/test_db.sqlite";

    protected final String DEFAULT_TEST_TABLE = "test_table";
    protected final String DEFAULT_SQLITE_TEST_TABLE = "test_data";

    protected Properties properties;

    protected Logger logger;

    protected String getTestParameter(String name, String defaultValue) {
        return System.getProperty(name, defaultValue);
    }

    protected String getDatabaseTestTable(String databasePrefix, String defaultValue) {
        return getTestParameter(databasePrefix + "TestTable", defaultValue);
    }

    protected DatabaseConfiguration getMySQLDatabaseConfiguration() {
        return getDatabaseConfiguration("mySql", MySQLDatabaseService.DB_NAME, DEFAULT_MYSQL_DB_NAME,
                DEFAULT_MYSQL_HOST, DEFAULT_MYSQL_PORT, DEFAULT_MYSQL_USER, DEFAULT_MYSQL_PASSWORD);
    }

    protected DatabaseConfiguration getPgSQLDatabaseConfiguration() {
        return getDatabaseConfiguration("pgSql", PgSQLDatabaseService.DB_NAME, DEFAULT_PGSQL_DB_NAME,
                DEFAULT_PGSQL_HOST, DEFAULT_PGSQL_PORT, DEFAULT_PGSQL_USER, DEFAULT_PGSQL_PASSWORD);
    }

    protected DatabaseConfiguration getMariaDBDatabaseConfiguration() {
        return getDatabaseConfiguration("mariadb", MariaDBDatabaseService.DB_NAME, DEFAULT_MARIADB_NAME,
                DEFAULT_MARIADB_HOST, DEFAULT_MARIADB_PORT, DEFAULT_MARIADB_USER, DEFAULT_MARIADB_PASSWORD);
    }

    protected DatabaseConfiguration getSQLiteDatabaseConfiguration() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseName(getTestParameter("sqliteDbName", DEFAULT_SQLITE_DB_NAME));
        config.setDatabaseType(SQLiteDatabaseService.DB_NAME);
        return config;
    }

    private DatabaseConfiguration getDatabaseConfiguration(String propertyPrefix, String databaseType,
            String defaultName, String defaultHost, String defaultPort, String defaultUser, String defaultPassword) {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseHost(getTestParameter(propertyPrefix + "DbHost", defaultHost));
        config.setDatabaseName(getTestParameter(propertyPrefix + "DbName", defaultName));
        config.setDatabasePassword(getTestParameter(propertyPrefix + "DbPassword", defaultPassword));
        config.setDatabasePort(Integer.parseInt(getTestParameter(propertyPrefix + "DbPort", defaultPort)));
        config.setDatabaseType(databaseType);
        config.setDatabaseUser(getTestParameter(propertyPrefix + "DbUser", defaultUser));
        config.setUseSSL(false);
        return config;
    }

}
