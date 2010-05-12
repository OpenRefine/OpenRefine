package com.metaweb.gridworks.importers.parsers;

import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.importers.ImporterUtilities;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Row;

public class NonSplitRowParser extends RowParser {

    public List<String> split(String line, LineNumberReader lineReader) {
        List<String> results = new ArrayList<String>(1);
        
        results.add(line.trim());
        
        return results;
    }
    
    public boolean parseRow(Row row, String line, boolean guessValueType, LineNumberReader lineReader) {
        line = line.trim();
        if (line.isEmpty()) {
            return false;
        } else {
            Serializable value = guessValueType ? ImporterUtilities.parseCellValue(line) : line;
            if (ExpressionUtils.isNonBlankData(value)) {
                row.cells.add(new Cell(value, null));
                return true;
            } else {
                row.cells.add(null);
                return false;
            }
        }
    }    
    
}
