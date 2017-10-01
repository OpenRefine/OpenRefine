package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;


public class WbMonolingualExpr extends WbValueExpr {
    
    public static final String jsonType = "wbmonolingualexpr";
    
    protected WbLanguageExpr _languageExpr;
    protected WbStringExpr _valueExpr;
    
    public WbMonolingualExpr(WbLanguageExpr languageExpr, WbStringExpr valueExpr) {
        _languageExpr = languageExpr;
        _valueExpr = valueExpr;
    }

    @Override
    public MonolingualTextValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        return Datamodel.makeMonolingualTextValue(
                _valueExpr.evaluate(ctxt).getString(),
                _languageExpr.evaluate(ctxt));
    }

    public static WbMonolingualExpr fromJSON(JSONObject obj) throws JSONException {
        return new WbMonolingualExpr(
                WbLanguageExpr.fromJSON(obj.getJSONObject("language")),
                WbStringExpr.fromJSON(obj.getJSONObject("value")));
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("language");
        _languageExpr.write(writer, options);
        writer.key("value");
        _valueExpr.write(writer, options);
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

}
