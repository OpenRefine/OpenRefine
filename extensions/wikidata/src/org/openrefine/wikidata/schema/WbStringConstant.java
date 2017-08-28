package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StringValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class WbStringConstant extends WbValueExpr {
    
    public static final String jsonType = "wbstringconstant";
    
    private String value;
    
    public WbStringConstant(String value) {
        this.value = value;
    }

    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("value");
        writer.value(value);
    }
    
    public static WbStringConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbStringConstant(obj.getString("value"));
    }

    @Override
    public Value evaluate(ExpressionContext ctxt) {
        return Datamodel.makeStringValue(value);
    }

}
