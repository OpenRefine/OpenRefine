package org.openrefine.wikidata.schema;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;


public abstract class WbDateExpr extends WbValueExpr {

    @Override
    public abstract TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException;

    public static WbDateExpr fromJSON(JSONObject obj) throws JSONException {
        String type = obj.getString(jsonTypeKey);
        if (WbDateConstant.jsonType.equals(type)) {
            return WbDateConstant.fromJSON(obj);
        } else if (WbDateVariable.jsonType.equals(type)) {
            return WbDateVariable.fromJSON(obj);
        } else {
            throw new JSONException("unknown type for WbDateExpr");
        }
    }

}
