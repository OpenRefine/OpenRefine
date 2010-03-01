package com.metaweb.gridworks.gel.controls;

import com.metaweb.gridworks.expr.ExpressionUtils;

public class IsBlank extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is null or an empty string";
    }

    @Override
    protected boolean test(Object o) {
        return !ExpressionUtils.isNonBlankData(o);
    }
}
