package com.metaweb.gridworks.model.changes;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Recon;

public class CellAtRow {

    final public int    row;
    final public Cell   cell;
    
    public CellAtRow(int row, Cell cell) {
        this.row = row;
        this.cell = cell;
    }
    
    public void save(Writer writer, Properties options) throws IOException {
        writer.write(Integer.toString(row));
        writer.write(';');
        if (cell != null) {
            cell.save(writer, options);
        }
    }
    
    static public CellAtRow load(String s, Map<Long, Recon> reconCache) throws Exception {
        int semicolon = s.indexOf(';');
        int row = Integer.parseInt(s.substring(0, semicolon));
        Cell cell = semicolon < s.length() - 1 ? Cell.loadStreaming(s.substring(semicolon + 1), reconCache) : null;
        
        return new CellAtRow(row, cell);
    }
}
