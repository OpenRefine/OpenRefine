package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;


public class WbItemConstant implements WbItemExpr {
    /* Represents an item that does not vary,
     * it is independent of the row. */
    
    private String qid;
    private String label;
    
    public WbItemConstant(String qid, String label) {
        this.qid = qid;
        this.label = label;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("qid");
        writer.value(qid);
        writer.key("label");
        writer.value(label);
        writer.endObject();
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) {
        return ItemIdValueImpl.create(qid, ctxt.getBaseIRI());
    }

}
