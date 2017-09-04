package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.openrefine.wikidata.schema.ExpressionContext;
import org.openrefine.wikidata.schema.WbValueExpr;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public abstract class WbItemExpr extends WbValueExpr {
    public abstract ItemIdValue evaluate(ExpressionContext ctxt) throws SkipStatementException;
    
    public static WbItemExpr fromJSON(JSONObject obj) throws JSONException {
        String type = obj.getString(jsonTypeKey);
        if (WbItemConstant.jsonType.equals(type)) {
            return WbItemConstant.fromJSON(obj);
        } else if (WbItemVariable.jsonType.equals(type)) {
            return WbItemVariable.fromJSON(obj);
        } else {
            throw new JSONException("unknown type for WbItemExpr");
        }
    }
}
