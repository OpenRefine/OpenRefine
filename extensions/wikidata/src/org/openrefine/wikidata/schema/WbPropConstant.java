package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.entityvalues.SuggestedPropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A constant property, that does not change depending on the row
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbPropConstant implements WbExpression<PropertyIdValue> {
    
    private String pid;
    private String label;
    private String datatype;
    
    @JsonCreator
    public WbPropConstant(
            @JsonProperty("pid") String pid,
            @JsonProperty("label") String label,
            @JsonProperty("datatype") String datatype) {
        this.pid = pid;
        this.label = label;
        this.datatype = datatype;
    }

    @Override
    public PropertyIdValue evaluate(ExpressionContext ctxt) {
        return new SuggestedPropertyIdValue(pid, ctxt.getBaseIRI(), label);
    }
    
    @JsonProperty("pid")
    public String getPid() {
        return pid;
    }

    @JsonProperty("label")
    public String getLabel() {
        return label;
    }
    
    @JsonProperty("datatype")
    public String getDatatype() {
        return datatype;
    }
    
    @Override
    public boolean equals(Object other) {
        if(other == null || !WbPropConstant.class.isInstance(other)) {
            return false;
        }
        WbPropConstant otherConstant = (WbPropConstant)other;
        return pid.equals(otherConstant.getPid()) && label.equals(otherConstant.getLabel()) && datatype.equals(otherConstant.getDatatype());
    }

}
