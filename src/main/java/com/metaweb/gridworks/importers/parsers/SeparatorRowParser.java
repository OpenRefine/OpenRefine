package com.metaweb.gridworks.importers.parsers;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.importers.ImporterUtilities;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Row;

public class SeparatorRowParser extends RowParser {

    String sep;
    
    public SeparatorRowParser(String sep) {
        this.sep = sep;
    }
    
    public boolean parseRow(Row row, String line) {
        boolean hasData = false;
        
        String[] cells = StringUtils.splitPreserveAllTokens(line, sep);
        for (int c = 0; c < cells.length; c++) {
            String text = cells[c];
            
            Serializable value = ImporterUtilities.parseCellValue(text);
            if (ExpressionUtils.isNonBlankData(value)) {
                row.cells.add(new Cell(value, null));
                hasData = true;
            } else {
                row.cells.add(null);
            }
        }
        return hasData;
    }    
    
}
