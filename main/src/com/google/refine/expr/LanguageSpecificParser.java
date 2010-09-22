package com.google.refine.expr;

public interface LanguageSpecificParser {
    public Evaluable parse(String s) throws ParsingException;
}
