package com.google.refine.util;

import java.time.OffsetDateTime;

public class StringUtils {
    /**
     * String formatting method that knows how to format dates (using the default locale's date formatter)
     * @param o object to be converted to a string
     * @return string representing object
     */
    public static String toString(Object o) {
        // to replace the DateFormat with java.time.format.DateTimeFormatter 
        if (o instanceof OffsetDateTime) {
            OffsetDateTime odt = (OffsetDateTime)o;
            return ParsingUtilities.dateToString((OffsetDateTime) odt);
        } else if (o == null) {
            return "";
        } else {
            return o.toString();
        }
    }
}
