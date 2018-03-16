package com.google.refine.grel.controls;

public class IsEmptyString extends IsTest {
    @Override
    protected String getDescription() {
        return "Returns whether o is an empty string";
    }

    @Override
    protected boolean test(Object o) {
        return o != null && o.getClass().equals(String.class)
                && o.equals("");
    }
}
