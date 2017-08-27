package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public class WbPropConstant implements WbPropExpr {
    /* A constant property, that does not change depending on the row */
    
    private String pid;
    
    public WbPropConstant(String pid) {
        this.pid = pid;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("pid");
        writer.value(pid);
        writer.endObject();
    }

    @Override
    public PropertyIdValue evaluate(ExpressionContext ctxt) {
        return PropertyIdValueImpl.create(pid, ctxt.getBaseIRI());
    }
}
