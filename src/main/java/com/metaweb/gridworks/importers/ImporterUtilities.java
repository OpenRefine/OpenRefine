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
                double d = Double.parseDouble(text);
                if (!Double.isInfinite(d) && !Double.isNaN(d)) {
                    return d;
                }
            } catch (NumberFormatException e) {
            }
        }
        return text;
    }

}
