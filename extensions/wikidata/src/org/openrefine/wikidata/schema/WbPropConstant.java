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
    
    public WbPropConstant(String pid) {
        this.pid = pid;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("pid");
        writer.value(pid);
    }
    
    public static WbPropConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbPropConstant(obj.getString("pid"));
    }

    @Override
    public PropertyIdValue evaluate(ExpressionContext ctxt) {
        return PropertyIdValueImpl.create(pid, ctxt.getBaseIRI());
    }
}
