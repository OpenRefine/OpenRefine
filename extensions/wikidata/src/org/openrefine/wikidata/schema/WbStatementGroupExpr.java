/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WbStatementGroupExpr {

    private WbExpression<? extends PropertyIdValue> propertyExpr;
    private List<WbStatementExpr> statementExprs;

    @JsonCreator
    public WbStatementGroupExpr(@JsonProperty("property") WbExpression<? extends PropertyIdValue> propertyExpr,
            @JsonProperty("statements") List<WbStatementExpr> claimExprs) {
        Validate.notNull(propertyExpr);
        this.propertyExpr = propertyExpr;
        Validate.notNull(claimExprs);
        Validate.isTrue(!claimExprs.isEmpty());
        this.statementExprs = claimExprs;
    }

    public StatementGroup evaluate(ExpressionContext ctxt, ItemIdValue subject)
            throws SkipSchemaExpressionException {
        PropertyIdValue propertyId = propertyExpr.evaluate(ctxt);
        List<Statement> statements = new ArrayList<Statement>(statementExprs.size());
        for (WbStatementExpr expr : statementExprs) {
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

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbStatementGroupExpr.class.isInstance(other)) {
            return false;
        }
        WbStatementGroupExpr otherExpr = (WbStatementGroupExpr) other;
        return propertyExpr.equals(otherExpr.getProperty()) && statementExprs.equals(otherExpr.getStatements());
    }

    @Override
    public int hashCode() {
        return propertyExpr.hashCode() + statementExprs.hashCode();
    }
}
