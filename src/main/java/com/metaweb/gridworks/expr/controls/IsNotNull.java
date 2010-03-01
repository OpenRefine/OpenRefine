package com.metaweb.gridworks.expr.controls;

public class IsNotNull extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is not null";
    }

    @Override
    protected boolean test(Object o) {
        return o != null;
    }
}
