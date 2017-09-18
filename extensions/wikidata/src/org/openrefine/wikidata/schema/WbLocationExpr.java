package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;


public abstract class WbLocationExpr extends WbValueExpr {
    @Override
    public abstract GlobeCoordinatesValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException;

    public static WbLocationExpr fromJSON(JSONObject obj) throws JSONException {
        String type = obj.getString(jsonTypeKey);
        if (WbLocationConstant.jsonType.equals(type)) {
            return WbLocationConstant.fromJSON(obj);
        } else if (WbLocationVariable.jsonType.equals(type)) {
            return WbLocationVariable.fromJSON(obj);
        } else {
            throw new JSONException("unknown type for WbLocationExpr");
        }
    }
}
