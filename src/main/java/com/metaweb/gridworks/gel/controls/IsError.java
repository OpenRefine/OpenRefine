package com.metaweb.gridworks.gel.controls;

import com.metaweb.gridworks.expr.ExpressionUtils;

public class IsError extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is an error";
    }

    @Override
    protected boolean test(Object o) {
        return ExpressionUtils.isError(o);
    }
}
