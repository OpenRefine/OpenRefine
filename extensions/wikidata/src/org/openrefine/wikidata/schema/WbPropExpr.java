package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;


public abstract class WbPropExpr extends WbValueExpr {
    /* An expression that represents a property */
    
    public abstract PropertyIdValue evaluate(ExpressionContext ctxt);
}
