package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.wikidata.utils.JacksonJsonizable;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbStatementGroupExpr extends JacksonJsonizable {
    
    private WbPropExpr propertyExpr;
    private List<WbStatementExpr> statementExprs;
    
    @JsonCreator
    public WbStatementGroupExpr(
            @JsonProperty("property") WbPropExpr propertyExpr,
            @JsonProperty("statements") List<WbStatementExpr> claimExprs) {
        this.propertyExpr = propertyExpr;
        this.statementExprs = claimExprs;
    }

    public StatementGroup evaluate(ExpressionContext ctxt, ItemIdValue subject) throws SkipStatementException {
        PropertyIdValue propertyId = propertyExpr.evaluate(ctxt);
        List<Statement> statements = new ArrayList<Statement>(statementExprs.size());
        for(WbStatementExpr expr : statementExprs) {
            statements.add(expr.evaluate(ctxt, subject, propertyId));
        }
        return Datamodel.makeStatementGroup(statements);
    }

    public WbPropExpr getProperty() {
        return propertyExpr;
    }

    public List<WbStatementExpr> getStatements() {
        return statementExprs;
    }
}
