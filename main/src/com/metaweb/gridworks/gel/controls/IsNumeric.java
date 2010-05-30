package com.metaweb.gridworks.gel.controls;

import org.apache.commons.lang.StringUtils;

public class IsNumeric extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o can represent a number";
    }

    @Override
    protected boolean test(Object o) {
        if (o instanceof Number) return true;
        
        String s = (o instanceof String) ? (String) o : o.toString();
        
        return StringUtils.isNumeric(s);
    }
}
