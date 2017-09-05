package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public class WbPropConstant extends WbPropExpr {
    /* A constant property, that does not change depending on the row */
    
    public static final String jsonType = "wbpropconstant";
    
    private String pid;
    private String label;
    private String datatype;
    
    public WbPropConstant(String pid, String label, String datatype) {
        this.pid = pid;
        this.label = label;
        this.datatype = datatype;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("pid");
        writer.value(pid);
        writer.key("label");
        writer.value(label);
        writer.key("datatype");
        writer.value(datatype);
    }
    
    public static WbPropConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbPropConstant(
                obj.getString("pid"),
                obj.getString("label"),
                obj.getString("datatype"));
    }

    @Override
    public PropertyIdValue evaluate(ExpressionContext ctxt) {
        return PropertyIdValueImpl.create(pid, ctxt.getBaseIRI());
    }
    
    public String getJsonType() {
        return jsonType;
    }
}
