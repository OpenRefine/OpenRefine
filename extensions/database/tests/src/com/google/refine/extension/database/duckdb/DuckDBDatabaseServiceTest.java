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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;

@Test(groups = { "requiresDuckDB" })
public class DuckDBDatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    @Parameters({ "duckdbDbName", "duckdbTestTable" })
    public void beforeTest(@Optional(DEFAULT_DUCKDB_DB_NAME) String duckdbDbName,
            @Optional(DEFAULT_TEST_TABLE) String duckdbTestTable)
            throws DatabaseServiceException, SQLException {

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseName(duckdbDbName);
        testDbConfig.setDatabaseType(DuckDBDatabaseService.DB_NAME);

        testTable = duckdbTestTable;

        DatabaseService.DBType.registerDatabase(DuckDBDatabaseService.DB_NAME, DuckDBDatabaseService.getInstance());
    }

    @Test
    public void testGetDatabaseUrl() {
        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        String dbUrl = duckDBDatabaseService.getDatabaseUrl(testDbConfig);

        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, "jdbc:duckdb:tests/resources/test_db.duckdb");
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        Connection conn = duckDBDatabaseService.getConnection(testDbConfig);

        Assert.assertNotNull(conn);
    }

    /*
     * We don't allow loading extensions because that executes arbitrary code
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetConnectionWithExtensions() throws DatabaseServiceException {
        DatabaseConfiguration testDbConfigWithExtensions = new DatabaseConfiguration();
        testDbConfigWithExtensions.setDatabaseName("test_db.duckdb?allow_unsigned_extensions=true");
        testDbConfigWithExtensions.setDatabaseType(DuckDBDatabaseService.DB_NAME);

        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        duckDBDatabaseService.getConnection(testDbConfigWithExtensions);
    }

    /*
     * We don't allow loading a remote DuckDB file to make remote code execution harder
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetConnectionWithRemoteFile() throws DatabaseServiceException {
        DatabaseConfiguration testDbConfigWithExtensions = new DatabaseConfiguration();
        testDbConfigWithExtensions
                .setDatabaseName("https://github.com/duckdb/duckdb/raw/main/test/sqlite_scanner/sqlite.db");
        testDbConfigWithExtensions.setDatabaseType(DuckDBDatabaseService.DB_NAME);

        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        duckDBDatabaseService.getConnection(testDbConfigWithExtensions);
    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);

        boolean result = duckDBDatabaseService.testConnection(testDbConfig);
        Assert.assertTrue(result);
    }

    @Test
    public void testConnect() throws DatabaseServiceException {

        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = duckDBDatabaseService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testExecuteQuery() throws DatabaseServiceException {
        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = duckDBDatabaseService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        String limitQuery = duckDBDatabaseService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery,
                "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetRows() throws DatabaseServiceException {
        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);
        List<DatabaseRow> dbRows = duckDBDatabaseService.getRows(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
    }

    @Test
    public void testGetInstance() {
        DuckDBDatabaseService instance = DuckDBDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;

        DuckDBDatabaseService duckDBDatabaseService = (DuckDBDatabaseService) DatabaseService
                .get(DuckDBDatabaseService.DB_NAME);

        dbColumns = duckDBDatabaseService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbColumns);
    }
}
