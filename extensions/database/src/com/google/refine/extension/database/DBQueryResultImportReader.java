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
import java.util.List;

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
    private int processedRows = 0;
    private static int progress = 0;

    public DBQueryResultImportReader(
            ImportingJob job,
            DatabaseService databaseService,
            String querySource,
            List<DatabaseColumn> columns,
            DatabaseQueryInfo dbQueryInfo,
            int batchSize) {

        this.job = job;
        this.querySource = querySource;
        this.batchSize = batchSize;
        this.dbColumns = columns;
        this.databaseService = databaseService;
        this.dbQueryInfo = dbQueryInfo;
        if (logger.isDebugEnabled()) {
            logger.debug("batchSize:" + batchSize);
        }

    }

    @Override
    public List<Object> getNextRowOfCells() throws IOException {

        try {

            if (!usedHeaders) {
                List<Object> row = new ArrayList<Object>(dbColumns.size());
                for (DatabaseColumn cd : dbColumns) {
                    row.add(cd.getName());
                }
                usedHeaders = true;
                // logger.info("Exit::getNextRowOfCells return header::row:" + row);
                return row;
            }

            if (rowsOfCells == null || (nextRow >= batchRowStart + rowsOfCells.size() && !end)) {
                int newBatchRowStart = batchRowStart + (rowsOfCells == null ? 0 : rowsOfCells.size());
                rowsOfCells = getRowsOfCells(newBatchRowStart);
                processedRows = processedRows + rowsOfCells.size();
                batchRowStart = newBatchRowStart;
                setProgress(job, querySource, -1 /* batchRowStart * 100 / totalRows */);
            }

            if (rowsOfCells != null && nextRow - batchRowStart < rowsOfCells.size()) {
                List<Object> result = rowsOfCells.get(nextRow++ - batchRowStart);
                if (nextRow >= batchSize) {
                    rowsOfCells = getRowsOfCells(processedRows);
                    processedRows = processedRows + rowsOfCells.size();

                    if (logger.isDebugEnabled()) {
                        logger.debug("[[ Returning last row in batch:nextRow::{}, processedRows:{} ]]", nextRow, processedRows);
                    }

                    nextRow = 0;
                    if (processedRows % 100 == 0) {
                        setProgress(job, querySource, progress++);
                    }
                    if (processedRows % 10000 == 0) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("[[ {} rows processed... ]]", processedRows);
                        }
                    }
                }
                return result;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("[[processedRows:{} ]]", processedRows);
                }
                return null;
            }

        } catch (DatabaseServiceException e) {
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
        // logger.info("Entry getRowsOfCells::startRow:" + startRow);

        List<List<Object>> rowsOfCells = new ArrayList<List<Object>>(batchSize);

        String query = databaseService.buildLimitQuery(batchSize, startRow, dbQueryInfo.getQuery());
        // logger.info("batchSize::" + batchSize + " startRow::" + startRow + " query::" + query );

        List<DatabaseRow> dbRows = databaseService.getRows(dbQueryInfo.getDbConfig(), query);

        if (dbRows != null && !dbRows.isEmpty() && dbRows.size() > 0) {

            for (DatabaseRow dbRow : dbRows) {
                List<String> row = dbRow.getValues();
                List<Object> rowOfCells = new ArrayList<Object>(row.size());

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

        }
        end = dbRows.size() < batchSize + 1;
        // logger.info("Exit::getRowsOfCells::rowsOfCells:{}", rowsOfCells);
        return rowsOfCells;

    }

    private static void setProgress(ImportingJob job, String querySource, int percent) {
        job.setProgress(percent, "Reading " + querySource);
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
