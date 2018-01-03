package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.openrefine.wikidata.utils.JacksonJsonizable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;


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
    @Type(value = WbDateVariable.class, name = "wbdatevariable") ,
  })
public abstract class WbValueExpr extends JacksonJsonizable {
    /* An expression that represents a Wikibase value,
     * i.e. anything that can be on the right-hand side
     * of a statement.
     */
    
    /*
     * Evaluates the value expression in a given context,
     * returns a wikibase value suitable to be the target of a claim.
     */
    public abstract Value evaluate(ExpressionContext ctxt) throws SkipStatementException;
    
    public static WbValueExpr fromJSON(JSONObject obj) throws JSONException {
        return fromJSONClass(obj, WbValueExpr.class);
    }
}
