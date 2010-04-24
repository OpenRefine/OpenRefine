package com.metaweb.gridworks.model;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.CellTuple;
import com.metaweb.gridworks.expr.HasFields;
import com.metaweb.gridworks.util.Pool;

public class Row implements HasFields, Jsonizable {
    public boolean             flagged;
    public boolean             starred;
    final public List<Cell>    cells;
    
    transient public int            recordIndex = -1; // -1 for rows that are not main record rows
    transient public List<Integer>  contextRows;
    transient public int[]          contextRowSlots;
    transient public int[]          contextCellSlots;
    
    private static final String FLAGGED = "flagged";
    private static final String STARRED = "starred";
    
    public Row(int cellCount) {
        cells = new ArrayList<Cell>(cellCount);
    }
    
    protected Row(List<Cell> cells, boolean flagged, boolean starred) {
        this.cells = cells;
        this.flagged = flagged;
        this.starred = starred;
    }
    
    public Row dup() {
        Row row = new Row(cells.size());
        row.flagged = flagged;
        row.starred = starred;
        row.cells.addAll(cells);
        return row;
    }
    
    public Object getField(String name, Properties bindings) {
        if (FLAGGED.equals(name)) {
            return flagged;
        } else if (STARRED.equals(name)) {
            return starred;
        }
        return null;
    }
    
    public boolean fieldAlsoHasFields(String name) {
        return "cells".equals(name) || "record".equals(name);
    }
    
    public boolean isEmpty() {
        for (Cell cell : cells) {
            if (cell != null && cell.value != null && !isValueBlank(cell.value)) {
                return false;
            }
        }
        return true;
    }
    
    public Cell getCell(int cellIndex) {
        if (cellIndex >= 0 && cellIndex < cells.size()) {
            return cells.get(cellIndex);
        } else {
            return null;
        }
    }
    
    public Object getCellValue(int cellIndex) {
        if (cellIndex >= 0 && cellIndex < cells.size()) {
            Cell cell = cells.get(cellIndex);
            if (cell != null) {
                return cell.value;
            }
        }
        return null;
    }
    
    public boolean isCellBlank(int cellIndex) {
        return isValueBlank(getCellValue(cellIndex));
    }
    
    protected boolean isValueBlank(Object value) {
        return value == null || !(value instanceof String) || ((String) value).trim().length() == 0;
    }
    
    public void setCell(int cellIndex, Cell cell) {
        if (cellIndex < cells.size()) {
            cells.set(cellIndex, cell);
        } else {
            while (cellIndex > cells.size()) {
                cells.add(null);
            }
            cells.add(cell);
        }
    }
    
    public CellTuple getCellTuple(Project project) {
        return new CellTuple(project, this);
    }
    
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.object();
        writer.key(FLAGGED); writer.value(flagged);
        writer.key(STARRED); writer.value(starred);
        
        writer.key("cells"); writer.array();
        for (Cell cell : cells) {
            if (cell != null) {
                cell.write(writer, options);
            } else {
                writer.value(null);
            }
        }
        writer.endArray();
        
        if (!"save".equals(options.getProperty("mode"))) {
            if (recordIndex >= 0) {
                writer.key("j"); writer.value(recordIndex);
            }
            
            if (options.containsKey("rowIndex")) {
                writer.key("i"); writer.value(options.get("rowIndex"));
            }
            if (options.containsKey("extra")) {
                Properties extra = (Properties) options.get("extra");
                if (extra != null) {
                    for (Entry<Object,Object> e : extra.entrySet()) {
                        writer.key((String) e.getKey());
                        writer.value(e.getValue());
                    }
                }
            }
        }
        
        writer.endObject();
    }
    
    public void save(Writer writer, Properties options) {
        JSONWriter jsonWriter = new JSONWriter(writer);
        try {
            write(jsonWriter, options);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    static public Row load(String s, Pool pool) throws Exception {
        return s.length() == 0 ? null : 
            loadStreaming(s, pool);
    }
    
    static public Row loadStreaming(String s, Pool pool) throws Exception {
        JsonFactory jsonFactory = new JsonFactory(); 
        JsonParser jp = jsonFactory.createJsonParser(s);
        
        if (jp.nextToken() != JsonToken.START_OBJECT) {
            return null;
        }
        
        List<Cell>  cells = new ArrayList<Cell>();
        boolean     starred = false;
        boolean     flagged = false;
        
        while (jp.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jp.getCurrentName();
            jp.nextToken();
            
            if (STARRED.equals(fieldName)) {
                starred = jp.getBooleanValue();
            } else if (FLAGGED.equals(fieldName)) {
                flagged = jp.getBooleanValue();
            } else if ("cells".equals(fieldName)) {
                if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
                    return null;
                }
                
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    Cell cell = Cell.loadStreaming(jp, pool);
                    
                    cells.add(cell);
                }
            }
        }
        
        return (cells.size() > 0) ? new Row(cells, flagged, starred) : null;
    }
}
