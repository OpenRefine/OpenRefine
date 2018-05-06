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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class RestrictedPositionScrutinizer extends StatementScrutinizer {

    protected enum SnakPosition {
        MAINSNAK, QUALIFIER, REFERENCE
    }

    private Map<PropertyIdValue, SnakPosition> _restrictedPids;
    private Set<PropertyIdValue> _unrestrictedPids;

    public RestrictedPositionScrutinizer() {
        _restrictedPids = new HashMap<>();
        _unrestrictedPids = new HashSet<>();
    }

    SnakPosition positionRestriction(PropertyIdValue pid) {
        if (_unrestrictedPids.contains(pid)) {
            return null;
        }
        SnakPosition restriction = _restrictedPids.get(pid);
        if (restriction != null) {
            return restriction;
        } else {
            if (_fetcher.isForValuesOnly(pid)) {
                restriction = SnakPosition.MAINSNAK;
            } else if (_fetcher.isForQualifiersOnly(pid)) {
                restriction = SnakPosition.QUALIFIER;
            } else if (_fetcher.isForReferencesOnly(pid)) {
                restriction = SnakPosition.REFERENCE;
            }

            // Cache these results:
            if (restriction != null) {
                _restrictedPids.put(pid, restriction);
            } else {
                _unrestrictedPids.add(pid);
            }
            return restriction;
        }
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
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
        SnakPosition restriction = positionRestriction(snak.getPropertyId());
        if (restriction != null && position != restriction) {
            String positionStr = position.toString().toLowerCase();
            String restrictionStr = restriction.toString().toLowerCase();

            QAWarning issue = new QAWarning("property-restricted-to-" + restrictionStr + "-found-in-" + positionStr,
                    snak.getPropertyId().getId(), QAWarning.Severity.IMPORTANT, 1);
            issue.setProperty("property_entity", snak.getPropertyId());
            addIssue(issue);
        }
    }

}
