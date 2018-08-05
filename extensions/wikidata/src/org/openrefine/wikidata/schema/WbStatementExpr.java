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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.helper.Validate;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementRank;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WbStatementExpr {

    private WbExpression<? extends Value> mainSnakValueExpr;
    private List<WbSnakExpr> qualifierExprs;
    private List<WbReferenceExpr> referenceExprs;

    @JsonCreator
    public WbStatementExpr(@JsonProperty("value") WbExpression<? extends Value> mainSnakValueExpr,
            @JsonProperty("qualifiers") List<WbSnakExpr> qualifierExprs,
            @JsonProperty("references") List<WbReferenceExpr> referenceExprs) {
        Validate.notNull(mainSnakValueExpr);
        this.mainSnakValueExpr = mainSnakValueExpr;
        if (qualifierExprs == null) {
            qualifierExprs = Collections.emptyList();
        }
        this.qualifierExprs = qualifierExprs;
        if (referenceExprs == null) {
            referenceExprs = Collections.emptyList();
        }
        this.referenceExprs = referenceExprs;
    }

    public static List<SnakGroup> groupSnaks(List<Snak> snaks) {
        Map<PropertyIdValue, List<Snak>> snakGroups = new HashMap<>();
        List<PropertyIdValue> propertyOrder = new ArrayList<PropertyIdValue>();
        for (Snak snak : snaks) {
            List<Snak> existingSnaks = snakGroups.get(snak.getPropertyId());
            if(existingSnaks == null) {
                existingSnaks = new ArrayList<Snak>();
                snakGroups.put(snak.getPropertyId(), existingSnaks);
                propertyOrder.add(snak.getPropertyId());
            }
            if (!existingSnaks.contains(snak)) {
                existingSnaks.add(snak);
            }
        }
        return propertyOrder.stream()
                .map(pid -> Datamodel.makeSnakGroup(snakGroups.get(pid)))
                .collect(Collectors.toList());
    }

    public Statement evaluate(ExpressionContext ctxt, ItemIdValue subject, PropertyIdValue propertyId)
            throws SkipSchemaExpressionException {
        Value mainSnakValue = getMainsnak().evaluate(ctxt);
        Snak mainSnak = Datamodel.makeValueSnak(propertyId, mainSnakValue);

        // evaluate qualifiers
        List<Snak> qualifiers = new ArrayList<Snak>(getQualifiers().size());
        for (WbSnakExpr qExpr : getQualifiers()) {
            try {
                qualifiers.add(qExpr.evaluate(ctxt));
            } catch (SkipSchemaExpressionException e) {
                QAWarning warning = new QAWarning("ignored-qualifiers", null, QAWarning.Severity.INFO, 1);
                warning.setProperty("example_entity", subject);
                warning.setProperty("example_property_entity", mainSnak.getPropertyId());
                ctxt.addWarning(warning);
            }
        }
        List<SnakGroup> groupedQualifiers = groupSnaks(qualifiers);
        Claim claim = Datamodel.makeClaim(subject, mainSnak, groupedQualifiers);

        // evaluate references
        List<Reference> references = new ArrayList<Reference>();
        for (WbReferenceExpr rExpr : getReferences()) {
            try {
                references.add(rExpr.evaluate(ctxt));
            } catch (SkipSchemaExpressionException e) {
                QAWarning warning = new QAWarning("ignored-references", null, QAWarning.Severity.INFO, 1);
                warning.setProperty("example_entity", subject);
                warning.setProperty("example_property_entity", mainSnak.getPropertyId());
                ctxt.addWarning(warning);
            }
        }

        StatementRank rank = StatementRank.NORMAL;
        return Datamodel.makeStatement(claim, references, rank, "");
    }

    @JsonProperty("value")
    public WbExpression<? extends Value> getMainsnak() {
        return mainSnakValueExpr;
    }

    @JsonProperty("qualifiers")
    public List<WbSnakExpr> getQualifiers() {
        return qualifierExprs;
    }

    @JsonProperty("references")
    public List<WbReferenceExpr> getReferences() {
        return referenceExprs;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbStatementExpr.class.isInstance(other)) {
            return false;
        }
        WbStatementExpr otherExpr = (WbStatementExpr) other;
        return mainSnakValueExpr.equals(otherExpr.getMainsnak()) && qualifierExprs.equals(otherExpr.getQualifiers())
                && referenceExprs.equals(otherExpr.getReferences());
    }

    @Override
    public int hashCode() {
        return mainSnakValueExpr.hashCode() + qualifierExprs.hashCode() + referenceExprs.hashCode();
    }
}
