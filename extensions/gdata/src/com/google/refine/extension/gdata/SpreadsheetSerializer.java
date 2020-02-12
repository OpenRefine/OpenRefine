package com.google.refine.extension.gdata;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AppendCellsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.refine.exporters.TabularSerializer;

final class SpreadsheetSerializer implements TabularSerializer {
    static final Logger logger = LoggerFactory.getLogger("SpreadsheetSerializer");
    
    private static final int BATCH_SIZE = 1000;
    
    private Sheets service;
    private String spreadsheetId;
    private List<Exception> exceptions;
    
    // A list of updates to apply to the spreadsheet.
    private List<Request> requests = new ArrayList<>(); 
    
    private Request batchRequest = null;
    private int row = 0;

    private List<RowData> rows;
    
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
        if (batchRequest != null) {
            sendBatch(rows);
        }
        
        BatchUpdateSpreadsheetRequest requestBody = new BatchUpdateSpreadsheetRequest();
        requestBody.setIncludeSpreadsheetInResponse(false);
        requestBody.setRequests(requests);

        Sheets.Spreadsheets.BatchUpdate request;
        try {
            logger.debug("spreadsheetId: " + spreadsheetId);
            logger.debug("requestBody:" + requestBody.toString());
            request = service.spreadsheets().batchUpdate(spreadsheetId, requestBody);
            
            BatchUpdateSpreadsheetResponse response = request.execute();
            logger.debug("response:" + response.toPrettyString());
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    @Override
    public void addRow(List<CellData> cells, boolean isHeader) {
        if (batchRequest == null) {
            batchRequest = new Request();
            rows = new ArrayList<RowData>(BATCH_SIZE);
        }
        List<com.google.api.services.sheets.v4.model.CellData> cellDatas = new ArrayList<>();
        RowData rowData = new RowData();
        
        for (int c = 0; c < cells.size(); c++) {
            CellData cellData = cells.get(c);
            if (cellData != null && cellData.text != null) {
                cellDatas.add(cellData2sheetCellData(cellData));
            }
        }
        
        rowData.setValues(cellDatas);
        rows.add(rowData);
        row++;
        
        if (row % BATCH_SIZE == 0) {
            sendBatch(rows);
        }
    }
    
    private com.google.api.services.sheets.v4.model.CellData cellData2sheetCellData(CellData cellData) {
        com.google.api.services.sheets.v4.model.CellData sheetCellData = new com.google.api.services.sheets.v4.model.CellData();
        
        ExtendedValue ev = new ExtendedValue();
        ev.setStringValue(cellData.value.toString());
        
        sheetCellData.setUserEnteredValue(ev);
        
        return sheetCellData;
    }
    
    private void sendBatch(List<RowData> rows) {
        AppendCellsRequest acr = new AppendCellsRequest();
        acr.setFields("*");
        acr.setSheetId(0);
        acr.setRows(rows);
        batchRequest.setAppendCells(acr);
        
        requests.add(batchRequest);
    }
    
    public String getUrl() throws UnsupportedEncodingException {
        String urlString = "https://docs.google.com/spreadsheets/d/" + spreadsheetId + "/edit#gid=0";
        return urlString;
    }
}
