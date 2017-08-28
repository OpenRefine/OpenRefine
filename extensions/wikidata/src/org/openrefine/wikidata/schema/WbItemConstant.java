package org.openrefine.wikidata.schema;

import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;


public class WbItemConstant extends WbItemExpr {
    /* Represents an item that does not vary,
     * it is independent of the row. */
    
    private String qid;
    private String label;
    
    public WbItemConstant(String qid, String label) {
        this.qid = qid;
        this.label = label;
    }

    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("qid");
        writer.value(qid);
        writer.key("label");
        writer.value(label);
    }
    
    public static WbItemConstant fromJSON(JSONObject obj) throws JSONException {
        return new WbItemConstant(obj.getString("qid"), obj.getString("label"));
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) {
        return ItemIdValueImpl.create(qid, ctxt.getBaseIRI());
    }

}
