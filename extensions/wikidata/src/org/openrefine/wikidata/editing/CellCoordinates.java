package org.openrefine.wikidata.editing;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * A class to facilitate serialization of 
 * the map from cell positions to qids
 * 
 * @author antonin
 *
 */
public class CellCoordinates {
    public int row;
    public int col;
    
    public CellCoordinates(int row, int col) {
        this.row = row;
        this.col = col;
    }
    
    public CellCoordinates(String serialized) {
        String[] coords = serialized.split("_");
        this.row = Integer.parseInt(coords[0]);
        this.col = Integer.parseInt(coords[1]);
    }
    
    @JsonIgnore
    public String toString() {
        return String.format("%d_%d", row, col);
    }
}

