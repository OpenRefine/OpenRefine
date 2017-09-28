package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public abstract class WbLanguageExpr extends BiJsonizable {
    /**
     * Evaluates the language expression to a Wikimedia language code
     * 
     * @param ctxt the evulation context
     * @return a Wikimedia language code
     * @throws SkipStatementException when the code is invalid
     */
    public abstract String evaluate(ExpressionContext ctxt) throws SkipStatementException;
    
    public static WbLanguageExpr fromJSON(JSONObject obj) throws JSONException {
        String type = obj.getString(jsonTypeKey);
        if (WbLanguageConstant.jsonType.equals(type)) {
            return WbLanguageConstant.fromJSON(obj);
        } else {
            throw new JSONException("unknown type for WbLocationExpr");
        }
    }
}
