package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.implementation.StringValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class WbStringConstant implements WbValueExpr {
    
    private String value;
    
    public WbStringConstant(String value) {
        this.value = value;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("value");
        writer.value(value);
        writer.endObject();
    }

    @Override
    public Value evaluate(ExpressionContext ctxt) {
        return Datamodel.makeStringValue(value);
    }

}
