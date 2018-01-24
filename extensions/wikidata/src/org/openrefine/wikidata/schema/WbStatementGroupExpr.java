package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbStatementGroupExpr {
    
    private WbExpression<? extends PropertyIdValue> propertyExpr;
    private List<WbStatementExpr> statementExprs;
    
    @JsonCreator
    public WbStatementGroupExpr(
            @JsonProperty("property") WbExpression<? extends PropertyIdValue> propertyExpr,
            @JsonProperty("statements") List<WbStatementExpr> claimExprs) {
        this.propertyExpr = propertyExpr;
        this.statementExprs = claimExprs;
    }

    public StatementGroup evaluate(ExpressionContext ctxt, ItemIdValue subject) throws SkipSchemaExpressionException {
        PropertyIdValue propertyId = propertyExpr.evaluate(ctxt);
        List<Statement> statements = new ArrayList<Statement>(statementExprs.size());
        for(WbStatementExpr expr : statementExprs) {
            try {
                statements.add(expr.evaluate(ctxt, subject, propertyId));
            } catch (SkipSchemaExpressionException e) {
                continue;
            }
        }
        if (!statements.isEmpty()) {
            return Datamodel.makeStatementGroup(statements);
        } else {
            throw new SkipSchemaExpressionException();
        }
    }

    @JsonProperty("property")
    public WbExpression<? extends PropertyIdValue> getProperty() {
        return propertyExpr;
    }

    @JsonProperty("statements")
    public List<WbStatementExpr> getStatements() {
        return statementExprs;
    }
}
