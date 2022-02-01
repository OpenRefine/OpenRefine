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

public class DBExtensionTests {

    protected final String MYSQL_DB_NAME = "mysql";
    protected final String DEFAULT_MYSQL_HOST = "127.0.0.1";
    protected final String DEFAULT_MYSQL_PORT = "3306";
    protected final String DEFAULT_MYSQL_USER = "root";
    protected final String DEFAULT_MYSQL_PASSWORD = "secret";
    protected final String DEFAULT_MYSQL_DB_NAME = "testdb";

    protected final String PGSQL_DB_NAME = "postgresql";
    protected final String DEFAULT_PGSQL_HOST = "127.0.0.1";
    protected final String DEFAULT_PGSQL_PORT = "5432";
    protected final String DEFAULT_PGSQL_USER = "postgres";
    protected final String DEFAULT_PGSQL_PASSWORD = "";
    protected final String DEFAULT_PGSQL_DB_NAME = "testdb";

    protected final String MARIA_DB_NAME = "mariadb";
    protected final String DEFAULT_MARIADB_HOST = "127.0.0.1";
    protected final String DEFAULT_MARIADB_PORT = "3306";
    protected final String DEFAULT_MARIADB_USER = "root";
    protected final String DEFAULT_MARIADB_PASSWORD = "secret";
    protected final String DEFAULT_MARIADB_NAME = "testdb";

    protected final String SQLITE_DB_NAME = "sqlite";
    protected final String DEFAULT_SQLITE_DB_NAME = "extension_test_db.sqlite";

    protected final String DEFAULT_TEST_TABLE = "test_data";

    protected Properties properties;

    protected Logger logger;

}
