
package com.google.refine.extension.database;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseQueryInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.importing.ImportingJob;

public class DBQueryResultImportReaderTest {

    @Test
    public void testGettingColumnNamesAsHeader() throws IOException {
        // given
        ImportingJob job = mock(ImportingJob.class);
        DatabaseService dbService = mock(DatabaseService.class);
        String querySource = "db_url";
        List<DatabaseColumn> columns = List.of(
                new DatabaseColumn("name1", 3, DatabaseColumnType.STRING),
                new DatabaseColumn("name2", 7, DatabaseColumnType.BOOLEAN),
                new DatabaseColumn("name3", 5, DatabaseColumnType.FLOAT));
        DatabaseQueryInfo dbQueryInfo = mock(DatabaseQueryInfo.class);

        DBQueryResultImportReader reader = new DBQueryResultImportReader(job, dbService, querySource, columns, dbQueryInfo, 10, 100);
        assertFalse(reader.isUsedHeaders());

        // when
        List<Object> header = reader.getNextRowOfCells();

        // then
        assertEquals(header, List.of("name1", "name2", "name3"));
        assertTrue(reader.isUsedHeaders());
    }

    @DataProvider
    public Object[][] batches() {
        return new Object[][] {
                { 100, 10 }, // tableCount is a multiple of batchSize
                { 100, 8 }, // tableCount % batchSize leaves a rest (last batch is smaller than batchSize)
                { 0, 10 } // table is empty
        };
    }

    @Test(dataProvider = "batches")
    public void testLoadingAllBatches(final int tableCount, final int batchSize) throws IOException, DatabaseServiceException {
        // given
        ImportingJob job = mock(ImportingJob.class);
        String querySource = "db_url";
        List<DatabaseColumn> columns = List.of(
                new DatabaseColumn("name1", 3, DatabaseColumnType.STRING),
                new DatabaseColumn("name2", 7, DatabaseColumnType.BOOLEAN),
                new DatabaseColumn("name3", 5, DatabaseColumnType.FLOAT));
        DatabaseQueryInfo dbQueryInfo = new DatabaseQueryInfo(new DatabaseConfiguration(), "SELECT * FROM TABLE");

        DatabaseService dbService = stubDatabaseServiceForBatchQueries(columns, tableCount, batchSize); // simulate db

        DBQueryResultImportReader reader = new DBQueryResultImportReader(job, dbService, querySource, columns, dbQueryInfo,
                batchSize, tableCount);

        // when
        reader.getNextRowOfCells(); // ignore first call (header)

        // simulate calls in TabularImportingParserBase.readTable
        int count = tableCount;
        int sizeOfLastBatch = tableCount % batchSize;
        while (reader.getNextRowOfCells() != null) {
            count--;
            assertEquals(reader.isEnd(), count < sizeOfLastBatch,
                    "end should only be set after querying last batch, but was set after " + (tableCount - count) + " rows");
        }

        // then
        assertEquals(count, 0);
        verify(dbService, times(tableCount / batchSize + 1)).getRows(any(), any());
    }

    /** Simulate querying database via DatabaseService **/
    private DatabaseService stubDatabaseServiceForBatchQueries(List<DatabaseColumn> columns, int tableCount, int batchSize)
            throws DatabaseServiceException {

        DatabaseService dbService = mock(DatabaseService.class);

        // simply add limit to query string
        when(dbService.buildLimitQuery(anyInt(), anyInt(), anyString())).thenCallRealMethod();

        // simulate table that contains tableCount rows
        // all rows can be retrieved in batches of batchSize via calls to dbService.getRows()
        when(dbService.getRows(any(), anyString())).thenAnswer(new Answer<List<DatabaseRow>>() {

            private int rowCount = 0;

            public List<DatabaseRow> answer(InvocationOnMock invocation) {
                List<DatabaseRow> rows = new ArrayList<>();

                for (int i = 0; i < batchSize && rowCount < tableCount; i++, rowCount++) {
                    DatabaseRow row = new DatabaseRow();
                    row.setIndex(rowCount);
                    row.setValues(generateRowValuesBasedOnColumnTypes(columns));
                    rows.add(row);
                }

                return rows;
            }
        });

        return dbService;
    }

    private List<String> generateRowValuesBasedOnColumnTypes(List<DatabaseColumn> columns) {
        List<String> rowValues = new ArrayList<>(columns.size());

        for (DatabaseColumn column : columns) {
            switch (column.getType()) {
                case STRING:
                    rowValues.add("test");
                    break;
                case NUMBER:
                    rowValues.add("1234567890");
                    break;
                case DATETIME:
                    rowValues.add("2025-10-22 09:30:45");
                    break;
                case LOCATION:
                    rowValues.add("is never used anyway");
                    break;
                case BOOLEAN:
                    rowValues.add("TRUE");
                    break;
                case DATE:
                    rowValues.add("2025-10-22");
                    break;
                case DOUBLE:
                case FLOAT:
                    rowValues.add("1.0");
                    break;
            }
        }
        return rowValues;
    }
}
