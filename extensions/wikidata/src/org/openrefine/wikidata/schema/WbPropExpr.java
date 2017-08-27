package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public interface WbPropExpr extends WbValueExpr {
    /* An expression that represents a property */
    
    public PropertyIdValue evaluate(ExpressionContext ctxt);
    
}
