package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.implementation.ItemIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbItemConstant extends WbItemExpr {
    /* Represents an item that does not vary,
     * it is independent of the row. */
    
    private String qid;
    private String label;
    
    @JsonCreator
    public WbItemConstant(
            @JsonProperty("qid") String qid,
            @JsonProperty("label") String label) {
        this.qid = qid;
        this.label = label;
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) {
        return ItemIdValueImpl.create(qid, ctxt.getBaseIRI());
    }

    public String getQid() {
        return qid;
    }

    public String getLabel() {
        return label;
    }
}
