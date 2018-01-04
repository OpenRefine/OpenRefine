package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.utils.JacksonJsonizable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,
              include=JsonTypeInfo.As.PROPERTY,
              property="type")
@JsonSubTypes({ 
    @Type(value = WbLanguageConstant.class, name = "wblanguageconstant"), 
    @Type(value = WbLanguageVariable.class, name = "wblanguagevariable")
})
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
