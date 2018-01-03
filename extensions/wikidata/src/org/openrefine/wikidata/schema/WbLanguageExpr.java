package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.utils.JacksonJsonizable;

public abstract class WbLanguageExpr extends JacksonJsonizable {
    /**
     * Evaluates the language expression to a Wikimedia language code
     * 
     * @param ctxt the evaluation context
     * @return a Wikimedia language code
     * @throws SkipStatementException when the code is invalid
     */
    public abstract String evaluate(ExpressionContext ctxt) throws SkipStatementException;
}
