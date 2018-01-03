package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.utils.JacksonJsonizable;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbSnakExpr extends JacksonJsonizable {
    
    private WbPropExpr prop;
    private WbValueExpr value;
    
    @JsonCreator
    public WbSnakExpr(
            @JsonProperty("prop") WbPropExpr propExpr,
            @JsonProperty("value") WbValueExpr valueExpr) {
        this.prop = propExpr;
        this.value = valueExpr;
    }

    public Snak evaluate(ExpressionContext ctxt) throws SkipStatementException {
        PropertyIdValue propertyId = getProp().evaluate(ctxt);
        Value evaluatedValue = value.evaluate(ctxt);
        return Datamodel.makeValueSnak(propertyId, evaluatedValue);
    }

    public WbPropExpr getProp() {
        return prop;
    }

    public WbValueExpr getValue() {
        return value;
    }
}
