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

import com.google.refine.extension.database.*;
import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Test(groups = { "requiresSQLite" })
public class SQLiteDatabaseServiceTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;
    private String testTable;

    @BeforeTest
    @Parameters({ "sqliteDbName", "sqliteTestTable" })
    public void beforeTest(@Optional(DEFAULT_SQLITE_DB_NAME) String sqliteDbName,
            @Optional(DEFAULT_TEST_TABLE) String sqliteTestTable)
            throws DatabaseServiceException, SQLException {

        MockitoAnnotations.initMocks(this);
        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseName(sqliteDbName);
        testDbConfig.setDatabaseType(SQLiteDatabaseService.DB_NAME);

        testTable = sqliteTestTable;
        DBExtensionTestUtils.initTestData(testDbConfig, sqliteTestTable);

        DatabaseService.DBType.registerDatabase(SQLiteDatabaseService.DB_NAME, SQLiteDatabaseService.getInstance());
    }

    @AfterTest
    @Parameters({ "sqliteDbName" })
    public void afterTest(@Optional(DEFAULT_SQLITE_DB_NAME) String sqliteDbName) {
        File f = new File(sqliteDbName);
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    public void testGetDatabaseUrl() {
        SQLiteDatabaseService sqLiteDatabaseService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        String dbUrl = sqLiteDatabaseService.getDatabaseUrl(testDbConfig);

        Assert.assertNotNull(dbUrl);
        Assert.assertEquals(dbUrl, "jdbc:sqlite:extension_test_db.sqlite");
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        SQLiteDatabaseService sqLiteDatabaseService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        Connection conn = sqLiteDatabaseService.getConnection(testDbConfig);

        Assert.assertNotNull(conn);
    }

    @Test
    public void testTestConnection() throws DatabaseServiceException {
        SQLiteDatabaseService sqLiteDatabaseService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);

        boolean result = sqLiteDatabaseService.testConnection(testDbConfig);
        Assert.assertTrue(result);
    }

    @Test
    public void testConnect() throws DatabaseServiceException {

        SQLiteDatabaseService sqLiteDatabaseService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = sqLiteDatabaseService.connect(testDbConfig);
        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testExecuteQuery() throws DatabaseServiceException {
        SQLiteDatabaseService sqLiteDatabaseService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        DatabaseInfo databaseInfo = sqLiteDatabaseService.testQuery(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(databaseInfo);
    }

    @Test
    public void testBuildLimitQuery() {
        SQLiteDatabaseService sqliteSqlService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        String limitQuery = sqliteSqlService.buildLimitQuery(100, 0, "SELECT * FROM " + testTable);
        Assert.assertNotNull(limitQuery);
        Assert.assertEquals(limitQuery,
                "SELECT * FROM (SELECT * FROM " + testTable + ") data LIMIT " + 100 + " OFFSET " + 0 + ";");
    }

    @Test
    public void testGetRows() throws DatabaseServiceException {
        SQLiteDatabaseService sqliteSqlService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);
        List<DatabaseRow> dbRows = sqliteSqlService.getRows(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbRows);
    }

    @Test
    public void testGetInstance() {
        SQLiteDatabaseService instance = SQLiteDatabaseService.getInstance();
        Assert.assertNotNull(instance);
    }

    @Test
    public void testGetColumns() throws DatabaseServiceException {
        List<DatabaseColumn> dbColumns;

        SQLiteDatabaseService sqliteSqlService = (SQLiteDatabaseService) DatabaseService
                .get(SQLiteDatabaseService.DB_NAME);

        dbColumns = sqliteSqlService.getColumns(testDbConfig, "SELECT * FROM " + testTable);

        Assert.assertNotNull(dbColumns);
    }
}
