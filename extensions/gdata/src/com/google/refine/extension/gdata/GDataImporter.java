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
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingUtilities;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

/**
 * Google Refine parser for Google Spreadsheets.
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class GDataImporter extends TabularImportingParserBase {
    public GDataImporter() {
        super(false);
    }
    
    public void parseOneFile(
        Project project,
        ProjectMetadata metadata,
        ImportingJob job,
        JSONObject fileRecord,
        int limit,
        JSONObject options,
        List<Exception> exceptions
    ) throws IOException {
        String fileSource = ImportingUtilities.getFileSource(fileRecord);
        String urlString = JSONUtilities.getString(fileRecord, "url", null);
        URL url = new URL(urlString);

        SpreadsheetService service = new SpreadsheetService(GDataExtension.SERVICE_APP_NAME);
        // String token = TokenCookie.getToken(request);
        // if (token != null) {
        // service.setAuthSubToken(token);
        // }
        String spreadsheetKey = getSpreadsheetKey(url);
        
        int[] sheets = JSONUtilities.getIntArray(options, "sheets");
        for (int sheetIndex : sheets) {
            WorksheetEntry worksheet;
            try {
                worksheet = getWorksheetEntries(service, spreadsheetKey).get(sheetIndex);
            } catch (ServiceException e) {
                exceptions.add(e);
                continue;
            }
            
            readTable(
                project,
                metadata,
                job,
                new BatchRowReader(service, worksheet, 20),
                fileSource + "#" + worksheet.getTitle().getPlainText(),
                limit,
                options,
                exceptions
            );
        }
    }
    
    static private class BatchRowReader implements TableDataReader {
        final int batchSize;
        final SpreadsheetService service;
        final WorksheetEntry worksheet;
        final int totalRowCount;
        
        int nextRow = 0; // 0-based
        int batchRowStart = -1; // 0-based
        List<List<Object>> rowsOfCells = null;
        
        public BatchRowReader(SpreadsheetService service, WorksheetEntry worksheet, int batchSize) {
            this.service = service;
            this.worksheet = worksheet;
            this.batchSize = batchSize;
            this.totalRowCount = worksheet.getRowCount();
        }
        
        @Override
        public List<Object> getNextRowOfCells() throws IOException {
            if (rowsOfCells == null || nextRow > batchRowStart + rowsOfCells.size()) {
                batchRowStart = batchRowStart + (rowsOfCells == null ? 0 : rowsOfCells.size());
                if (batchRowStart < totalRowCount) {
                    try {
                        rowsOfCells = getRowsOfCells(service, worksheet, batchRowStart + 1, batchSize);
                    } catch (ServiceException e) {
                        rowsOfCells = null;
                        throw new IOException(e);
                    }
                } else {
                    rowsOfCells = null;
                }
            }
            
            if (rowsOfCells != null && nextRow - batchRowStart < rowsOfCells.size()) {
                return rowsOfCells.get(nextRow++ - batchRowStart);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Retrieves the spreadsheets that an authenticated user has access to. Not
     * valid for unauthenticated access.
     * 
     * @return a list of spreadsheet entries
     * @throws Exception
     *             if error in retrieving the spreadsheet information
     */
    static public List<SpreadsheetEntry> getSpreadsheetEntries(
        SpreadsheetService service
    ) throws Exception {
        SpreadsheetFeed feed = service.getFeed(
            GDataExtension.getFeedUrlFactory().getSpreadsheetsFeedUrl(),
            SpreadsheetFeed.class);
        return feed.getEntries();
    }
    
    static public List<WorksheetEntry> getWorksheetEntries(
        SpreadsheetService service, String spreadsheetKey
    ) throws MalformedURLException, IOException, ServiceException {
        WorksheetFeed feed = service.getFeed(
            GDataExtension.getFeedUrlFactory().getWorksheetFeedUrl(spreadsheetKey, "public", "values"),
            WorksheetFeed.class);
        return feed.getEntries();
    }
    
    static public List<List<Object>> getRowsOfCells(
        SpreadsheetService service,
        WorksheetEntry worksheet,
        int startRow, // 1-based
        int rowCount
    ) throws IOException, ServiceException {
        URL cellFeedUrl = worksheet.getCellFeedUrl();

        int minRow = Math.max(1, startRow);
        int maxRow = Math.min(worksheet.getRowCount(), startRow + rowCount - 1);
        int rows = maxRow - minRow + 1;
        int cols = worksheet.getColCount();
        
        CellQuery cellQuery = new CellQuery(cellFeedUrl);
        cellQuery.setMinimumRow(minRow);
        cellQuery.setMaximumRow(maxRow);
        cellQuery.setMaximumCol(cols);
        cellQuery.setMaxResults(rows * cols);
        cellQuery.setReturnEmpty(false);
        
        CellFeed cellFeed = service.query(cellQuery, CellFeed.class);
        List<CellEntry> cellEntries = cellFeed.getEntries();
        
        List<List<Object>> rowsOfCells = new ArrayList<List<Object>>(rows);
        for (CellEntry cellEntry : cellEntries) {
            Cell cell = cellEntry.getCell();
            int row = cell.getRow();
            int col = cell.getCol();
            
            while (row > rowsOfCells.size()) {
                rowsOfCells.add(new ArrayList<Object>(cols));
            }
            List<Object> rowOfCells = rowsOfCells.get(row - 1); // 1-based
            
            while (col > rowOfCells.size()) {
                rowOfCells.add(null);
            }
            rowOfCells.set(col - 1, cell.getValue());
        }
        return rowsOfCells;
    }

    // Modified version of FeedURLFactory.getSpreadsheetKeyFromUrl()
    private String getSpreadsheetKey(URL url) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");

            int offset = -1;
            int numParts = 0;
            String keyOrId = "";

            for (String part : parts) {
                if (part.startsWith("id=")) {
                    offset = ("id=").length();
                    keyOrId = part.substring(offset);
                    numParts = 4;
                    break;
                } else if (part.startsWith("key=")) {
                    offset = ("key=").length();
                    keyOrId = part.substring(offset);
                    if (keyOrId.startsWith("p") || !keyOrId.contains(".")) {
                        return keyOrId;
                    }
                    numParts = 2;
                    break;
                }
            }

            if (offset > -1) {
                String[] dottedParts = keyOrId.split("\\.");
                if (dottedParts.length == numParts) {
                    return dottedParts[0] + "." + dottedParts[1];
                }
            }
        }
        return null;
    }
}