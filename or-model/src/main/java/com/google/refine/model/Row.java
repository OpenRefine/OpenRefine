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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.InjectableValues;
import com.google.refine.expr.CellTuple;
import com.google.refine.expr.HasFields;
import com.google.refine.util.ParsingUtilities;
import com.google.refine.util.Pool;

/**
 * Class representing a single Row which contains a list of {@link Cell}s.  There may
 * be multiple rows in a {@link Record}.
 */
public class Row implements HasFields {
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
    
    @JsonIgnore
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
    
    @JsonProperty(FLAGGED)
    public boolean isFlagged() {
        return flagged;
    }
    
    @JsonProperty(STARRED)
    public boolean isStarred() {
        return starred;
    }
    
    @JsonProperty("cells")
    public List<Cell> getCells() {
        return cells;
    }
    
    /*
    @JsonView(JsonViews.SaveMode.class)
    public 
    */
    
    public void save(Writer writer, Properties options) {
        if (options.containsKey("rowIndex")) {
            // See GetRowsCommand to serialize a row with indices
            throw new IllegalArgumentException("Serializing with row indices is not supported anymore.");
        }
        try {
            ParsingUtilities.saveWriter.writeValue(writer, this);
            Pool pool = (Pool)options.get("pool");
            if(pool != null) {
                for(Cell c : cells) {
                    if (c != null && c.recon != null) {
                        pool.pool(c.recon);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static public Row load(String s, Pool pool) throws Exception {
        return s.length() == 0 ? null : 
            loadStreaming(s, pool);
    }
    
    @JsonCreator
    static public Row deserialize(
            @JsonProperty(STARRED)
            boolean starred,
            @JsonProperty(FLAGGED)
            boolean flagged,
            @JsonProperty("cells")
            List<Cell> cells) {
        if (cells == null) {
            cells = new ArrayList<>();
        }
        return new Row(cells, flagged, starred);
    }
    
    static public Row loadStreaming(String s, Pool pool) throws Exception {
        InjectableValues injectableValues = new InjectableValues.Std()
                .addValue("pool", pool);
        return ParsingUtilities.mapper.setInjectableValues(injectableValues)
                .readValue(s, Row.class);
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
