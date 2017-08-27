package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.google.refine.Jsonizable;


public interface WbValueExpr extends Jsonizable {
    /* An expression that represents a Wikibase value,
     * i.e. anything that can be on the right-hand side
     * of a statement.
     */
    
    public Value evaluate(ExpressionContext ctxt);
    
}
