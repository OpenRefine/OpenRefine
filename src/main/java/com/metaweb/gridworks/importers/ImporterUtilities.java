package com.metaweb.gridworks.importers;

import java.io.Serializable;

public class ImporterUtilities {

    static public Serializable parseCellValue(String text) {
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

}
