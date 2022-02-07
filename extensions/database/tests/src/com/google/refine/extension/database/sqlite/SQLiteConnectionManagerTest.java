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

import com.google.refine.extension.database.DBExtensionTests;
import com.google.refine.extension.database.DatabaseConfiguration;
import com.google.refine.extension.database.DatabaseService;
import com.google.refine.extension.database.DatabaseServiceException;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@Test(groups = { "requiresSQLite" })
public class SQLiteConnectionManagerTest extends DBExtensionTests {

    private DatabaseConfiguration testDbConfig;

    @BeforeTest
    @Parameters({ "sqliteDbName", "sqliteTestTable" })
    public void beforeTest(@Optional(DEFAULT_SQLITE_DB_NAME) String sqliteDbName,
            @Optional(DEFAULT_TEST_TABLE) String sqliteTestTable)
            throws DatabaseServiceException, SQLException {

        MockitoAnnotations.initMocks(this);

        testDbConfig = new DatabaseConfiguration();
        testDbConfig.setDatabaseName(sqliteDbName);
        testDbConfig.setDatabaseType(SQLiteDatabaseService.DB_NAME);

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
    public void testTestConnection() throws DatabaseServiceException {
        boolean isConnected = SQLiteConnectionManager.getInstance().testConnection(testDbConfig);
        Assert.assertTrue(isConnected);
    }

    @Test
    public void testGetConnection() throws DatabaseServiceException {

        Connection conn = SQLiteConnectionManager.getInstance().getConnection(testDbConfig);
        Assert.assertNotNull(conn);
    }

    @Test
    public void testShutdown() throws DatabaseServiceException, SQLException {

        Connection conn = SQLiteConnectionManager.getInstance().getConnection(testDbConfig);
        Assert.assertNotNull(conn);

        SQLiteConnectionManager.getInstance().shutdown();

        if (conn != null) {
            Assert.assertTrue(conn.isClosed());
        }

    }
}
