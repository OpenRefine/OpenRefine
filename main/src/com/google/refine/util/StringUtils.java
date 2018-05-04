package com.google.refine.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class StringUtils {

    /**
     * String formatting method that knows how to format dates (using the defaul locale's date formatter)
     * @param o object to be converted to a string
     * @return string representing object
     */
    public static String toString(Object o) {
        // to replace the DateFormat with java.time.format.DateTimeFormatter 
        if (o instanceof OffsetDateTime) {
            OffsetDateTime odt = (OffsetDateTime)o;
            return odt.format(DateTimeFormatter.ISO_INSTANT);
        } else if (o == null) {
            return "null";
        } else {
            return o.toString();
        }
    }
}

