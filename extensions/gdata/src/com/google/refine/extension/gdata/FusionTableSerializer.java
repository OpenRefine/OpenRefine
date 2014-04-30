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
            sendBatch(true);
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
                if (!sendBatch(false)) {
                    return;
                }
            }
        }
    }
    
    private boolean sendBatch(boolean isLastChunk) {
        try {
            // FIXME: text/csv doesn't work even though that's what the content is
          AbstractInputStreamContent content = ByteArrayContent.fromString("application/octet-stream", sbBatch.toString());

//            AbstractInputStreamContent content = new InputStreamContent("application/octet-stream",
//                    // TODO: we really want to do GZIP compression here 
//                            new ByteArrayInputStream(sbBatch.toString().getBytes("UTF-8")));
            Long count = FusionTableHandler.insertRows(service, tableId, content);
            if (!isLastChunk && count != BATCH_SIZE) {
                // FIXME: this message should say numbers instead of %d but we'd need to know the batch number for this
                exceptions.add(new IOException("Only imported %d of %d rows"));
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
                sb.append(cellData.text.replaceAll("\"", "\\\\\""));
            }
            sb.append("\"");
        }
        sb.append("\n");
    }
    
    // Old-style SQL INSERT can be removed once we're sure importRows will work
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
