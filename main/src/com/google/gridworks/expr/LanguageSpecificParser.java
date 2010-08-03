package com.google.gridworks.expr;

public interface LanguageSpecificParser {
    public Evaluable parse(String s) throws ParsingException;
}
