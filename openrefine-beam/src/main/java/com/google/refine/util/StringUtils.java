package com.google.refine.util;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class StringUtils {

    /**
     * String formatting method that knows how to format dates (using the defaul locale's date formatter)
     * @param o object to be converted to a string
     * @return string representing object
     */
    public static String toString(Object o) {
        if (o instanceof Calendar || o instanceof Date) {
            DateFormat formatter = DateFormat.getDateInstance();
            return formatter.format(o instanceof Date ? ((Date) o) : ((Calendar) o).getTime());
        } else if (o == null) {
            return "null";
        } else {
            return o.toString();
        }
    }
}

