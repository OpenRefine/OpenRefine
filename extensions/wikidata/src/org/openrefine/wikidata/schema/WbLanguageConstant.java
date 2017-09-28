package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class WbLanguageConstant extends WbLanguageExpr {
    
    public static final String jsonType = "wblanguageconstant";
    
    protected String _langId;
    protected String _langLabel;
    
    public String evaluate(ExpressionContext ctxt) throws SkipStatementException {
        return _langId;
    }
    
    public WbLanguageConstant(String langId, String langLabel) {
        _langId = langId;
        _langLabel = langLabel;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("id");
        writer.value(_langId);
        writer.key("label");
        writer.value(_langLabel);
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }
    
    public static WbLanguageExpr fromJSON(JSONObject obj) throws JSONException {
        return new WbLanguageConstant(obj.getString("id"), obj.getString("label"));
    }
}
