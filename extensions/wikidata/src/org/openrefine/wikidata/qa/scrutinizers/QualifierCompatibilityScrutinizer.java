package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer that checks the compatibility of the qualifiers
 * and the property of a statement, and looks for mandatory qualifiers.
 * @author antonin
 *
 */
public class QualifierCompatibilityScrutinizer extends StatementScrutinizer {
    
    private Map<PropertyIdValue, Set<PropertyIdValue>> _allowedQualifiers;
    private Map<PropertyIdValue, Set<PropertyIdValue>> _mandatoryQualifiers;
    
    public QualifierCompatibilityScrutinizer() {
        _allowedQualifiers = new HashMap<>();
        _mandatoryQualifiers = new HashMap<>();
    }
    
    protected boolean qualifierIsAllowed(PropertyIdValue statementProperty, PropertyIdValue qualifierProperty) {
        Set<PropertyIdValue> allowed = null;
        if (_allowedQualifiers.containsKey(statementProperty)) {
            allowed = _allowedQualifiers.get(statementProperty);
        } else {
            allowed = _fetcher.allowedQualifiers(statementProperty);
            _allowedQualifiers.put(statementProperty, allowed);
        }
        return allowed == null || allowed.contains(qualifierProperty);
    }
    
    protected Set<PropertyIdValue> mandatoryQualifiers(PropertyIdValue statementProperty) {
        Set<PropertyIdValue> mandatory = null;
        if (_mandatoryQualifiers.containsKey(statementProperty)) {
            mandatory = _mandatoryQualifiers.get(statementProperty);
        } else {
            mandatory = _fetcher.mandatoryQualifiers(statementProperty);
            if (mandatory == null) {
                mandatory = new HashSet<>();
            }
            _mandatoryQualifiers.put(statementProperty, mandatory);
        }
        return mandatory;
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        PropertyIdValue statementProperty = statement.getClaim().getMainSnak().getPropertyId();
        Set<PropertyIdValue> qualifiers = statement.getClaim().getQualifiers().
            stream().map(e -> e.getProperty()).collect(Collectors.toSet());
        
        Set<PropertyIdValue> missingQualifiers = mandatoryQualifiers(statementProperty)
                .stream().filter(p -> !qualifiers.contains(p)).collect(Collectors.toSet());
        Set<PropertyIdValue> disallowedQualifiers = qualifiers
                .stream().filter(p -> !qualifierIsAllowed(statementProperty, p)).collect(Collectors.toSet());
        
        for (PropertyIdValue missing : missingQualifiers) {
            QAWarning issue = new QAWarning(
                    "missing-mandatory-qualifiers",
                    statementProperty.getId()+"-"+missing.getId(),
                    QAWarning.Severity.WARNING,
                    1);
            issue.setProperty("statement_property_entity", statementProperty);
            issue.setProperty("missing_property_entity", missing);
            issue.setProperty("example_item_entity", entityId);
            addIssue(issue);
        }
        for (PropertyIdValue disallowed : disallowedQualifiers) {
            QAWarning issue = new QAWarning(
                    "disallowed-qualifiers",
                    statementProperty.getId()+"-"+disallowed.getId(),
                    QAWarning.Severity.WARNING,
                    1);
            issue.setProperty("statement_property_entity", statementProperty);
            issue.setProperty("disallowed_property_entity", disallowed);
            issue.setProperty("example_item_entity", entityId);
            addIssue(issue);
        }
    }

}
