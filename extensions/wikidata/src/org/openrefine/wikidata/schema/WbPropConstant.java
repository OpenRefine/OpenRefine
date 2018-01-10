package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.entityvalues.TermedPropertyIdValue;
import org.wikidata.wdtk.datamodel.implementation.PropertyIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbPropConstant extends WbPropExpr {
    /* A constant property, that does not change depending on the row */
    
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
        return new TermedPropertyIdValue(pid, ctxt.getBaseIRI(), label);
    }
    
    public String getPid() {
        return pid;
    }

    
    public String getLabel() {
        return label;
    }
    
    public String getDatatype() {
        return datatype;
    }

}
