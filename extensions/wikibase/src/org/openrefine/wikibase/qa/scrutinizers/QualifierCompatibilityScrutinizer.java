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
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A scrutinizer that checks the compatibility of the qualifiers and the property of a statement, and looks for
 * mandatory qualifiers.
 * 
 * @author Antonin Delpeuch
 */
public class QualifierCompatibilityScrutinizer extends StatementScrutinizer {

    public static final String missingMandatoryQualifiersType = "missing-mandatory-qualifiers";
    public static final String disallowedQualifiersType = "disallowed-qualifiers";

    public String allowedQualifiersConstraintQid;
    public String allowedQualifiersConstraintPid;

    public String mandatoryQualifiersConstraintQid;
    public String mandatoryQualifiersConstraintPid;

    class AllowedQualifierConstraint {

        Set<PropertyIdValue> allowedProperties;

        AllowedQualifierConstraint(Statement statement) {
            allowedProperties = new HashSet<>();
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = findValues(specs, allowedQualifiersConstraintPid);
                allowedProperties = properties.stream()
                        .filter(e -> e != null)
                        .map(e -> (PropertyIdValue) e)
                        .collect(Collectors.toSet());
            }

        }
    }

    class MandatoryQualifierConstraint {

        Set<PropertyIdValue> mandatoryProperties;

        MandatoryQualifierConstraint(Statement statement) {
            mandatoryProperties = new HashSet<>();
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = findValues(specs, mandatoryQualifiersConstraintPid);
                mandatoryProperties = properties.stream()
                        .filter(e -> e != null)
                        .map(e -> (PropertyIdValue) e)
                        .collect(Collectors.toSet());
            }
        }
    }

    private Map<PropertyIdValue, Set<PropertyIdValue>> _allowedQualifiers;
    private Map<PropertyIdValue, Set<PropertyIdValue>> _mandatoryQualifiers;

    public QualifierCompatibilityScrutinizer() {
        _allowedQualifiers = new HashMap<>();
        _mandatoryQualifiers = new HashMap<>();
    }

    @Override
    public boolean prepareDependencies() {
        allowedQualifiersConstraintQid = getConstraintsRelatedId("allowed_qualifiers_constraint_qid");
        allowedQualifiersConstraintPid = getConstraintsRelatedId("property_pid");
        mandatoryQualifiersConstraintQid = getConstraintsRelatedId("mandatory_qualifier_constraint_qid");
        mandatoryQualifiersConstraintPid = getConstraintsRelatedId("property_pid");
        return _fetcher != null && allowedQualifiersConstraintQid != null && allowedQualifiersConstraintPid != null &&
                mandatoryQualifiersConstraintQid != null && mandatoryQualifiersConstraintPid != null;
    }

    protected boolean qualifierIsAllowed(PropertyIdValue statementProperty, PropertyIdValue qualifierProperty) {
        Set<PropertyIdValue> allowed = null;
        if (_allowedQualifiers.containsKey(statementProperty)) {
            allowed = _allowedQualifiers.get(statementProperty);
        } else {
            List<Statement> statementList = _fetcher.getConstraintsByType(statementProperty, allowedQualifiersConstraintQid);
            if (!statementList.isEmpty()) {
                AllowedQualifierConstraint allowedQualifierConstraint = new AllowedQualifierConstraint(statementList.get(0));
                allowed = allowedQualifierConstraint.allowedProperties;
            }
            _allowedQualifiers.put(statementProperty, allowed);
        }
        return allowed == null || allowed.contains(qualifierProperty);
    }

    protected Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue statementProperty) {
        Set<PropertyIdValue> mandatory = null;
        if (_mandatoryQualifiers.containsKey(statementProperty)) {
            mandatory = _mandatoryQualifiers.get(statementProperty);
        } else {
            List<Statement> statementList = _fetcher.getConstraintsByType(statementProperty, mandatoryQualifiersConstraintQid);
            if (!statementList.isEmpty()) {
                MandatoryQualifierConstraint mandatoryQualifierConstraint = new MandatoryQualifierConstraint(statementList.get(0));
                mandatory = mandatoryQualifierConstraint.mandatoryProperties;
            }
            if (mandatory == null) {
                mandatory = new HashSet<>();
            }
            _mandatoryQualifiers.put(statementProperty, mandatory);
        }
        return mandatory;
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        if (!added) {
            // not scrutinizing deleted statements
            return;
        }
        PropertyIdValue statementProperty = statement.getClaim().getMainSnak().getPropertyId();
        Set<PropertyIdValue> qualifiers = statement.getClaim().getQualifiers().stream().map(e -> e.getProperty())
                .collect(Collectors.toSet());

        Set<PropertyIdValue> missingQualifiers = mandatoryQualifiers(statementProperty).stream()
                .filter(p -> !qualifiers.contains(p)).collect(Collectors.toSet());
        Set<PropertyIdValue> disallowedQualifiers = qualifiers.stream()
                .filter(p -> !qualifierIsAllowed(statementProperty, p)).collect(Collectors.toSet());

        for (PropertyIdValue missing : missingQualifiers) {
            QAWarning issue = new QAWarning(missingMandatoryQualifiersType,
                    statementProperty.getId() + "-" + missing.getId(), QAWarning.Severity.WARNING, 1);
            issue.setProperty("statement_property_entity", statementProperty);
            issue.setProperty("missing_property_entity", missing);
            issue.setProperty("example_item_entity", entityId);
            addIssue(issue);
        }
        for (PropertyIdValue disallowed : disallowedQualifiers) {
            QAWarning issue = new QAWarning(disallowedQualifiersType,
                    statementProperty.getId() + "-" + disallowed.getId(), QAWarning.Severity.WARNING, 1);
            issue.setProperty("statement_property_entity", statementProperty);
            issue.setProperty("disallowed_property_entity", disallowed);
            issue.setProperty("example_item_entity", entityId);
            addIssue(issue);
        }
    }

}
