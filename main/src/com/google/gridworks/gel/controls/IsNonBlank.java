package com.google.gridworks.gel.controls;

import com.google.gridworks.expr.ExpressionUtils;

public class IsNonBlank extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is not null and not an empty string";
    }

    @Override
    protected boolean test(Object o) {
        return ExpressionUtils.isNonBlankData(o);
    }
}
