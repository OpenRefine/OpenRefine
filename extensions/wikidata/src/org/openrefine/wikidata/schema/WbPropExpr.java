package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;


@JsonSubTypes({ 
    @Type(value = WbPropConstant.class, name = "wbpropconstant")
})
public abstract class WbPropExpr extends WbValueExpr {
    /* An expression that represents a property */
    
    public abstract PropertyIdValue evaluate(ExpressionContext ctxt);
    
    public static WbPropExpr fromJSON(JSONObject obj) throws JSONException {
        return WbPropConstant.fromJSON(obj);
    }

}
