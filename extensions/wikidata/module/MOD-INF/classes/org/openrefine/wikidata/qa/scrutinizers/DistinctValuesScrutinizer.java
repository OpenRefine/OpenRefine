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
import java.util.Map;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A scrutinizer that checks for properties using the same value on different
 * items.
 * 
 * @author Antonin Delpeuch
 *
 */
public class DistinctValuesScrutinizer extends StatementScrutinizer {

    public final static String type = "identical-values-for-distinct-valued-property";

    private Map<PropertyIdValue, Map<Value, EntityIdValue>> _seenValues;

    public DistinctValuesScrutinizer() {
        _seenValues = new HashMap<>();
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
        if (_fetcher.hasDistinctValues(pid)) {
            Value mainSnakValue = statement.getClaim().getMainSnak().getValue();
            Map<Value, EntityIdValue> seen = _seenValues.get(pid);
            if (seen == null) {
                seen = new HashMap<Value, EntityIdValue>();
                _seenValues.put(pid, seen);
            }
            if (seen.containsKey(mainSnakValue)) {
                EntityIdValue otherId = seen.get(mainSnakValue);
                QAWarning issue = new QAWarning(type, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("item1_entity", entityId);
                issue.setProperty("item2_entity", otherId);
                addIssue(issue);
            } else {
                seen.put(mainSnakValue, entityId);
            }
        }
    }

}
