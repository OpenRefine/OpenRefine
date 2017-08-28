package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.Value;


public abstract class WbValueExpr extends BiJsonizable {
    /* An expression that represents a Wikibase value,
     * i.e. anything that can be on the right-hand side
     * of a statement.
     */
    
 
    /*
     * Evaluates the value expression in a given context,
     * returns a wikibase value suitable to be the target of a claim.
     */
    public abstract Value evaluate(ExpressionContext ctxt);
}
