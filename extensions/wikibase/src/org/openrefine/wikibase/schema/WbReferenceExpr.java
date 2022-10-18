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

import org.apache.commons.lang.Validate;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An expression for a reference (list of reference snaks).
 * 
 * @author Antonin Delpeuch
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class WbReferenceExpr implements WbExpression<Reference> {

    private List<WbSnakExpr> snakExprs;

    @JsonCreator
    public WbReferenceExpr(@JsonProperty("snaks") List<WbSnakExpr> snakExprs) {
        Validate.notNull(snakExprs);
        this.snakExprs = snakExprs;
    }

    @Override
    public void validate(ValidationState validation) {
        if (snakExprs == null) {
            validation.addError("No reference snaks provided");
        } else {
            // empty reference snaks are allowed
            snakExprs.forEach(snak -> {
                if (snak == null) {
                    validation.addError("Null snak in reference");
                } else {
                    snak.validate(validation);
                }
            });
        }
    }

    @Override
    public Reference evaluate(ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException {
        List<Snak> snakList = new ArrayList<Snak>();
        for (WbSnakExpr expr : getSnaks()) {
            try {
                snakList.add(expr.evaluate(ctxt));
            } catch (SkipSchemaExpressionException e) {
                continue;
            }
        }
        List<SnakGroup> snakGroups = WbStatementExpr.groupSnaks(snakList);
        if (!snakGroups.isEmpty()) {
            return Datamodel.makeReference(snakGroups);
        } else {
            throw new SkipSchemaExpressionException();
        }
    }

    @JsonProperty("snaks")
    public List<WbSnakExpr> getSnaks() {
        return Collections.unmodifiableList(snakExprs);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbReferenceExpr.class.isInstance(other)) {
            return false;
        }
        WbReferenceExpr otherExpr = (WbReferenceExpr) other;
        return snakExprs.equals(otherExpr.getSnaks());
    }

    @Override
    public int hashCode() {
        return snakExprs.hashCode();
    }

}
