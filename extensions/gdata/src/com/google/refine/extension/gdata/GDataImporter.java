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
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.client.spreadsheet.CellQuery;
import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.InvalidEntryException;
import com.google.gdata.util.ServiceException;
import com.google.refine.ProjectMetadata;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.importers.ImporterUtilities;
import com.google.refine.importers.UrlImporter;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;

/**
 * Google Refine importer for Google Spreadsheets.
 * 
 * @author Tom Morris <tfmorris@gmail.com>
 * @copyright 2010 Thomas F. Morris
 * @license New BSD http://www.opensource.org/licenses/bsd-license.php
 */
public class GDataImporter implements UrlImporter {

	static final String SERVICE_APP_NAME = "Google-Refine-GData-Extension";

	private FeedURLFactory factory;

    public GDataImporter() {
        // Careful - this constructor is called at server init time 
        // and is shared by everyone.
        factory = FeedURLFactory.getDefault();
    }

    @Override
    public void read(URL url, Project project, ProjectMetadata metadata,
            Properties options) throws Exception {

        int ignoreLines = ImporterUtilities.getIntegerOption("ignore", options, -1);
        int headerLines = ImporterUtilities.getIntegerOption("header-lines", options, 1);
        int limit = ImporterUtilities.getIntegerOption("limit", options, -1);

        // Note: Unlike TSV/CSV importer, we count all rows towards skip, not
        // just "data" rows
        int skip = ImporterUtilities.getIntegerOption("skip", options, 0);
        int dataStart = ignoreLines + headerLines + skip;
        boolean guessValueType = ImporterUtilities.getBooleanOption(
                "guess-value-type", options, true);

        // TODO: Put this in a namespace?
        metadata.setCustomMetadata("source-url", url.toExternalForm());

        // Start fresh for each read so that we're not caching authorization or
        // anything
        if (isSpreadsheetURL(url)) {
            importSpreadsheet(url, project, ignoreLines, headerLines, limit,
                    dataStart, guessValueType);
        } else if (isFusionTableURL(url)) {
            importFusionTable(url, project, ignoreLines, headerLines, limit,
                    dataStart, guessValueType);
        } else {
            // should never happen (famous last words)
            throw new IllegalArgumentException(
                    "Got invalid format URL in GDataImporter.read()");
        }
    }

    private void importSpreadsheet(URL url, Project project, int ignoreLines,
            int headerLines, int limit, int dataStart, boolean guessValueType)
            throws MalformedURLException, IOException, ServiceException,
            Exception {
        SpreadsheetService service = new SpreadsheetService(SERVICE_APP_NAME);
        // String token = TokenCookie.getToken(request);
        // if (token != null) {
        // service.setAuthSubToken(token);
        // }
        String spreadsheetKey = getSpreadsheetKey(url);
        WorksheetEntry worksheet;
        try {
            worksheet = getWorksheetEntries(service, spreadsheetKey).get(0);
        } catch (InvalidEntryException e) {
            throw new RuntimeException("Failed to open spreadsheet "
                    + e.getResponseBody(), e);
        }

        // Create columns
        List<String> columnHeaders = getColumnHeaders(service, worksheet,
                ignoreLines, headerLines);

        int columnCount = worksheet.getColCount();
        project.columnModel.setMaxCellIndex(columnCount);
        boolean validColumn[] = new boolean[columnCount];
        int index = 0;
        for (String name : columnHeaders) {
            Column column = new Column(index, name + " " + index);
            project.columnModel.columns.add(column);
            validColumn[index++] = true;
        }
        for (int i = index; index < columnCount; index++) {
            Column column = new Column(index, "Column " + index);
            project.columnModel.columns.add(column);
            validColumn[i] = true;
        }

        // Create data rows & cells
        int previousRow = dataStart - 1;
        int previousCol = -1;
        List<CellEntry> cellEntries = getCells(service, worksheet, dataStart);
        Row row = null;
        for (CellEntry cellEntry : cellEntries) {
            com.google.gdata.data.spreadsheet.Cell cell = cellEntry.getCell();
            if (cell == null) {
                continue;
            }
            int r = cell.getRow() - 1; // convert from 1-based to 0-based
            int c = cell.getCol() - 1;

            if (limit > 0 && r > limit) {
                break;
            }

            // Handle gaps in rows
            if (r > previousRow) {
                // Finish and add current row
                if (row != null) {
                    project.rows.add(row);
                    // project.columnModel.setMaxCellIndex(row.cells.size()); //
                    // TODO: ???
                }

                // Add empty rows for skipped rows
                while (previousRow < r - 1) {
                    project.rows.add(new Row(columnCount));
                    previousRow++;
                }
                row = new Row(columnCount);
                previousRow = r;
                previousCol = 0;
            }

            // Add blank cells for any that were skipped before the current one
            for (int col = previousCol + 1; col < c; col++) {
                row.cells.add(new Cell("", null));
            }
            previousCol = c;

            String s = cell.getValue();
            if (s != null) {
                s = s.trim();
            }
            if (ExpressionUtils.isNonBlankData(s)) {
                Serializable value = guessValueType ? ImporterUtilities
                        .parseCellValue(s) : s;
                row.cells.add(new Cell(value, null));
            } else {
                row.cells.add(null);
            }
        }
        // Add last row
        if (row != null) {
            project.rows.add(row);
        }
    }
    
    private void importFusionTable(URL url, Project project, int ignoreLines,
            int headerLines, int limit, int dataStart, boolean guessValueType)
            throws MalformedURLException, IOException, ServiceException,
            Exception {
        GoogleService service = new GoogleService("fusiontables", SERVICE_APP_NAME);
        // String token = TokenCookie.getToken(request);
        // if (token != null) {
        // service.setAuthSubToken(token);
        // }
        String tableId = getFusionTableKey(url);
        
        final String SERVICE_URL =
            "http://www.google.com/fusiontables/api/query";
        final String selectQuery = "select * from " + tableId 
            + " offset " + (dataStart) + (limit>0 ? (" limit " + limit):"");

        URL queryUrl = new URL(
                SERVICE_URL + "?sql=" + URLEncoder.encode(selectQuery, "UTF-8"));
        GDataRequest queryRequest = service.getRequestFactory().getRequest(
                RequestType.QUERY, queryUrl, ContentType.TEXT_PLAIN);
        queryRequest.execute();

        Scanner scanner = new Scanner(queryRequest.getResponseStream(),"UTF-8");

        // TODO: Just use the first row of data as column headers for now
        List<String> columnHeaders = getTableRow(scanner);

        // Create columns
        int columnCount = columnHeaders.size();
        project.columnModel.setMaxCellIndex(columnCount);
        boolean validColumn[] = new boolean[columnCount];
        int index = 0;
        for (String name : columnHeaders) {
            Column column = new Column(index, name + " " + index);
            project.columnModel.columns.add(column);
            validColumn[index++] = true;
        }
        for (int i = index; index < columnCount; index++) {
            Column column = new Column(index, "Column " + index);
            project.columnModel.columns.add(column);
            validColumn[i] = true;
        }

        // Create data rows & cells
        List<String> values = columnHeaders;
        while (values != null) {
            Row row = new Row(columnCount);
            for (String valString : values) {
                valString = valString.trim();
                if (ExpressionUtils.isNonBlankData(valString)) {
                    Serializable value = guessValueType ? ImporterUtilities
                            .parseCellValue(valString) : valString;
                            row.cells.add(new Cell(value, null));
                } else {
                    row.cells.add(null);
                }
            }
            project.rows.add(row);
            values = getTableRow(scanner);
        }
    }

    private List<String> getTableRow(Scanner scanner) {
        /**
         * CSV values are terminated by comma or end-of-line and consist either of
         * plain text without commas or quotes, or a quoted expression, where inner
         * quotes are escaped by doubling.
         */
        final Pattern CSV_VALUE_PATTERN =
            Pattern.compile("([^,\\r\\n\"]*|\"(([^\"]*\"\")*[^\"]*)\")(,|\\r?\\n)");

        if (!scanner.hasNextLine()) {
            return null;
        }

        List<String> result = new ArrayList<String>();
        while (scanner.hasNextLine()) {            
            scanner.findWithinHorizon(CSV_VALUE_PATTERN, 0);
            MatchResult match = scanner.match();
            String quotedString = match.group(2);
            String decoded = quotedString == null ? match.group(1)
                    : quotedString.replaceAll("\"\"", "\"");
            result.add(decoded);
            if (!match.group(4).equals(",")) {
                break;
            }
        }
        return result;
    }

    /**
     * Retrieves the spreadsheets that an authenticated user has access to. Not
     * valid for unauthenticated access.
     * 
     * @return a list of spreadsheet entries
     * @throws Exception
     *             if error in retrieving the spreadsheet information
     */
    public List<SpreadsheetEntry> getSpreadsheetEntries(
            SpreadsheetService service) throws Exception {
        SpreadsheetFeed feed = service.getFeed(
                factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
        return feed.getEntries();
    }

    public List<WorksheetEntry> getWorksheetEntries(SpreadsheetService service,
            String spreadsheetKey) throws MalformedURLException, IOException,
            ServiceException {
        WorksheetFeed feed = service
                .getFeed(factory.getWorksheetFeedUrl(spreadsheetKey, "public",
                        "values"), WorksheetFeed.class);
        return feed.getEntries();
    }

    /**
     * Retrieves the columns headers from the cell feed of the worksheet entry.
     * 
     * @param worksheet
     *            worksheet entry containing the cell feed in question
     * @return a list of column headers
     * @throws Exception
     *             if error in retrieving the spreadsheet information
     */
    public List<String> getColumnHeaders(SpreadsheetService service,
            WorksheetEntry worksheet, int startRow, int rows) throws Exception {
        List<String> headers = new ArrayList<String>();

        // Get the appropriate URL for a cell feed
        URL cellFeedUrl = worksheet.getCellFeedUrl();

        // Create a query for the cells in the header row(s) (1-based)
        CellQuery cellQuery = new CellQuery(cellFeedUrl);
        if (startRow > 0) {
            cellQuery.setMinimumRow(startRow + 1);
        }
        cellQuery.setMaximumRow(startRow + rows);

        // Get the cell feed matching the query
        CellFeed topRowCellFeed = service.query(cellQuery, CellFeed.class);

        // Get the cell entries from the feed
        List<CellEntry> cellEntries = topRowCellFeed.getEntries();
        for (CellEntry entry : cellEntries) {

            // Get the cell element from the entry
            com.google.gdata.data.spreadsheet.Cell cell = entry.getCell();
            int r = cell.getRow() - 1;
            if (cell != null) {
                if (r == startRow) {
                    headers.add(cell.getValue().trim());
                } else if (r < startRow + rows) {
                    headers.set(r, headers.get(r) + " "
                            + cell.getValue().trim());
                }
            }
        }

        return headers;
    }

    public List<CellEntry> getCells(SpreadsheetService service,
            WorksheetEntry worksheet, int startRow) throws IOException,
            ServiceException {

        URL cellFeedUrl = worksheet.getCellFeedUrl();

        // Create a query skipping the desired number of rows
        CellQuery cellQuery = new CellQuery(cellFeedUrl);
        cellQuery.setMinimumRow(startRow + 1); // 1-based
        int rows = worksheet.getRowCount();
        cellQuery.setMaximumRow(rows);
        // cellQuery.setMinimumCol(1);
        int cols = worksheet.getColCount();
        cellQuery.setMaximumCol(cols);
        cellQuery.setMaxResults(rows * cols);
        cellQuery.setReturnEmpty(false);

        CellFeed cellFeed = service.query(cellQuery, CellFeed.class);
        return cellFeed.getEntries();
    }

    List<ListEntry> getListEntries(SpreadsheetService service,
            WorksheetEntry worksheet) throws IOException, ServiceException {
        URL listFeedUrl = worksheet.getListFeedUrl();
        ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);
        return feed.getEntries();
    }

    @Override
    public boolean canImportData(String contentType, String filename) {
        return false;
    }

    @Override
    public boolean canImportData(URL url) {
        return isSpreadsheetURL(url) | isFusionTableURL(url);
    }

    private boolean isSpreadsheetURL(URL url) {
        String host = url.getHost();
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        // http://spreadsheets.google.com/ccc?key=tI36b9Fxk1lFBS83iR_3XQA&hl=en
        return host.endsWith(".google.com") && host.contains("spreadsheet") && query.contains("key=");
    }
    
    private boolean isFusionTableURL(URL url) {
        // http://www.google.com/fusiontables/DataSource?dsrcid=1219
        String query = url.getQuery();
        if (query == null) {
            query = "";
        }
        return url.getHost().endsWith(".google.com") 
                && url.getPath().startsWith("/fusiontables/DataSource")
                && query.contains("dsrcid=");
    }
    
    // Modified version of FeedURLFactor.getSpreadsheetKeyFromUrl()
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

    private String getFusionTableKey(URL url) {
        String query = url.getQuery();
        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith("dsrcid=")) {
                    int offset = ("dsrcid=").length();
                    String tableId = part.substring(offset);
                    // TODO: Any special id format considerations to worry about?
//                    if (tableId.startsWith("p") || !tableId.contains(".")) {
//                        return tableId;
//                    }
                    return tableId;
                }
            }
        }
        return null;
    }
}