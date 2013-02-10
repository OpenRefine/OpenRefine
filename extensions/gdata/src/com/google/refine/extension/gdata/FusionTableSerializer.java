package com.google.refine.extension.gdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.gdata.client.GoogleService;
import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.Service.GDataRequest.RequestType;
import com.google.gdata.util.ServiceException;

import com.google.refine.exporters.TabularSerializer;

final class FusionTableSerializer implements TabularSerializer {
    GoogleService service;
    String tableName;
    List<Exception> exceptions;
    
    String tableId;
    List<String> columnNames;
    StringBuffer sbBatch;
    int rows;
    
    FusionTableSerializer(GoogleService service, String tableName, List<Exception> exceptions) {
        this.service = service;
        this.tableName = tableName;
        this.exceptions = exceptions;
    }
    
    @Override
    public void startFile(JSONObject options) {
    }

    @Override
    public void endFile() {
        if (sbBatch != null) {
            sendBatch();
        }
    }

    @Override
    public void addRow(List<CellData> cells, boolean isHeader) {
        if (isHeader) {
            columnNames = new ArrayList<String>(cells.size());
            
            StringBuffer sb = new StringBuffer();
            sb.append("CREATE TABLE '");
            sb.append(tableName);
            sb.append("' (");
            boolean first = true;
            for (CellData cellData : cells) {
                columnNames.add(cellData.text);
                
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append("'");
                sb.append(cellData.text);
                sb.append("': STRING");
            }
            sb.append(")");
            
            try {
                String createQuery = sb.toString();
                
                GDataRequest createTableRequest = FusionTableHandler.createFusionTablesPostRequest(
                        service, RequestType.INSERT, createQuery);
                createTableRequest.execute();
                
                List<List<String>> createTableResults =
                        FusionTableHandler.parseFusionTablesResults(createTableRequest);
                if (createTableResults != null && createTableResults.size() == 2) {
                    tableId = createTableResults.get(1).get(0);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        } else if (tableId != null) {
            if (sbBatch == null) {
                sbBatch = new StringBuffer();
            }
            formulateInsert(cells, sbBatch);
            
            rows++;
            if (rows % 20 == 0) {
                sendBatch();
            }
        }
    }
    
    private void sendBatch() {
        try {
            GDataRequest createTableRequest = FusionTableHandler.createFusionTablesPostRequest(
                    service, RequestType.INSERT, sbBatch.toString());
            createTableRequest.execute();
        } catch (IOException e) {
            exceptions.add(e);
        } catch (ServiceException e) {
            exceptions.add(e);
        } finally {
            sbBatch = null;
        }
    }
    
    private void formulateInsert(List<CellData> cells, StringBuffer sb) {
        StringBuffer sbColumnNames = new StringBuffer();
        StringBuffer sbValues = new StringBuffer();
        boolean first = true;
        for (int i = 0; i < cells.size() && i < columnNames.size(); i++) {
            CellData cellData = cells.get(i);
            if (first) {
                first = false;
            } else {
                sbColumnNames.append(',');
                sbValues.append(',');
            }
            sbColumnNames.append("'");
            sbColumnNames.append(columnNames.get(i));
            sbColumnNames.append("'");
            
            sbValues.append("'");
            if (cellData != null && cellData.text != null) {
                sbValues.append(cellData.text.replaceAll("'", "\\\\'"));
            }
            sbValues.append("'");
        }
        
        if (sb.length() > 0) {
            sb.append(';');
        }
        sb.append("INSERT INTO ");
        sb.append(tableId);
        sb.append("(");
        sb.append(sbColumnNames.toString());
        sb.append(") values (");
        sb.append(sbValues.toString());
        sb.append(")");
    }
    
    public String getUrl() {
        return tableId == null || exceptions.size() > 0 ? null :
            "https://www.google.com/fusiontables/DataSource?docid=" + tableId;
    }
}