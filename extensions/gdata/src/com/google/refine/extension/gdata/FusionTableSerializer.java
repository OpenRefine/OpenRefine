package com.google.refine.extension.gdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.fusiontables.Fusiontables;

import com.google.refine.exporters.TabularSerializer;

final class FusionTableSerializer implements TabularSerializer {
    private static final int BATCH_SIZE = 20;
    Fusiontables service;
    String tableName;
    List<Exception> exceptions;
    
    String tableId;
    List<String> columnNames;
    StringBuffer sbBatch;
    int rows;
    
    FusionTableSerializer(Fusiontables service, String tableName, List<Exception> exceptions) {
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
            sendBatch(rows % BATCH_SIZE);
        }
    }

    @Override
    public void addRow(List<CellData> cells, boolean isHeader) {
        if (isHeader) {
            columnNames = new ArrayList<String>(cells.size());
            for (CellData cellData : cells) {
                columnNames.add(cellData.text);
            }                
            try {
                tableId = FusionTableHandler.createTable(service, tableName, columnNames);
            } catch (Exception e) {
                tableId = null;
                exceptions.add(e);
            }
        } else if (tableId != null) {
            if (sbBatch == null) {
                sbBatch = new StringBuffer();
            }
            formatCsv(cells, sbBatch);            
            rows++;
            if (rows % BATCH_SIZE == 0) {
                if (!sendBatch(BATCH_SIZE)) {
                    return;
                }
            }
        }
    }
    
    private boolean sendBatch(int batchSize) {
        try {
            // TODO: we really want to do GZIP compression here 
            // FIXME: text/csv doesn't work even though that's what the content is
            AbstractInputStreamContent content = ByteArrayContent.fromString("application/octet-stream", sbBatch.toString());
            Long count = FusionTableHandler.insertRows(service, tableId, content);
            if (count != null && count.intValue() != batchSize) {
                exceptions.add(new IOException("only imported " + count + " of " + batchSize + " rows"));
            }
        } catch (IOException e) {
            exceptions.add(e);
            if (e instanceof HttpResponseException) {
                int code = ((HttpResponseException)e).getStatusCode();
                if (code >= 400 && code < 500) {
                    return false;
                }
                // 500s appear to be retried automatically by li
            }
        } finally {
            sbBatch = null;
        }
        return true;
    }

    private void formatCsv(List<CellData> cells, StringBuffer sb) {
        boolean first = true;
        for (int i = 0; i < cells.size() && i < columnNames.size(); i++) {
            CellData cellData = cells.get(i);
            if (!first) {
                sb.append(',');
            } else {
                first = false;
            }
            sb.append("\"");
            if (cellData != null && cellData.text != null) {
                sb.append(cellData.text.replaceAll("\"", "\"\""));
            }
            sb.append("\"");
        }
        sb.append("\n");
    }
    
    public String getUrl() {
        return tableId == null || exceptions.size() > 0 ? null :
            "https://www.google.com/fusiontables/DataSource?docid=" + tableId;
    }
}
