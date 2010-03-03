package com.metaweb.gridworks.importers;

import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Row;

public class ImporterUtilities {

    static public Object parseCellValue(String text) {
        if (text.length() > 0) {
            if (text.length() > 1 && text.startsWith("\"") && text.endsWith("\"")) {
                return text.substring(1, text.length() - 1);
            }
            
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException e) {
            }
        
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
            }
        }
        return text;
    }

    static public boolean parseCSVIntoRow(Row row, String line) {
        boolean hasData = false;
        
        int start = 0;
        while (start < line.length()) {
            String text = null;
            
            if (line.charAt(start) == '"') {
                int next = line.indexOf('"', start + 1);
                if (next < 0) {
                    text = line.substring(start);
                    start = line.length();
                } else {
                    text = line.substring(start, next + 1);
                    start = next + 2;
                }
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
            
            Object value = parseCellValue(text);
            if (ExpressionUtils.isNonBlankData(value)) {
                row.cells.add(new Cell(value, null));
                hasData = true;
            } else {
                row.cells.add(null);
            }
        }
        
        return hasData;
    }

    static public boolean parseTSVIntoRow(Row row, String line) {
        boolean hasData = false;
        
        String[] cells = line.split("\t");
        for (int c = 0; c < cells.length; c++) {
            String text = cells[c];
            
            Object value = parseCellValue(text);
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
