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

package org.openrefine.model;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.openrefine.expr.CellTuple;
import org.openrefine.expr.HasFields;
import org.openrefine.util.ParsingUtilities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * Class representing a single Row which contains a list of {@link Cell}s.  There may
 * be multiple rows in a {@link Record}.
 */
public class Row implements HasFields, Serializable {
	private static final long serialVersionUID = -6999837276078747023L;
	public final boolean             flagged;
    public final boolean             starred;
    public final ImmutableList<Cell>    cells;
    
    private static final String FLAGGED = "flagged";
    private static final String STARRED = "starred";
    
    /**
     * Construct a new Row containing the given cells.
     */
    public Row(List<Cell> cells) {
        this(cells, false, false);
    }
    
    @JsonCreator
    public Row(
            @JsonProperty("cells")
            List<Cell> cells,
            @JsonProperty("flagged")
            boolean flagged,
            @JsonProperty("starred")
            boolean starred) {
        this(cells != null ?
                ImmutableList.<Cell>builder().addAll(
                        cells.stream().map(c -> c != null ? c : Cell.NULL).collect(Collectors.toList())
                ).build() : ImmutableList.of(),
                flagged, starred);
    }
    
    public Row(int nbCells) {
        this(Collections.nCopies(nbCells, null), false, false);
    }
    
    protected Row(ImmutableList<Cell> cells, boolean flagged, boolean starred) {
        this.cells = cells;
        this.flagged = flagged;
        this.starred = starred;
    }
    
    @Override
    public Object getField(String name) {
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
            Cell cell = cells.get(cellIndex);
            if (cell != null && cell.value != null) {
                return cell;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
    
    public Serializable getCellValue(int cellIndex) {
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
    
    public CellTuple getCellTuple(ColumnModel columnModel) {
        return new CellTuple(columnModel, this);
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
        return cells.stream()
                .map(c -> c.value == null && c.recon == null ? null : c)
                .collect(Collectors.toList());
    }
    
    /**
     * Overwrite a cell at a given index. As rows are 
     * immutable this returns a new row.
     * 
     * @param index the position of the cell to overwrite
     * @param cell the cell value
     * @return the modified row
     */
    public Row withCell(int index, Cell cell) {
        List<Cell> newCells = new ArrayList<>(cells);
        newCells.set(index, cell);
        return new Row(newCells, flagged, starred);
    }
    
    /**
     * Inserts a cell at a given index. As rows are
     * immutable this returns a new row.
     * 
     * @param index the position where to insert the cell
     * @param cell the cell to insert
     * @return the new row
     */
    public Row insertCell(int index, Cell cell) {
        List<Cell> newCells = new ArrayList<>(cells.size() + 1);
        newCells.addAll(cells.subList(0, index));
        newCells.add(cell);
        newCells.addAll(cells.subList(index, cells.size()));
        return new Row(newCells, flagged, starred);
    }
    
    /**
     * Insest multiple cells contiguously, starting at a given index
     * @param index the index of the first inserted cell
     * @param cells the cells to insert
     * @return
     */
    public Row insertCells(int index, List<Cell> cells) {
        List<Cell> newCells = new ArrayList<>(this.cells.size() + cells.size());
        newCells.addAll(this.cells.subList(0, index));
        newCells.addAll(cells);
        newCells.addAll(this.cells.subList(index, this.cells.size()));
        return new Row(newCells, flagged, starred);
    }
    
    /**
     * Removes a cell from the row (removing the corresponding column).
     * 
     * @param index the index of the cell to remove
     * @return the new row without the cell
     */
    public Row removeCell(int index) {
        List<Cell> newCells = new ArrayList<>(cells.size() - 1);
        newCells.addAll(cells.subList(0, index));
        newCells.addAll(cells.subList(index+1, cells.size()));
        return new Row(newCells, flagged, starred);
    }
    
    /**
     * Changes the flag on the row.
     * 
     * @param newFlagged
     * @return
     */
    public Row withFlagged(boolean newFlagged) {
        return new Row(cells, newFlagged, starred);
    }
    
    /**
     * Changes the star on the row.
     * 
     * @param newStarred
     * @return
     */
    public Row withStarred(boolean newStarred) {
        return new Row(cells, flagged, newStarred);
    }
    
    /**
     * Returns a copy of this row with null cells added at the end,
     * such that the new row has the supplied size.
     * 
     * @param finalSize the size of the returned row
     * @return
     */
    public Row padWithNull(int finalSize) {
        if (cells.size() == finalSize) {
            return this;
        } else if (cells.size() > finalSize) {
            throw new IllegalArgumentException("Desired row size is smaller than the actual row size: impossible to pad with null cells");
        } else {
            return insertCells(cells.size(), Arrays.asList(new Cell[finalSize - cells.size()]));
        }
    }
    
    public void save(Writer writer, Properties options) {
        if (options.containsKey("rowIndex")) {
            // See GetRowsCommand to serialize a row with indices
            throw new IllegalArgumentException("Serializing with row indices is not supported anymore.");
        }
        try {
            ParsingUtilities.saveWriter.writeValue(writer, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static public Row load(String s) throws Exception {
        return s.length() == 0 ? null : 
            loadStreaming(s);
    }
    
    static public Row loadStreaming(String s) throws Exception {
        return ParsingUtilities.mapper.readValue(s, Row.class);
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
    
    @Override
    public boolean equals(Object other) {
    	if(!(other instanceof Row)) {
    		return false;
    	}
    	Row otherRow = (Row)other;
    	return (otherRow.cells.equals(cells) &&
    			flagged == otherRow.flagged &&
    			starred == otherRow.starred);
    }


}
