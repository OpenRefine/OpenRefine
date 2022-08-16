
package com.google.refine.extension.gdata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendCellsRequest;
import com.google.api.services.sheets.v4.model.AppendDimensionRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.refine.exporters.TabularSerializer;

class SpreadsheetSerializer implements TabularSerializer {

    static final Logger logger = LoggerFactory.getLogger("SpreadsheetSerializer");

    private static final int BATCH_SIZE = 500;

    private Sheets service;
    private String spreadsheetId;
    private List<Exception> exceptions;

    protected List<RowData> rows = new ArrayList<>();

    // FIXME: This is fragile. Can we find out how many columns we have rather than assuming
    // it'll always be the default A-Z?
    private int maxColumns = 26;

    SpreadsheetSerializer(Sheets service, String spreadsheetId, List<Exception> exceptions) {
        this.service = service;
        this.spreadsheetId = spreadsheetId;
        this.exceptions = exceptions;
    }

    @Override
    public void startFile(JsonNode options) {
    }

    @Override
    public void endFile() {
        if (rows.size() > 0) {
            sendBatch(rows);
        }
    }

    @Override
    public void addRow(List<CellData> cells, boolean isHeader) {
        List<com.google.api.services.sheets.v4.model.CellData> cellDatas = new ArrayList<>();
        RowData rowData = new RowData();

        for (int c = 0; c < cells.size(); c++) {
            CellData cellData = cells.get(c);
            cellDatas.add(cellData2sheetCellData(cellData));
        }

        rowData.setValues(cellDatas);
        rows.add(rowData);

        if (rows.size() >= BATCH_SIZE) {
            sendBatch(rows);
            if (exceptions.size() > 0) {
                throw new RuntimeException(exceptions.get(0));
            }
        }
    }

    private com.google.api.services.sheets.v4.model.CellData cellData2sheetCellData(CellData cellData) {
        com.google.api.services.sheets.v4.model.CellData sheetCellData = new com.google.api.services.sheets.v4.model.CellData();

        ExtendedValue ev = new ExtendedValue();
        if (cellData != null) {
            if (cellData.value instanceof String) {
                ev.setStringValue((String) cellData.value);
            } else if (cellData.value instanceof Integer) {
                ev.setNumberValue(new Double((Integer) cellData.value));
            } else if (cellData.value instanceof Double) {
                ev.setNumberValue((Double) cellData.value);
            } else if (cellData.value instanceof OffsetDateTime) {
                // supposedly started internally as a double, but not sure how to transform correctly
                // ev.setNumberValue((Double) cellData.value);
                ev.setStringValue(cellData.value.toString());
            } else if (cellData.value instanceof Boolean) {
                ev.setBoolValue((Boolean) cellData.value);
            } else if (cellData.value == null) {
                ev.setStringValue("");
            } else {
                ev.setStringValue(cellData.value.toString());
            }
        } else {
            ev.setStringValue("");
        }

        sheetCellData.setUserEnteredValue(ev);

        return sheetCellData;
    }

    private void sendBatch(List<RowData> rows) {
        List<Request> requests = prepareBatch(rows);

        // FIXME: We have a 10MB cap on the request size, but I'm not sure we've got a good
        // way to quickly tell how big our request is. Just reduce row count for now.
        BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
        requestBody.setIncludeSpreadsheetInResponse(false);
        requestBody.setRequests(requests);

        Sheets.Spreadsheets.BatchUpdate request;
        try {
            logger.debug("spreadsheetId: " + spreadsheetId);
//            logger.debug("requestBody:" + requestBody.toString());
            request = service.spreadsheets().batchUpdate(spreadsheetId, requestBody);
            BatchUpdateSpreadsheetResponse response = request.execute();
            logger.debug("response:" + response.toPrettyString());
        } catch (IOException e) {
            exceptions.add(e);
        } finally {
            requestBody.clear();
            requests.clear();
            rows.clear();
        }

    }

    protected List<Request> prepareBatch(List<RowData> rows) {
        List<Request> requests = new ArrayList<>();

        // If this row is wider than our sheet, add columns to the sheet
        int columns = rows.get(0).getValues().size();
        if (columns > maxColumns) {
            AppendDimensionRequest adr = new AppendDimensionRequest();
            adr.setDimension("COLUMNS");
            adr.setLength(columns - maxColumns);
            maxColumns = columns;
            Request req = new Request();
            req.setAppendDimension(adr);
            requests.add(req);
        }
        AppendCellsRequest acr = new AppendCellsRequest();
        acr.setFields("*");
        acr.setSheetId(0);
        acr.setRows(rows);

        Request request = new Request();
        request.setAppendCells(acr);
        requests.add(request);
        return requests;
    }

    public String getUrl() throws UnsupportedEncodingException {
        String urlString = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit#gid=0";
        return urlString;
    }
}
