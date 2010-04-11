package com.metaweb.gridworks.importers;

import java.io.Serializable;
import java.util.Properties;

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
    
    static public int getIntegerOption(String name, Properties options, int def) {
        int value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        return value;
    }
    
    static public boolean getBooleanOption(String name, Properties options, boolean def) {
        boolean value = def;
        if (options.containsKey(name)) {
            String s = options.getProperty(name);
            try {
                value = Boolean.parseBoolean(s);
            } catch (Exception e) {
            }
        }
        return value;
    }

}
