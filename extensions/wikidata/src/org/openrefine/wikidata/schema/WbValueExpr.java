package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public abstract class WbValueExpr extends BiJsonizable {
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
        String type = obj.getString(WbValueExpr.jsonTypeKey);
        WbValueExpr valueExpr = null;
        if (WbPropConstant.jsonType.equals(type)) {
            valueExpr = WbPropConstant.fromJSON(obj);
        } else if (WbItemConstant.jsonType.equals(type)) {
            valueExpr = WbItemConstant.fromJSON(obj);
        } else if (WbItemVariable.jsonType.equals(type)) {
            valueExpr = WbItemVariable.fromJSON(obj);
        } else if (WbStringVariable.jsonType.equals(type)) {
            valueExpr = WbStringVariable.fromJSON2(obj);
        } else if (WbStringConstant.jsonType.equals(type)) {
            valueExpr = WbStringConstant.fromJSON(obj);
        } else {
            throw new JSONException("unknown type '"+type+"' for WbValueExpr");
        }
        return valueExpr;
    }
}
