package com.metaweb.gridworks;

/**
 * Centralized configuration facility.
 */
public class Configurations {

    public static String get(String name) {
        return System.getProperty(name);
    }
    
    public static String get(String name, String def) {
        String val = get(name);
        return (val == null) ? def : val;
    }

    public static boolean getBoolean(String name, boolean def) {
        String val = get(name);
        return (val == null) ? def : Boolean.parseBoolean(val);
    }

    public static int getInteger(String name, int def) {
        String val = get(name);
        try {
            return (val == null) ? def : Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as an integer number.");
        }
    }

    public static float getFloat(String name, float def) {
        String val = get(name);
        try {
            return (val == null) ? def : Float.parseFloat(val);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Could not parse '" + val + "' as a floating point number.");
        }
    }
    
}
