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

package org.openrefine.wikibase.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueNoValueException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueSomeValueException;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.StatementEdit;
import org.openrefine.wikibase.updates.StatementGroupEdit;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

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
        this.propertyExpr = propertyExpr;
        this.statementExprs = claimExprs != null ? claimExprs : Collections.emptyList();
    }

    /**
     * Checks that the expression has all its required components and is ready to be evaluated.
     * 
     * @param validation
     */
    public void validate(ValidationState validation) {
        validation.enter(new PathElement(Type.STATEMENT));
        if (propertyExpr == null) {
            validation.addError("No property");
        } else {
            propertyExpr.validate(validation);
        }
        validation.leave();

        // Extract property name to contribute to further validation paths
        String propertyName = null;
        if (propertyExpr instanceof WbPropConstant) {
            WbPropConstant propConstant = (WbPropConstant) propertyExpr;
            if (propConstant.getLabel() != null && propConstant.getPid() != null) {
                propertyName = String.format("%s (%s)", propConstant.getLabel(), propConstant.getPid());
            }
        }
        if (statementExprs == null || statementExprs.isEmpty()) {
            validation.addError("No statements");
        }
        for (WbStatementExpr statement : statementExprs) {
            validation.enter(new PathElement(Type.STATEMENT, propertyName));
            if (statement != null) {
                statement.validate(validation);
            } else {
                validation.addError("Empty statement");
            }
            validation.leave();
        }
    }

    public StatementGroupEdit evaluate(ExpressionContext ctxt, EntityIdValue subject)
            throws SkipSchemaExpressionException, QAWarningException {
        try {
            PropertyIdValue propertyId = propertyExpr.evaluate(ctxt);
            List<StatementEdit> statements = new ArrayList<>(statementExprs.size());
            for (WbStatementExpr expr : statementExprs) {
                try {
                    statements.add(expr.evaluate(ctxt, subject, propertyId));
                } catch (SkipSchemaExpressionException e) {
                    continue;
                }
            }
            if (!statements.isEmpty()) {
                return new StatementGroupEdit(statements);
            } else {
                throw new SkipSchemaExpressionException();
            }
        } catch (SpecialValueNoValueException | SpecialValueSomeValueException e) {
            throw new SkipSchemaExpressionException(); // this should never happen
        }
    }

    @JsonProperty("property")
    public WbExpression<? extends PropertyIdValue> getProperty() {
        return propertyExpr;
    }

    @JsonProperty("statements")
    public List<WbStatementExpr> getStatements() {
        return Collections.unmodifiableList(statementExprs);
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
