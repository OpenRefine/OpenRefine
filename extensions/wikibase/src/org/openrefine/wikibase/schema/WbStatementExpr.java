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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.schema.entityvalues.FullyPropertySerializingNoValueSnak;
import org.openrefine.wikibase.schema.entityvalues.FullyPropertySerializingSomeValueSnak;
import org.openrefine.wikibase.schema.entityvalues.FullyPropertySerializingValueSnak;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueNoValueException;
import org.openrefine.wikibase.schema.exceptions.SpecialValueSomeValueException;
import org.openrefine.wikibase.schema.strategies.PropertyOnlyStatementMerger;
import org.openrefine.wikibase.schema.strategies.StatementEditingMode;
import org.openrefine.wikibase.schema.strategies.StatementMerger;
import org.openrefine.wikibase.schema.validation.PathElement;
import org.openrefine.wikibase.schema.validation.PathElement.Type;
import org.openrefine.wikibase.schema.validation.ValidationState;
import org.openrefine.wikibase.updates.StatementEdit;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
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
    private StatementMerger merger;
    private StatementEditingMode mode;

    @JsonCreator
    public WbStatementExpr(
            @JsonProperty("value") WbExpression<? extends Value> mainSnakValueExpr,
            @JsonProperty("qualifiers") List<WbSnakExpr> qualifierExprs,
            @JsonProperty("references") List<WbReferenceExpr> referenceExprs,
            @JsonProperty("mergingStrategy") StatementMerger merger,
            @JsonProperty("mode") StatementEditingMode mode) {
        // do not require a main value when deleting with a property only merger

        this.mainSnakValueExpr = mainSnakValueExpr;
        if (qualifierExprs == null) {
            qualifierExprs = Collections.emptyList();
        }
        this.qualifierExprs = qualifierExprs;
        if (referenceExprs == null) {
            referenceExprs = Collections.emptyList();
        }
        this.referenceExprs = referenceExprs;
        if (merger == null) {
            merger = StatementMerger.FORMER_DEFAULT_STRATEGY;
        }
        this.merger = merger;
        if (mode == null) {
            mode = StatementEditingMode.ADD_OR_MERGE;
        }
        this.mode = mode;
    }

    public void validate(ValidationState validation) {
        if (!(StatementEditingMode.DELETE.equals(mode)
                && (merger instanceof PropertyOnlyStatementMerger))
                && mainSnakValueExpr == null) {
            validation.addError("Missing main statement value");
        }
        if (mainSnakValueExpr != null) {
            validation.enter(new PathElement(Type.VALUE));
            mainSnakValueExpr.validate(validation);
            validation.leave();
        }
        int index = 0;
        for (WbSnakExpr qualifier : qualifierExprs) {
            if (qualifier == null) {
                validation.addError("Empty qualifier in statement");
            } else {
                validation.enter(new PathElement(Type.QUALIFIER, index));
                qualifier.validate(validation);
                validation.leave();
            }
            index++;
        }
        index = 0;
        for (WbReferenceExpr reference : referenceExprs) {
            if (reference == null) {
                validation.addError("Empty reference in statement");
            } else {
                validation.enter(new PathElement(Type.REFERENCE, index));
                reference.validate(validation);
                validation.leave();
            }
            index++;
        }
    }

    public static List<SnakGroup> groupSnaks(List<Snak> snaks) {
        Map<PropertyIdValue, List<Snak>> snakGroups = new HashMap<>();
        List<PropertyIdValue> propertyOrder = new ArrayList<PropertyIdValue>();
        for (Snak snak : snaks) {
            List<Snak> existingSnaks = snakGroups.get(snak.getPropertyId());
            if (existingSnaks == null) {
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

    public StatementEdit evaluate(ExpressionContext ctxt, EntityIdValue subject, PropertyIdValue propertyId)
            throws SkipSchemaExpressionException, QAWarningException {
        Snak mainSnak = null;
        if (mainSnakValueExpr != null) {
            try {
                Value mainSnakValue = mainSnakValueExpr.evaluate(ctxt);
                mainSnak = new FullyPropertySerializingValueSnak(propertyId, mainSnakValue);
            } catch (SpecialValueNoValueException e) {
                mainSnak = new FullyPropertySerializingNoValueSnak(propertyId);
            } catch (SpecialValueSomeValueException e) {
                mainSnak = new FullyPropertySerializingSomeValueSnak(propertyId);
            }
        } else {
            // hack to make sure we have a non-null snak
            mainSnak = Datamodel.makeNoValueSnak(propertyId);
        }

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
        return new StatementEdit(Datamodel.makeStatement(claim, references, rank, ""), merger, mode);
    }

    @JsonProperty("value")
    public WbExpression<? extends Value> getMainsnak() {
        return mainSnakValueExpr;
    }

    @JsonProperty("qualifiers")
    public List<WbSnakExpr> getQualifiers() {
        return Collections.unmodifiableList(qualifierExprs);
    }

    @JsonProperty("references")
    public List<WbReferenceExpr> getReferences() {
        return Collections.unmodifiableList(referenceExprs);
    }

    @JsonProperty("mergingStrategy")
    public StatementMerger getStatementMerger() {
        return merger;
    }

    @JsonProperty("mode")
    public StatementEditingMode getMode() {
        return mode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !WbStatementExpr.class.isInstance(other)) {
            return false;
        }
        WbStatementExpr otherExpr = (WbStatementExpr) other;
        return Objects.equals(mainSnakValueExpr, otherExpr.getMainsnak()) && qualifierExprs.equals(otherExpr.getQualifiers())
                && referenceExprs.equals(otherExpr.getReferences()) && merger.equals(otherExpr.getStatementMerger())
                && mode.equals(otherExpr.getMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainSnakValueExpr, qualifierExprs, referenceExprs);
    }

}
