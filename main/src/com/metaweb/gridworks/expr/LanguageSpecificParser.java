package com.metaweb.gridworks.expr;

public interface LanguageSpecificParser {
    public Evaluable parse(String s) throws ParsingException;
}
