package org.openrefine.wikidata.schema;


import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public abstract class WbStringExpr extends WbValueExpr {
    public abstract StringValue evaluate(ExpressionContext ctxt) throws SkipStatementException;
    
    public static WbStringExpr fromJSON(JSONObject obj) throws JSONException {
        String type = obj.getString(jsonTypeKey);
        if (WbStringConstant.jsonType.equals(type)) {
            return WbStringConstant.fromJSON(obj);
        } else if (WbStringVariable.jsonType.equals(type)) {
            return WbStringVariable.fromJSON2(obj);
        } else {
            throw new JSONException("unknown type for WbItemExpr");
        }
    }
}
