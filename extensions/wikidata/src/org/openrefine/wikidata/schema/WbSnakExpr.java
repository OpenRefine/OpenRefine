package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.google.refine.Jsonizable;


public class WbSnakExpr implements Jsonizable {
    
    private WbPropExpr propExpr;
    private WbValueExpr valueExpr;
    
    public WbSnakExpr(WbPropExpr propExpr, WbValueExpr valueExpr) {
        this.propExpr = propExpr;
        this.valueExpr = valueExpr;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("prop");
        propExpr.write(writer, options);
        writer.key("value");
        valueExpr.write(writer, options);
        writer.endObject();
    }
    
    public static WbSnakExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject propObj = obj.getJSONObject("prop");
        WbPropExpr propExpr = WbPropConstant.fromJSON(propObj);
        JSONObject valueObj = obj.getJSONObject("value");
        WbValueExpr valueExpr = WbValueExpr.fromJSON(valueObj);
        return new WbSnakExpr(propExpr, valueExpr);
    }
   
    public Snak evaluate(ExpressionContext ctxt) {
        PropertyIdValue propertyId = propExpr.evaluate(ctxt);
        Value value = valueExpr.evaluate(ctxt);
        return Datamodel.makeValueSnak(propertyId, value);
    }

}
