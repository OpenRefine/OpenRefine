package com.metaweb.gridworks.importers.parsers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.importers.ImporterUtilities;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Row;

public class CSVRowParser extends RowParser {
    public List<String> split(String line) {
        List<String> results = new ArrayList<String>();
        
        int start = 0;
        while (start < line.length()) {
            String text = null;
            
            if (line.charAt(start) == '"') {
                StringBuffer sb = new StringBuffer();
                
                start++; // skip over "
                while (start < line.length()) {
                    int quote = line.indexOf('"', start);
                    if (quote < 0) {
                        sb.append(line.substring(start));
                        start = line.length();
                        break;
                    } else {
                        if (quote < line.length() - 1 && line.charAt(quote + 1) == '"') {
                            sb.append(line.substring(start, quote + 1)); // include " as well
                            start = quote + 2;
                        } else {
                            sb.append(line.substring(start, quote));
                            start = quote + 1;
                            if (start < line.length() && line.charAt(start) == ',') {
                                start++; // skip ,
                            }
                            break;
                        }
                    }
                }
                
                text = sb.toString();
            } else {
                int next = line.indexOf(',', start);
                if (next < 0) {
                    text = line.substring(start);
                    start = line.length();
                } else {
                    text = line.substring(start, next);
                    start = next + 1;
                }
            }
            
            results.add(text);
        }
        
        return results;
    }
    
    public boolean parseRow(Row row, String line, boolean guessValueType) {
        boolean hasData = false;
        
        List<String> strings = split(line);
        for (String s : strings) {
            Serializable value = guessValueType ? ImporterUtilities.parseCellValue(s) : s;
            
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
