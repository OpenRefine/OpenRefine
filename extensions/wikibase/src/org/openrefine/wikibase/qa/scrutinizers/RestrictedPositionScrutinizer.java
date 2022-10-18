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

package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.Iterator;
import java.util.List;

public class RestrictedPositionScrutinizer extends StatementScrutinizer {

    public String scopeConstraintQid;
    public String scopeConstraintPid;
    public String scopeConstraintValueQid;
    public String scopeConstraintQualifierQid;
    public String scopeConstraintReferenceQid;

    @Override
    public boolean prepareDependencies() {
        scopeConstraintQid = getConstraintsRelatedId("property_scope_constraint_qid");
        scopeConstraintPid = getConstraintsRelatedId("property_scope_pid");
        scopeConstraintValueQid = getConstraintsRelatedId("as_main_value_qid");
        scopeConstraintQualifierQid = getConstraintsRelatedId("as_qualifiers_qid");
        scopeConstraintReferenceQid = getConstraintsRelatedId("as_references_qid");
        return _fetcher != null && scopeConstraintQid != null && scopeConstraintPid != null && scopeConstraintValueQid != null
                && scopeConstraintQualifierQid != null && scopeConstraintReferenceQid != null;
    }

    protected enum SnakPosition {
        MAINSNAK, QUALIFIER, REFERENCE
    }

    class RestrictedPositionConstraint {

        boolean isAllowedAsValue, isAllowedAsQualifier, isAllowedAsReference;

        RestrictedPositionConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                ItemIdValue targetValue = Datamodel.makeWikidataItemIdValue(scopeConstraintValueQid);
                ItemIdValue targetQualifier = Datamodel.makeWikidataItemIdValue(scopeConstraintQualifierQid);
                ItemIdValue targetReference = Datamodel.makeWikidataItemIdValue(scopeConstraintReferenceQid);
                List<Value> snakValues = findValues(specs, scopeConstraintPid);
                isAllowedAsValue = snakValues.contains(targetValue);
                isAllowedAsQualifier = snakValues.contains(targetQualifier);
                isAllowedAsReference = snakValues.contains(targetReference);
            }
        }
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        if (!added) {
            // not scrutinizing deleted statements
            return;
        }
        // Skip the main snak
        scrutinize(statement.getClaim().getMainSnak(), entityId, SnakPosition.MAINSNAK, added);

        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), entityId, SnakPosition.QUALIFIER, added);

        // References
        for (Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), entityId, SnakPosition.REFERENCE, added);
        }
    }

    protected void scrutinizeSnakSet(Iterator<Snak> snaks, EntityIdValue entityId, SnakPosition position,
            boolean added) {
        while (snaks.hasNext()) {
            Snak snak = snaks.next();
            scrutinize(snak, entityId, position, added);
        }
    }

    public void scrutinize(Snak snak, EntityIdValue entityId, SnakPosition position, boolean added) {
        if (!positionAllowed(snak.getPropertyId(), position)) {
            String positionStr = position.toString().toLowerCase();

            QAWarning issue = new QAWarning("property-found-in-" + positionStr,
                    snak.getPropertyId().getId(), QAWarning.Severity.IMPORTANT, 1);
            issue.setProperty("property_entity", snak.getPropertyId());
            addIssue(issue);
        }
    }

    public boolean positionAllowed(PropertyIdValue pid, SnakPosition position) {
        List<Statement> constraintDefinitions = _fetcher.getConstraintsByType(pid, scopeConstraintQid);
        if (!constraintDefinitions.isEmpty()) {
            RestrictedPositionConstraint constraint = new RestrictedPositionConstraint(constraintDefinitions.get(0));
            if (position.equals(SnakPosition.MAINSNAK)) {
                return constraint.isAllowedAsValue;
            } else if (position.equals(SnakPosition.QUALIFIER)) {
                return constraint.isAllowedAsQualifier;
            } else if (position.equals(SnakPosition.REFERENCE)) {
                return constraint.isAllowedAsReference;
            }
        }
        return true;
    }

}
