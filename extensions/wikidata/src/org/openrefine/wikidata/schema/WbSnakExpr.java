package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikidata.utils.JacksonJsonizable;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbSnakExpr extends JacksonJsonizable {
    
    private WbValueExpr<? extends PropertyIdValue> prop;
    private WbValueExpr<? extends Value> value;
    
    @JsonCreator
    public WbSnakExpr(
            @JsonProperty("prop") WbValueExpr<? extends PropertyIdValue> propExpr,
            @JsonProperty("value") WbValueExpr<? extends Value> valueExpr) {
        this.prop = propExpr;
        this.value = valueExpr;
    }

    public Snak evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException {
        PropertyIdValue propertyId = getProp().evaluate(ctxt);
        Value evaluatedValue = value.evaluate(ctxt);
        return Datamodel.makeValueSnak(propertyId, evaluatedValue);
    }

    @JsonProperty("prop")
    public WbValueExpr<? extends PropertyIdValue> getProp() {
        return prop;
    }

    @JsonProperty("value")
    public WbValueExpr<? extends Value> getValue() {
        return value;
    }
}
