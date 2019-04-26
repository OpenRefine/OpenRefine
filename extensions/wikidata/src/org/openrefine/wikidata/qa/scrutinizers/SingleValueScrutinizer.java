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

import java.util.HashSet;
import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * For now this scrutinizer only checks for uniqueness at the item level (it
 * ignores qualifiers and references).
 * 
 * Given that all ranks are currently set to Normal, this also checks for
 * single best values.
 * 
 * @author Antonin Delpeuch
 *
 */
public class SingleValueScrutinizer extends EditScrutinizer {

    public static final String type = "single-valued-property-added-more-than-once";

    @Override
    public void scrutinize(ItemUpdate update) {
        Set<PropertyIdValue> seenSingleProperties = new HashSet<>();

        for (Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            if (seenSingleProperties.contains(pid)) {

                QAWarning issue = new QAWarning(type, pid.getId(), QAWarning.Severity.WARNING, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
            } else if (_fetcher.hasSingleValue(pid) || _fetcher.hasSingleBestValue(pid)) {
                seenSingleProperties.add(pid);
            }
        }
    }

}
