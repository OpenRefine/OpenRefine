/*
 * Copyright (c) 2010, Thomas F. Morris
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
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.gdata.client.spreadsheet.CellQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;

import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

/**
 * OpenRefine parser for Google Spreadsheets.
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class GDataImporter {
    static public void parse(
        String token,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        int limit,
        JSONObject options,
        List<Exception> exceptions) {
    
        String docType = JSONUtilities.getString(options, "docType", null);
        if ("spreadsheet".equals(docType)) {
            SpreadsheetService service = GDataExtension.getSpreadsheetService(token);
            parse(
                service,
                project,
                metadata,
                job,
                limit,
                options,
                exceptions
            );
        } else if ("table".equals(docType)) {
            FusionTableImporter.parse(token, 
                project,
                metadata,
                job,
                limit,
                options,
                exceptions
            );
        }
    }
    
    static public void parse(
        SpreadsheetService service,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        int limit,
        JSONObject options,
        List<Exception> exceptions) {
        
        String docUrlString = JSONUtilities.getString(options, "docUrl", null);
        String worksheetUrlString = JSONUtilities.getString(options, "sheetUrl", null);
        if (docUrlString != null && worksheetUrlString != null) {
            try {
                parseOneWorkSheet(
                    service,
                    project,
                    metadata,
                    job,
                    new URL(docUrlString),
                    new URL(worksheetUrlString),
                    limit,
                    options,
                    exceptions);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                exceptions.add(e);
            }
        }
    }
    
    static public void parseOneWorkSheet(
        SpreadsheetService service,
        Project project,
        ProjectMetadata metadata,
        final ImportingJob job,
        URL docURL,
        URL worksheetURL,
        int limit,
        JSONObject options,
        List<Exception> exceptions) {
        
        try {
            WorksheetEntry worksheetEntry = service.getEntry(worksheetURL, WorksheetEntry.class);
            String spreadsheetName = docURL.toExternalForm();
            try {
                SpreadsheetEntry spreadsheetEntry = service.getEntry(docURL, SpreadsheetEntry.class);
                spreadsheetName = spreadsheetEntry.getTitle().getPlainText();
            } catch (ServiceException e) { // RedirectRequiredException among others
                // fall back to just using the URL (better for traceability anyway?)
            }
            
            String fileSource = spreadsheetName + " # " +
                worksheetEntry.getTitle().getPlainText();
            
            setProgress(job, fileSource, 0);
            TabularImportingParserBase.readTable(
                project,
                metadata,
                job,
                new WorksheetBatchRowReader(job, fileSource, service, worksheetEntry, 20),
                fileSource,
                limit,
                options,
                exceptions
            );
            setProgress(job, fileSource, 100);
        } catch (IOException e) {
            e.printStackTrace();
            exceptions.add(e);
        } catch (ServiceException e) {
            e.printStackTrace();
            exceptions.add(e);
        }
    }
    
    static private void setProgress(ImportingJob job, String fileSource, int percent) {
        job.setProgress(percent, "Reading " + fileSource);
    }
    
    static private class WorksheetBatchRowReader implements TableDataReader {
        final ImportingJob job;
        final String fileSource;
        
        final SpreadsheetService service;
        final WorksheetEntry worksheet;
        final int batchSize;
        
        final int totalRows;
        
        int nextRow = 0; // 0-based
        int batchRowStart = 0; // 0-based
        List<List<Object>> rowsOfCells = null;
        
        public WorksheetBatchRowReader(ImportingJob job, String fileSource,
                SpreadsheetService service, WorksheetEntry worksheet,
                int batchSize) {
            this.job = job;
            this.fileSource = fileSource;
            this.service = service;
            this.worksheet = worksheet;
            this.batchSize = batchSize;
            
            this.totalRows = worksheet.getRowCount();
        }
        
        @Override
        public List<Object> getNextRowOfCells() throws IOException {
            if (rowsOfCells == null || (nextRow >= batchRowStart + rowsOfCells.size() && nextRow < totalRows)) {
                int newBatchRowStart = batchRowStart + (rowsOfCells == null ? 0 : rowsOfCells.size());
                try {
                    rowsOfCells = getRowsOfCells(
                        service,
                        worksheet,
                        newBatchRowStart + 1, // convert to 1-based
                        batchSize);
                    
                    batchRowStart = newBatchRowStart;
                    
                    setProgress(job, fileSource, batchRowStart * 100 / totalRows);
                } catch (ServiceException e) {
                    throw new IOException(e);
                }
            }
            
            if (rowsOfCells != null && nextRow - batchRowStart < rowsOfCells.size()) {
                return rowsOfCells.get(nextRow++ - batchRowStart);
            } else {
                return null;
            }
        }
        
        
        List<List<Object>> getRowsOfCells(
            SpreadsheetService service,
            WorksheetEntry worksheet,
            int startRow, // 1-based
            int rowCount
        ) throws IOException, ServiceException {
            URL cellFeedUrl = worksheet.getCellFeedUrl();
            
            int minRow = startRow;
            int maxRow = Math.min(worksheet.getRowCount(), startRow + rowCount - 1);
            int cols = worksheet.getColCount();
            int rows = worksheet.getRowCount();
            
            CellQuery cellQuery = new CellQuery(cellFeedUrl);
            cellQuery.setMinimumRow(minRow);
            cellQuery.setMaximumRow(maxRow);
            cellQuery.setMaximumCol(cols);
            cellQuery.setMaxResults(rows * cols);
            cellQuery.setReturnEmpty(false);
            
            CellFeed cellFeed = service.query(cellQuery, CellFeed.class);
            List<CellEntry> cellEntries = cellFeed.getEntries();
            
            List<List<Object>> rowsOfCells = new ArrayList<List<Object>>(rowCount);
            for (CellEntry cellEntry : cellEntries) {
                Cell cell = cellEntry.getCell();
                if (cell != null) {
                    int row = cell.getRow() - startRow;
                    int col = cell.getCol() - 1;
                    
                    while (row >= rowsOfCells.size()) {
                        rowsOfCells.add(new ArrayList<Object>());
                    }
                    List<Object> rowOfCells = rowsOfCells.get(row);
                    
                    while (col >= rowOfCells.size()) {
                        rowOfCells.add(null);
                    }
                    rowOfCells.set(col, cell.getValue());
                }
            }
            return rowsOfCells;
        }
    }
}
    
