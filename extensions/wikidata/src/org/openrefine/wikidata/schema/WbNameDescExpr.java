package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;


public class WbNameDescExpr extends BiJsonizable {
    
    public final String jsonType = "wbnamedescexpr";
    
    enum NameDescrType {
        LABEL,
        DESCRIPTION,
        ALIAS,
    }
    
    private NameDescrType _type;
    private WbMonolingualExpr _value;
    
    public WbNameDescExpr(NameDescrType type, WbMonolingualExpr value) {
        _type = type;
        _value = value;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("name_type");
        writer.value(_type.name());
        writer.value("value");
        _value.write(writer, options);
    }
    
    public void contributeTo(ItemUpdate item, ExpressionContext ctxt) throws SkipStatementException {
        MonolingualTextValue val = _value.evaluate(ctxt);
        switch (_type) {
            case LABEL:
                item.addLabel(val);
                break;
            case DESCRIPTION:
                item.addDescription(val);
                break;
            case ALIAS:
                item.addAlias(val);
                break;
        }
    }

    @Override
    public String getJsonType() {
        return jsonType;
    }

    public static WbNameDescExpr fromJSON(JSONObject obj) throws JSONException {
        return new WbNameDescExpr(NameDescrType.valueOf((String) obj.get("name_type")),
                WbMonolingualExpr.fromJSON(obj.getJSONObject("value")));
    }

}
