package com.metaweb.gridworks.gel.controls;

public class IsNull extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is null";
    }

    @Override
    protected boolean test(Object o) {
        return o == null;
    }
}
