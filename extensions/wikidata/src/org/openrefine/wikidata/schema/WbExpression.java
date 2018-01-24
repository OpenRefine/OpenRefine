package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

/**
 * The base interface for all expressions, which evaluate to a
 * particular type T in an ExpressionContext.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,
      include=JsonTypeInfo.As.PROPERTY,
      property="type")
@JsonSubTypes({ 
    @Type(value = WbStringConstant.class, name = "wbstringconstant"), 
    @Type(value = WbStringVariable.class, name = "wbstringvariable"),
    @Type(value = WbLocationConstant.class, name = "wblocationconstant"), 
    @Type(value = WbLocationVariable.class, name = "wblocationvariable"),
    @Type(value = WbItemConstant.class, name = "wbitemconstant"), 
    @Type(value = WbItemVariable.class, name = "wbitemvariable"),
    @Type(value = WbLanguageConstant.class, name = "wblanguageconstant"), 
    @Type(value = WbLanguageVariable.class, name = "wblanguagevariable"),
    @Type(value = WbDateConstant.class, name = "wbdateconstant"), 
    @Type(value = WbDateVariable.class, name = "wbdatevariable"),
    @Type(value = WbMonolingualExpr.class, name = "wbmonolingualexpr"),
    @Type(value = WbPropConstant.class, name = "wbpropconstant"),
    @Type(value = WbLanguageConstant.class, name = "wblanguageconstant"), 
    @Type(value = WbLanguageVariable.class, name = "wblanguagevariable"),
  })
public interface  WbExpression<T>  {

    /**
     * Evaluates the value expression in a given context,
     * returns a Wikibase value suitable to be the target of a claim.
     */
    public T evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException;
}
