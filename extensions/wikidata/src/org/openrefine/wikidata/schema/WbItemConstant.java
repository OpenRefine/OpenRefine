package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.entityvalues.SuggestedItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an item that does not vary,
 * it is independent of the row.
 */
public class WbItemConstant implements WbExpression<ItemIdValue> {
    
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
        return new SuggestedItemIdValue(
                qid,
                ctxt.getBaseIRI(),
                label);
    }

    @JsonProperty("qid")
    public String getQid() {
        return qid;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }
}
