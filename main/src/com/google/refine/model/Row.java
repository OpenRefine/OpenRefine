/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package com.google.refine.model;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.Jsonizable;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.HasFields;
import com.google.refine.util.Pool;

/**
 * Class representing a single Row which contains a list of {@link Cell}s.  There may
 * be multiple rows in a {@link Record}.
 */
public class Row implements HasFields, Jsonizable {
    public boolean             flagged;
    public boolean             starred;
    final public List<Cell>    cells;
    
    private static final String FLAGGED = "flagged";
    private static final String STARRED = "starred";
    
    /**
     * Construct a new Row.
     * 
     * @param cellCount number of cells to give row initially (can be extended later)
     */
    public Row(int cellCount) {
        cells = new ArrayList<Cell>(cellCount);
    }
    
    protected Row(List<Cell> cells, boolean flagged, boolean starred) {
        this.cells = cells;
        this.flagged = flagged;
        this.starred = starred;
    }
    
    /**
     * Copy a row and return the copy. Note that this is a shallow copy, so
     * if the contents of cells are changed in the original, they will be
     * be changed in the duplicate.
     * @return the duplicated row
     */
    public Row dup() {
        Row row = new Row(cells.size());
        row.flagged = flagged;
        row.starred = starred;
        row.cells.addAll(cells);
        return row;
    }
    
    @Override
    public Object getField(String name, Properties bindings) {
        if (FLAGGED.equals(name)) {
            return flagged;
        } else if (STARRED.equals(name)) {
            return starred;
        }
        return null;
    }
    
    @Override
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
    
    /**
     * @param cellIndex index of cell to return
     * @return given cell or null if cell doesn't exist or cell index is out of range
     */
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
        return value == null || (value instanceof String && ((String) value).trim().length() == 0);
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
    
    @Override
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
            if (options.containsKey("rowIndex")) {
                int rowIndex = (Integer) options.get("rowIndex");
                writer.key("i"); writer.value(rowIndex);

                if (options.containsKey("recordIndex")) {
                    int recordIndex = (Integer) options.get("recordIndex");

                    writer.key("j"); writer.value(recordIndex);
                }
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
        
        return (cells.size() > 0) ? new Row(cells, flagged, starred) : new Row(0);
    }
    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Cell cell : cells) {
            result.append(cell == null ? "null" : cell.toString());
            result.append(',');
        }
        return result.toString();
    }
}
