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
package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A scrutinizer that checks for missing inverse statements in edit batches.
 * 
 * @author Antonin Delpeuch
 *
 */
public class InverseConstraintScrutinizer extends StatementScrutinizer {

    public static final String type = "missing-inverse-statements";

    private Map<PropertyIdValue, PropertyIdValue> _inverse;
    private Map<PropertyIdValue, Map<EntityIdValue, Set<EntityIdValue>>> _statements;

    public InverseConstraintScrutinizer() {
        _inverse = new HashMap<>();
        _statements = new HashMap<>();
    }

    protected PropertyIdValue getInverseConstraint(PropertyIdValue pid) {
        if (_inverse.containsKey(pid)) {
            return _inverse.get(pid);
        } else {
            PropertyIdValue inversePid = _fetcher.getInversePid(pid);
            _inverse.put(pid, inversePid);
            _statements.put(pid, new HashMap<EntityIdValue, Set<EntityIdValue>>());

            // We are doing this check because we do not have any guarantee that
            // the inverse constraints are consistent on Wikidata.
            if (inversePid != null && !_inverse.containsKey(inversePid)) {
                _inverse.put(inversePid, pid);
                _statements.put(inversePid, new HashMap<EntityIdValue, Set<EntityIdValue>>());
            }
            return inversePid;
        }
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        if (!added) {
            return; // TODO support for deleted statements
        }

        Value mainSnakValue = statement.getClaim().getMainSnak().getValue();
        if (ItemIdValue.class.isInstance(mainSnakValue)) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            PropertyIdValue inversePid = getInverseConstraint(pid);
            if (inversePid != null) {
                EntityIdValue targetEntityId = (EntityIdValue) mainSnakValue;
                Set<EntityIdValue> currentValues = _statements.get(pid).get(entityId);
                if (currentValues == null) {
                    currentValues = new HashSet<EntityIdValue>();
                    _statements.get(pid).put(entityId, currentValues);
                }
                currentValues.add(targetEntityId);
            }
        }
    }

    @Override
    public void batchIsFinished() {
        // For each pair of inverse properties (in each direction)
        for (Entry<PropertyIdValue, PropertyIdValue> propertyPair : _inverse.entrySet()) {
            // Get the statements made for the first
            PropertyIdValue ourProperty = propertyPair.getKey();
            for (Entry<EntityIdValue, Set<EntityIdValue>> itemLinks : _statements.get(ourProperty).entrySet()) {
                // For each outgoing link
                for (EntityIdValue idValue : itemLinks.getValue()) {
                    // Check that they are in the statements made for the second
                    PropertyIdValue missingProperty = propertyPair.getValue();
                    Set<EntityIdValue> reciprocalLinks = _statements.get(missingProperty).get(idValue);
                    if (reciprocalLinks == null || !reciprocalLinks.contains(itemLinks.getKey())) {
                        QAWarning issue = new QAWarning(type, ourProperty.getId(), QAWarning.Severity.IMPORTANT, 1);
                        issue.setProperty("added_property_entity", ourProperty);
                        issue.setProperty("inverse_property_entity", missingProperty);
                        issue.setProperty("source_entity", itemLinks.getKey());
                        issue.setProperty("target_entity", idValue);
                        addIssue(issue);
                    }
                }
            }
        }
    }

}
