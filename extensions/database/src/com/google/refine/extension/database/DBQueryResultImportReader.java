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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.extension.database.model.DatabaseColumn;
import com.google.refine.extension.database.model.DatabaseQueryInfo;
import com.google.refine.extension.database.model.DatabaseRow;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingJob;

public class DBQueryResultImportReader implements TableDataReader {

    private static final Logger logger = LoggerFactory.getLogger("DBQueryResultImportReader");

    private final ImportingJob job;
    private final String querySource;
    private List<DatabaseColumn> dbColumns;
    private final int batchSize;

    private int nextRow = 0; // 0-based
    private int batchRowStart = 0; // 0-based
    private boolean end = false;
    private List<List<Object>> rowsOfCells = null;
    private boolean usedHeaders = false;
    private DatabaseService databaseService;
    private DatabaseQueryInfo dbQueryInfo;
    private final Integer totalCount;
    private final boolean useTotalForProgress;

    /**
     * @deprecated use constructor that includes the total count instead
     */
    @Deprecated
    public DBQueryResultImportReader(
            ImportingJob job,
            DatabaseService databaseService,
            String querySource,
            List<DatabaseColumn> columns,
            DatabaseQueryInfo dbQueryInfo,
            int batchSize) {
        this(job, databaseService, querySource, columns, dbQueryInfo, batchSize, null);
    }

    public DBQueryResultImportReader(
            ImportingJob job,
            DatabaseService databaseService,
            String querySource,
            List<DatabaseColumn> columns,
            DatabaseQueryInfo dbQueryInfo,
            int batchSize,
            Integer count) {

        this.job = job;
        this.querySource = querySource;
        this.batchSize = batchSize;
        this.totalCount = count;
        this.useTotalForProgress = totalCount != null && totalCount > 0;
        this.dbColumns = columns;
        this.databaseService = databaseService;
        this.dbQueryInfo = dbQueryInfo;

        logger.debug("Init with batchSize:{} and count:{}", batchSize, count);
    }

    @Override
    public List<Object> getNextRowOfCells() throws IOException {

        try {
            // on first call get column names for header
            if (!usedHeaders) {
                List<Object> row = new ArrayList<Object>(dbColumns.size());
                for (DatabaseColumn cd : dbColumns) {
                    row.add(cd.getName());
                }
                usedHeaders = true;
                logger.debug("Return header on first call: {}", row.stream().map(Object::toString).collect(Collectors.joining(",")));
                setProgress(job, buildProgressMessage(), -1);
                return row;
            }

            // load new batch from db
            if (rowsOfCells == null || nextRow - batchRowStart >= rowsOfCells.size()) {
                int newBatchRowStart = batchRowStart + (rowsOfCells == null ? 0 : rowsOfCells.size());
                rowsOfCells = getRowsOfCells(newBatchRowStart);
                logger.debug("Retrieved next {} rows from db.", rowsOfCells.size());
                batchRowStart = newBatchRowStart;
            }

            // return next row
            if (rowsOfCells != null && nextRow - batchRowStart < rowsOfCells.size()) {
                List<Object> result = rowsOfCells.get(nextRow++ - batchRowStart);
                setProgress(job, buildProgressMessage(), calculateProgress());
                return result;
            } else {
                logger.debug("Reached end of table at {} rows.", nextRow);
                return null;
            }

        } catch (DatabaseServiceException e) {
            // rethrow exception as IOException
            logger.error("DatabaseServiceException::{}", e);
            throw new IOException(e);
        }
    }

    /**
     * @param startRow
     * @return
     * @throws IOException
     * @throws DatabaseServiceException
     */
    private List<List<Object>> getRowsOfCells(int startRow) throws IOException, DatabaseServiceException {
        logger.debug("Query db for next batch: [{},{}]", startRow + 1, startRow + batchSize);

        // if end was already reached, do not query db again
        if (end) {
            logger.debug("No more batches to query, reached end of table.");
            return Collections.emptyList();
        }

        // build query to retrieve next batch from db
        String query = databaseService.buildLimitQuery(batchSize, startRow, dbQueryInfo.getQuery());
        logger.debug("batchSize::{} startRow::{} query::{}", batchSize, startRow, query);

        // retrieve next batch from db
        List<DatabaseRow> dbRows = databaseService.getRows(dbQueryInfo.getDbConfig(), query);
        if (dbRows == null || dbRows.isEmpty()) {
            end = true;
            return new ArrayList<>();
        }

        // parse db rows
        List<List<Object>> rowsOfCells = new ArrayList<>(dbRows.size());
        for (DatabaseRow dbRow : dbRows) {
            List<String> row = dbRow.getValues();
            List<Object> rowOfCells = new ArrayList<>(row.size());

            for (int j = 0; j < row.size() && j < dbColumns.size(); j++) {

                String text = row.get(j);
                if (text == null || text.isEmpty()) {
                    rowOfCells.add(null);
                } else {
                    DatabaseColumn col = dbColumns.get(j);
                    if (col.getType() == DatabaseColumnType.NUMBER) {
                        try {
                            rowOfCells.add(Long.parseLong(text));
                            continue;
                        } catch (NumberFormatException e) {
                        }

                    } else if (col.getType() == DatabaseColumnType.DOUBLE || col.getType() == DatabaseColumnType.FLOAT) {
                        try {
                            double d = Double.parseDouble(text);
                            if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                                rowOfCells.add(d);
                                continue;
                            }
                        } catch (NumberFormatException e) {
                        }

                    }

                    rowOfCells.add(text);
                }

            }

            rowsOfCells.add(rowOfCells);
        }

        end = dbRows.size() < batchSize;
        return rowsOfCells;
    }

    private int calculateProgress() {
        // only use `totalCount` if it is available; otherwise progress can not be tracked
        return useTotalForProgress ? (int) (((double) nextRow / totalCount) * 100) : -1;
    }

    private String buildProgressMessage() {
        String totalFormatted = useTotalForProgress ? totalCount.toString() : "?";
        return "Reading " + querySource + " (" + nextRow + " / " + totalFormatted + ")";
    }

    private static void setProgress(ImportingJob job, String message, int percent) {
        job.setProgress(percent, message);
    }

    public List<DatabaseColumn> getColumns() {
        return dbColumns;
    }

    public void setColumns(List<DatabaseColumn> columns) {
        this.dbColumns = columns;
    }

    public int getNextRow() {
        return nextRow;
    }

    public void setNextRow(int nextRow) {
        this.nextRow = nextRow;
    }

    public int getBatchRowStart() {
        return batchRowStart;
    }

    public void setBatchRowStart(int batchRowStart) {
        this.batchRowStart = batchRowStart;
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public List<List<Object>> getRowsOfCells() {
        return rowsOfCells;
    }

    public void setRowsOfCells(List<List<Object>> rowsOfCells) {
        this.rowsOfCells = rowsOfCells;
    }

    public boolean isUsedHeaders() {
        return usedHeaders;
    }

    public void setUsedHeaders(boolean usedHeaders) {
        this.usedHeaders = usedHeaders;
    }

    public ImportingJob getJob() {
        return job;
    }

    public String getQuerySource() {
        return querySource;
    }

    public int getBatchSize() {
        return batchSize;
    }

}
